package esw.segment.shared

import esw.segment.shared.EswSegmentData.{DateRange, SegmentToM1Pos}
import EswSegmentData._
import org.scalatest.funsuite.AsyncFunSuite
import spray.json._

import java.nio.charset.StandardCharsets
import java.time.LocalDate
import scala.async.Async.{async, await}
import scala.concurrent.Future

//noinspection SameParameterValue
class SegmentToM1ApiTestBase(posTable: SegmentToM1Api) extends AsyncFunSuite with JsonSupport {
  private val dateRange1 = DateRange(LocalDate.parse("2020-10-21"), LocalDate.parse("2020-10-21"))
  private val dateRange2 = DateRange(LocalDate.parse("2020-10-21"), LocalDate.parse("2020-10-22"))
  private val dateRange3 = DateRange(LocalDate.parse("2020-10-05"), LocalDate.parse("2020-10-06"))
  private val dateRange4 = DateRange(LocalDate.parse("2020-10-01"), LocalDate.parse("2020-10-06"))
  private val dateRange5 = DateRange(LocalDate.parse("2020-10-07"), LocalDate.parse("2020-10-07"))

  private def currentDate(): LocalDate = LocalDate.now()

  private def populateSomeSegments(): Future[Unit] =
    async {
      assert(await(posTable.setPosition(new SegmentToM1Pos(LocalDate.parse("2020-10-01"), "SN-001", "A4"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(LocalDate.parse("2020-10-02"), "SN-002", "A4"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(LocalDate.parse("2020-10-03"), "SN-002", "A4"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(LocalDate.parse("2020-10-04"), "SN-002", "A4"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(LocalDate.parse("2020-10-05"), "SN-003", "A4"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(LocalDate.parse("2020-10-06"), "SN-003", "A4"))))
      assert(await(posTable.setPosition(SegmentToM1Pos(LocalDate.parse("2020-10-07"), None, "A4"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(LocalDate.parse("2020-10-08"), "SN-003", "A4"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(LocalDate.parse("2020-10-09"), "SN-003", "A4"))))

      assert(await(posTable.setPosition(new SegmentToM1Pos(LocalDate.parse("2020-10-10"), "SN-032", "A32"))))
      assert(await(posTable.setPosition(SegmentToM1Pos(LocalDate.parse("2020-10-10"), None, "A33"))))

      assert(await(posTable.setPosition(new SegmentToM1Pos(LocalDate.parse("2020-10-21"), "SN-003", "A4"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(LocalDate.parse("2020-10-21"), "SN-513", "A2"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(LocalDate.parse("2020-10-21"), "SN-390", "F5"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(LocalDate.parse("2020-10-22"), "SN-003", "A4"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(LocalDate.parse("2020-10-22"), "SN-193", "B23"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(LocalDate.parse("2020-10-22"), "SN-513", "F82"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(LocalDate.parse("2020-10-23"), "SN-081", "A12"))))

      // XXX TODO: Allow inserts?
      // Insert row before the current ones to test update of following undefined positions
//      assert(await(posTable.setPosition(new SegmentToM1Pos(LocalDate.parse("2020-10-10"), "SN-032", "A32"))))
//      assert(await(posTable.setPosition(SegmentToM1Pos(LocalDate.parse("2020-10-10"), None, "A33"))))

      assert(
        await(
          posTable.setPositions(
            MirrorConfig(
              LocalDate.parse("2020-10-23"),
              List(
                SegmentConfig("A78", Some("SN-483")),
                SegmentConfig("B78", Some("SN-484")),
                SegmentConfig("D78", Some("SN-486"))
              )
            )
          )
        )
      )
    }

  // Populate all segments using the given date and resource file
  private def populateAllSegments(resource: String): Future[Unit] =
    async {
      val bytes        = getClass.getResourceAsStream(s"/$resource").readAllBytes()
      val json         = new String(bytes, StandardCharsets.UTF_8)
      val mirrorConfig = json.parseJson.convertTo[MirrorConfig]
      val result = await(posTable.setPositions(mirrorConfig))
      if (!result) fail("Call to setPositions() failed.")
    }

  test("Test with single segment positions") {
    async {
      assert(await(posTable.resetSegmentToM1PosTable()))
      await(populateSomeSegments())

      assert(await(posTable.mostRecentChange(currentDate())) == LocalDate.parse("2020-10-23"))

      assert(
        await(posTable.segmentPositions(dateRange1, "SN-513")) == List(
          new SegmentToM1Pos(LocalDate.parse("2020-10-21"), "SN-513", "A2")
        )
      )
      assert(await(posTable.segmentIds(dateRange1, "A2")) == List(new SegmentToM1Pos(LocalDate.parse("2020-10-21"), "SN-513", "A2")))
      assert(
        await(posTable.segmentPositions(dateRange1, "SN-390")) == List(
          new SegmentToM1Pos(LocalDate.parse("2020-10-21"), "SN-390", "F5")
        )
      )
      assert(
        await(posTable.segmentPositions(dateRange1, "SN-032")) == List(
          new SegmentToM1Pos(LocalDate.parse("2020-10-10"), "SN-032", "A32")
        )
      )

      assert(await(posTable.segmentIds(dateRange1, "F5")) == List(new SegmentToM1Pos(LocalDate.parse("2020-10-21"), "SN-390", "F5")))
      assert(await(posTable.segmentIds(dateRange1, "A8")).isEmpty)
      assert(await(posTable.segmentIds(dateRange1, "A4")) == List(new SegmentToM1Pos(LocalDate.parse("2020-10-08"), "SN-003", "A4")))
      //
      assert(
        await(posTable.segmentIds(dateRange1, "A32")) == List(new SegmentToM1Pos(LocalDate.parse("2020-10-10"), "SN-032", "A32"))
      )
      assert(await(posTable.segmentIds(dateRange1, "A33")).isEmpty)
      assert(await(posTable.segmentIds(dateRange3, "A4")) == List(new SegmentToM1Pos(LocalDate.parse("2020-10-05"), "SN-003", "A4")))
      assert(
        await(posTable.segmentIds(dateRange4, "A4")).toSet == Set(
          new SegmentToM1Pos(LocalDate.parse("2020-10-01"), "SN-001", "A4"),
          new SegmentToM1Pos(LocalDate.parse("2020-10-02"), "SN-002", "A4"),
          new SegmentToM1Pos(LocalDate.parse("2020-10-05"), "SN-003", "A4")
        )
      )
      assert(await(posTable.segmentIds(dateRange4, "A13")).isEmpty)
      assert(await(posTable.segmentIds(dateRange5, "A4")).isEmpty)

      assert(await(posTable.segmentPositions(dateRange1, "SN-081")).isEmpty)
      assert(await(posTable.segmentPositions(dateRange5, "SN-003")).isEmpty)
      assert(
        await(posTable.segmentPositions(dateRange2, "SN-390")).toSet == Set(
          new SegmentToM1Pos(LocalDate.parse("2020-10-21"), "SN-390", "F5")
        )
      )
      assert(
        await(posTable.segmentPositions(dateRange2, "SN-513")).toSet == Set(
          new SegmentToM1Pos(LocalDate.parse("2020-10-21"), "SN-513", "A2"),
          new SegmentToM1Pos(LocalDate.parse("2020-10-22"), "SN-513", "F82")
        )
      )
      assert(
        await(posTable.segmentPositions(dateRange2, "SN-003")) == List(
          new SegmentToM1Pos(LocalDate.parse("2020-10-08"), "SN-003", "A4")
        )
      )

      //      assert(await(posTable.setPosition(new SegmentToM1Pos(LocalDate.parse("2020-10-21"), "SN-513", "A2"))))
      //      assert(await(posTable.setPosition(new SegmentToM1Pos(LocalDate.parse("2020-10-21"), "SN-390", "F5"))))
      assert(
        await(posTable.newlyInstalledSegments(LocalDate.parse("2020-10-21"))).toSet == Set(
          new SegmentToM1Pos(LocalDate.parse("2020-10-21"), "SN-513", "A2"),
          new SegmentToM1Pos(LocalDate.parse("2020-10-21"), "SN-390", "F5"),
          new SegmentToM1Pos(LocalDate.parse("2020-10-23"), "SN-081", "A12"),
          new SegmentToM1Pos(LocalDate.parse("2020-10-22"), "SN-193", "B23"),
          new SegmentToM1Pos(LocalDate.parse("2020-10-22"), "SN-513", "F82"),
          new SegmentToM1Pos(LocalDate.parse("2020-10-23"), "SN-483", "A78"),
          new SegmentToM1Pos(LocalDate.parse("2020-10-23"), "SN-484", "B78"),
          new SegmentToM1Pos(LocalDate.parse("2020-10-23"), "SN-486", "D78")
        )
      )

      val currentPositions = await(posTable.currentPositions()).toSet
      assert(currentPositions.size == EswSegmentData.totalSegments)
      assert(
        currentPositions.filter(_.maybeId.isDefined) == Set(
          new SegmentToM1Pos(LocalDate.parse("2020-10-08"), "SN-003", "A4"),
          new SegmentToM1Pos(LocalDate.parse("2020-10-23"), "SN-081", "A12"),
          new SegmentToM1Pos(LocalDate.parse("2020-10-10"), "SN-032", "A32"),
          new SegmentToM1Pos(LocalDate.parse("2020-10-22"), "SN-193", "B23"),
          new SegmentToM1Pos(LocalDate.parse("2020-10-21"), "SN-390", "F5"),
          new SegmentToM1Pos(LocalDate.parse("2020-10-22"), "SN-513", "F82"),
          new SegmentToM1Pos(LocalDate.parse("2020-10-23"), "SN-483", "A78"),
          new SegmentToM1Pos(LocalDate.parse("2020-10-23"), "SN-484", "B78"),
          new SegmentToM1Pos(LocalDate.parse("2020-10-23"), "SN-486", "D78")
        )
      )

      assert(
        await(posTable.currentSegmentPosition("SN-513"))
          .contains(new SegmentToM1Pos(LocalDate.parse("2020-10-22"), "SN-513", "F82"))
      )

      assert(
        await(posTable.currentSegmentPosition("SN-032"))
          .contains(new SegmentToM1Pos(LocalDate.parse("2020-10-10"), "SN-032", "A32"))
      )

      assert(
        await(posTable.currentSegmentPosition("SN-003"))
          .contains(new SegmentToM1Pos(LocalDate.parse("2020-10-08"), "SN-003", "A4"))
      )

      assert(
        await(posTable.currentSegmentPosition("SN-193"))
          .contains(new SegmentToM1Pos(LocalDate.parse("2020-10-22"), "SN-193", "B23"))
      )

      assert(
        await(posTable.positionsOnDate(LocalDate.parse("2020-10-04"))).filter(_.maybeId.isDefined) == List(
          new SegmentToM1Pos(LocalDate.parse("2020-10-02"), "SN-002", "A4")
        )
      )
      assert(
        await(posTable.positionsOnDate(LocalDate.parse("2020-10-21"))).toSet.filter(_.maybeId.isDefined) == Set(
          new SegmentToM1Pos(LocalDate.parse("2020-10-21"), "SN-513", "A2"),
          new SegmentToM1Pos(LocalDate.parse("2020-10-08"), "SN-003", "A4"),
          new SegmentToM1Pos(LocalDate.parse("2020-10-10"), "SN-032", "A32"),
          new SegmentToM1Pos(LocalDate.parse("2020-10-21"), "SN-390", "F5")
        )
      )

      assert(
        await(posTable.positionsOnDate(LocalDate.parse("2020-10-23"))).toSet.filter(_.maybeId.isDefined) == Set(
          new SegmentToM1Pos(LocalDate.parse("2020-10-08"), "SN-003", "A4"),
          new SegmentToM1Pos(LocalDate.parse("2020-10-10"), "SN-032", "A32"),
          new SegmentToM1Pos(LocalDate.parse("2020-10-23"), "SN-081", "A12"),
          new SegmentToM1Pos(LocalDate.parse("2020-10-22"), "SN-193", "B23"),
          new SegmentToM1Pos(LocalDate.parse("2020-10-22"), "SN-513", "F82"),
          new SegmentToM1Pos(LocalDate.parse("2020-10-23"), "SN-483", "A78"),
          new SegmentToM1Pos(LocalDate.parse("2020-10-23"), "SN-484", "B78"),
          new SegmentToM1Pos(LocalDate.parse("2020-10-23"), "SN-486", "D78"),
          new SegmentToM1Pos(LocalDate.parse("2020-10-21"), "SN-390", "F5")
        )
      )

      assert(
        await(posTable.segmentPositionOnDate(LocalDate.parse("2020-10-04"), "SN-002"))
          .contains(new SegmentToM1Pos(LocalDate.parse("2020-10-02"), "SN-002", "A4"))
      )
      assert(
        await(posTable.segmentPositionOnDate(LocalDate.parse("2020-10-21"), "SN-513"))
          .contains(new SegmentToM1Pos(LocalDate.parse("2020-10-21"), "SN-513", "A2"))
      )

      assert(
        await(posTable.segmentAtPositionOnDate(LocalDate.parse("2020-10-04"), "A4"))
          .contains(new SegmentToM1Pos(LocalDate.parse("2020-10-02"), "SN-002", "A4"))
      )
      assert(
        await(posTable.segmentAtPositionOnDate(LocalDate.parse("2020-10-21"), "A2"))
          .contains(new SegmentToM1Pos(LocalDate.parse("2020-10-21"), "SN-513", "A2"))
      )
    }

  }

//  test("Test with all segment positions") {
//    async {
//      assert(await(posTable.resetSegmentToM1PosTable()))
//      await(populateAllSegments(dateRange1.from, "mirror-2021-01-01.json"))
//
//      assert(await(posTable.mostRecentChange(currentDate())) == dateRange1.from)
//
//      assert(await(posTable.segmentPositions(dateRange1, "SN-026")) == List(new SegmentToM1Pos(dateRange1.from, "SN-026", "A7")))
//      assert(await(posTable.segmentPositions(dateRange1, "SN-511")) == List(new SegmentToM1Pos(dateRange1.from, "SN-511", "A82")))
//
//      assert(await(posTable.segmentIds(dateRange1, "A6")) == List(new SegmentToM1Pos(dateRange1.from, "SN-073", "A6")))
//
//      // Set some more segment positions (the previous positions are automatically inherited/removed as needed)
//      await(populateSomeSegments())
//      assert(
//        await(posTable.segmentPositions(dateRange1, "SN-513")) == List(
//          new SegmentToM1Pos(LocalDate.parse("2020-10-21"), "SN-513", "A2")
//        )
//      )
//      // "SN-450" was replaced with "SN-513 (A2)"
//      assert(await(posTable.segmentPositions(dateRange1, "SN-450")).isEmpty)
//      // Previous "SN-513" position now empty
//      assert(await(posTable.segmentIds(dateRange1, "A7")).isEmpty)
//    }
//  }

  test("Test getting list of segment exchanges") {
    async {
      assert(await(posTable.resetSegmentToM1PosTable()))
      println(s"Start import")
      await(populateAllSegments("mirror-2021-01-01.json"))
      await(populateAllSegments("mirror-2021-01-02.json"))
      await(populateAllSegments("mirror-2021-01-03.json"))
//      await(populateAllSegments("mirror-2021-01-04.json"))
//      await(populateAllSegments("mirror-2021-01-05.json"))
//      await(populateAllSegments("mirror-2021-01-06.json"))
      println(s"End import")

      val changeList = await(posTable.segmentExchanges(LocalDate.parse("2021-01-02")))
      val map        = changeList.map(mirrorConfig => mirrorConfig.date.toString -> mirrorConfig.segments.toSet).toMap

      assert(map("2021-01-02") == Set(SegmentConfig("F1", None), SegmentConfig("F2", None)))
      assert(
        map("2021-01-03") == Set(
          SegmentConfig("A8", Some("SN-393")),
          SegmentConfig("A11", Some("SN-400")),
          SegmentConfig("B19", Some("SN-404")),
          SegmentConfig("B24", Some("SN-408")),
          SegmentConfig("C12", Some("SN-397")),
          SegmentConfig("C20", Some("SN-224")),
          SegmentConfig("D7", None),
          SegmentConfig("D14", Some("SN-399")),
          SegmentConfig("E9", Some("SN-395")),
          SegmentConfig("F1", Some("SN-018")),
          SegmentConfig("F2", Some("SN-455")),
          SegmentConfig("G8", None),
          SegmentConfig("G9", None),
          SegmentConfig("G11", None),
          SegmentConfig("G12", None),
          SegmentConfig("G14", None),
          SegmentConfig("G19", None),
          SegmentConfig("G20", None),
          SegmentConfig("G24", None)
        )
      )
    }
  }
}
