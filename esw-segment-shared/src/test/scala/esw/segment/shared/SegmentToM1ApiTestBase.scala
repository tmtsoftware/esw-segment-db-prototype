package esw.segment.shared

import java.sql.Date

import esw.segment.shared.EswSegmentData.{DateRange, SegmentToM1Pos}
import EswSegmentData._
import org.scalatest.funsuite.AsyncFunSuite

import scala.async.Async.{async, await}
import scala.concurrent.Future

class SegmentToM1ApiTestBase(posTable: SegmentToM1Api) extends AsyncFunSuite {
  private val dateRange1 = DateRange(Date.valueOf("2020-10-21"), Date.valueOf("2020-10-21"))
  private val dateRange2 = DateRange(Date.valueOf("2020-10-21"), Date.valueOf("2020-10-22"))
  private val dateRange3 = DateRange(Date.valueOf("2020-10-05"), Date.valueOf("2020-10-06"))
  private val dateRange4 = DateRange(Date.valueOf("2020-10-01"), Date.valueOf("2020-10-06"))
  private val dateRange5 = DateRange(Date.valueOf("2020-10-07"), Date.valueOf("2020-10-07"))

  private def populateSomeSegments(): Future[Unit] =
    async {
      assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-01"), "SN-002", "A4"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-02"), "SN-003", "A4"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-03"), "SN-003", "A4"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-04"), "SN-003", "A4"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-05"), "SN-004", "A4"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-06"), "SN-004", "A4"))))
      assert(await(posTable.setPosition(SegmentToM1Pos(Date.valueOf("2020-10-07"), None, "A4"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-08"), "SN-004", "A4"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-09"), "SN-004", "A4"))))

      assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN-004", "A4"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN-007", "A2"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN-005", "F5"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN-004", "A4"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN-005", "B23"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN-007", "F82"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-23"), "SN-008", "A12"))))

      // Insert row before the current ones to test update of following undefined positions
      assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-10"), "SN-032", "A32"))))
      assert(await(posTable.setPosition(SegmentToM1Pos(Date.valueOf("2020-10-10"), None, "A33"))))

    }

  private def populateAllSegments(date: Date): Future[Unit] =
    async {
      val positions = (1 to numSegments).toList.map(n => Some(f"SN-$n%03d"))
      assert(await(posTable.setAllPositions(date, positions)))
    }

  test("Test with single segment positions") {
    async {
      assert(await(posTable.resetTables()))
      await(populateSomeSegments())

      assert(await(posTable.mostRecentChange()) == Date.valueOf("2020-10-23"))

      assert(
        await(posTable.segmentPositions(dateRange1, "SN-007")) == List(
          new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN-007", "A2")
        )
      )
      assert(await(posTable.segmentIds(dateRange1, "A2")) == List(new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN-007", "A2")))
      assert(
        await(posTable.segmentPositions(dateRange1, "SN-005")) == List(
          new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN-005", "F5")
        )
      )
      assert(
        await(posTable.segmentPositions(dateRange1, "SN-032")) == List(
          new SegmentToM1Pos(Date.valueOf("2020-10-10"), "SN-032", "A32")
        )
      )

      assert(await(posTable.segmentIds(dateRange1, "F5")) == List(new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN-005", "F5")))
      assert(await(posTable.segmentIds(dateRange1, "A8")).isEmpty)
      assert(await(posTable.segmentIds(dateRange1, "A4")) == List(new SegmentToM1Pos(Date.valueOf("2020-10-08"), "SN-004", "A4")))
      //
      assert(
        await(posTable.segmentIds(dateRange1, "A32")) == List(new SegmentToM1Pos(Date.valueOf("2020-10-10"), "SN-032", "A32"))
      )
      assert(await(posTable.segmentIds(dateRange1, "A33")).isEmpty)
      assert(await(posTable.segmentIds(dateRange3, "A4")) == List(new SegmentToM1Pos(Date.valueOf("2020-10-05"), "SN-004", "A4")))
      assert(
        await(posTable.segmentIds(dateRange4, "A4")).toSet == Set(
          new SegmentToM1Pos(Date.valueOf("2020-10-01"), "SN-002", "A4"),
          new SegmentToM1Pos(Date.valueOf("2020-10-02"), "SN-003", "A4"),
          new SegmentToM1Pos(Date.valueOf("2020-10-05"), "SN-004", "A4")
        )
      )
      assert(await(posTable.segmentIds(dateRange4, "A13")).isEmpty)
      assert(await(posTable.segmentIds(dateRange5, "A4")).isEmpty)

      assert(await(posTable.segmentPositions(dateRange1, "SN-008")).isEmpty)
      assert(await(posTable.segmentPositions(dateRange5, "SN-004")).isEmpty)
      assert(
        await(posTable.segmentPositions(dateRange2, "SN-005")).toSet == Set(
          new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN-005", "F5"),
          new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN-005", "B23")
        )
      )
      assert(
        await(posTable.segmentPositions(dateRange2, "SN-007")).toSet == Set(
          new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN-007", "A2"),
          new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN-007", "F82")
        )
      )
      assert(
        await(posTable.segmentPositions(dateRange2, "SN-004")) == List(
          new SegmentToM1Pos(Date.valueOf("2020-10-08"), "SN-004", "A4")
        )
      )

      assert(
        await(posTable.newlyInstalledSegments(Date.valueOf("2020-10-21"))).toSet == Set(
          new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN-007", "A2"),
          new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN-005", "F5"),
          new SegmentToM1Pos(Date.valueOf("2020-10-23"), "SN-008", "A12"),
          new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN-005", "B23"),
          new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN-007", "F82")
        )
      )

      val currentPositions = await(posTable.currentPositions()).toSet
      assert(currentPositions.size == EswSegmentData.numSegments)
      assert(
        currentPositions.filter(_.maybeId.isDefined) == Set(
          new SegmentToM1Pos(Date.valueOf("2020-10-08"), "SN-004", "A4"),
          new SegmentToM1Pos(Date.valueOf("2020-10-23"), "SN-008", "A12"),
          new SegmentToM1Pos(Date.valueOf("2020-10-10"), "SN-032", "A32"),
          new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN-005", "B23"),
          new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN-007", "F82")
        )
      )

      assert(
        await(posTable.currentSegmentPosition("SN-007"))
          .contains(new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN-007", "F82"))
      )

      assert(
        await(posTable.currentSegmentPosition("SN-032"))
          .contains(new SegmentToM1Pos(Date.valueOf("2020-10-10"), "SN-032", "A32"))
      )

      assert(
        await(posTable.currentSegmentPosition("SN-004"))
          .contains(new SegmentToM1Pos(Date.valueOf("2020-10-08"), "SN-004", "A4"))
      )

      assert(
        await(posTable.currentSegmentPosition("SN-005"))
          .contains(new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN-005", "B23"))
      )

      assert(
        await(posTable.positionsOnDate(Date.valueOf("2020-10-04"))).filter(_.maybeId.isDefined) == List(
          new SegmentToM1Pos(Date.valueOf("2020-10-02"), "SN-003", "A4")
        )
      )
      assert(
        await(posTable.positionsOnDate(Date.valueOf("2020-10-21"))).toSet.filter(_.maybeId.isDefined) == Set(
          new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN-007", "A2"),
          new SegmentToM1Pos(Date.valueOf("2020-10-08"), "SN-004", "A4"),
          new SegmentToM1Pos(Date.valueOf("2020-10-10"), "SN-032", "A32"),
          new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN-005", "F5")
        )
      )

      assert(
        await(posTable.positionsOnDate(Date.valueOf("2020-10-23"))).toSet.filter(_.maybeId.isDefined) == Set(
          new SegmentToM1Pos(Date.valueOf("2020-10-08"), "SN-004", "A4"),
          new SegmentToM1Pos(Date.valueOf("2020-10-10"), "SN-032", "A32"),
          new SegmentToM1Pos(Date.valueOf("2020-10-23"), "SN-008", "A12"),
          new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN-005", "B23"),
          new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN-007", "F82")
        )
      )

      assert(
        await(posTable.segmentPositionOnDate(Date.valueOf("2020-10-04"), "SN-003"))
          .contains(new SegmentToM1Pos(Date.valueOf("2020-10-02"), "SN-003", "A4"))
      )
      assert(
        await(posTable.segmentPositionOnDate(Date.valueOf("2020-10-21"), "SN-007"))
          .contains(new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN-007", "A2"))
      )

      assert(
        await(posTable.segmentAtPositionOnDate(Date.valueOf("2020-10-04"), "A4"))
          .contains(new SegmentToM1Pos(Date.valueOf("2020-10-02"), "SN-003", "A4"))
      )
      assert(
        await(posTable.segmentAtPositionOnDate(Date.valueOf("2020-10-21"), "A2"))
          .contains(new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN-007", "A2"))
      )
    }

  }

  test("Test with all segment positions") {
    async {
      assert(await(posTable.resetTables()))
      await(populateAllSegments(dateRange1.from))

      assert(await(posTable.mostRecentChange()) == dateRange1.from)

      assert(await(posTable.segmentPositions(dateRange1, "SN-007")) == List(
        new SegmentToM1Pos(dateRange1.from, "SN-007", "A7")))
      assert(await(posTable.segmentPositions(dateRange1, "SN-492")) == List(
        new SegmentToM1Pos(dateRange1.from, "SN-492", "F82")))

      assert(await(posTable.segmentIds(dateRange1, "F5")) == List(
        new SegmentToM1Pos(dateRange1.from, "SN-415", "F5")))
      assert(await(posTable.segmentIds(dateRange1, "A6")) == List(
        new SegmentToM1Pos(dateRange1.from, "SN-006", "A6")))

      // Set some more segment positions (the previous positions are automatically inherited/removed as needed)
      await(populateSomeSegments())
      assert(await(posTable.segmentPositions(dateRange1, "SN-007")) == List(
        new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN-007", "A2")))
      // "SN-002" was replaced with "SN-007"
      assert(await(posTable.segmentPositions(dateRange1, "SN-002")).isEmpty)
      // Previous "SN-007" position now empty
      assert(await(posTable.segmentIds(dateRange1, "A7")).isEmpty)
    }
  }

}
