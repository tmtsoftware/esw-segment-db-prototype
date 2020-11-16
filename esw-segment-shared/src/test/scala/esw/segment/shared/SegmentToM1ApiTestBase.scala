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
      assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-01"), "SN01-2", "A4"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-02"), "SN01-3", "A4"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-03"), "SN01-3", "A4"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-04"), "SN01-3", "A4"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-05"), "SN04-1", "A4"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-06"), "SN04-1", "A4"))))
      assert(await(posTable.setPosition(SegmentToM1Pos(Date.valueOf("2020-10-07"), None, "A4"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-08"), "SN04-1", "A4"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-09"), "SN04-1", "A4"))))

      assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN04-1", "A4"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN07-1", "A2"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN05-1", "F5"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN04-1", "A4"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN05-1", "B23"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN07-1", "F82"))))
      assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-23"), "SN08-1", "A12"))))

      // Insert row before the current ones to test update of following undefined positions
      assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-10"), "SN32-1", "A32"))))
      assert(await(posTable.setPosition(SegmentToM1Pos(Date.valueOf("2020-10-10"), None, "A33"))))

    }

  private def populateAllSegments(date: Date): Future[Unit] =
    async {
      val positions = (1 to numSegments).toList.map{ n =>
        val sectorOffset = (n - 1) / 82 + 1
        val pos          = (n - 1) % segmentsPerSector + 1
        Some(f"SN$pos%02d-$sectorOffset")
      }
      assert(await(posTable.setAllPositions(date, positions)))
    }

  test("Test with single segment positions") {
    async {
      assert(await(posTable.resetTables()))
      await(populateSomeSegments())

      assert(await(posTable.mostRecentChange()) == Date.valueOf("2020-10-23"))

      assert(
        await(posTable.segmentPositions(dateRange1, "SN07-1")) == List(
          new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN07-1", "A2")
        )
      )
      assert(await(posTable.segmentIds(dateRange1, "A2")) == List(new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN07-1", "A2")))
      assert(
        await(posTable.segmentPositions(dateRange1, "SN05-1")) == List(
          new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN05-1", "F5")
        )
      )
      assert(
        await(posTable.segmentPositions(dateRange1, "SN32-1")) == List(
          new SegmentToM1Pos(Date.valueOf("2020-10-10"), "SN32-1", "A32")
        )
      )

      assert(await(posTable.segmentIds(dateRange1, "F5")) == List(new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN05-1", "F5")))
      assert(await(posTable.segmentIds(dateRange1, "A8")).isEmpty)
      assert(await(posTable.segmentIds(dateRange1, "A4")) == List(new SegmentToM1Pos(Date.valueOf("2020-10-08"), "SN04-1", "A4")))
      //
      assert(
        await(posTable.segmentIds(dateRange1, "A32")) == List(new SegmentToM1Pos(Date.valueOf("2020-10-10"), "SN32-1", "A32"))
      )
      assert(await(posTable.segmentIds(dateRange1, "A33")).isEmpty)
      assert(await(posTable.segmentIds(dateRange3, "A4")) == List(new SegmentToM1Pos(Date.valueOf("2020-10-05"), "SN04-1", "A4")))
      assert(
        await(posTable.segmentIds(dateRange4, "A4")).toSet == Set(
          new SegmentToM1Pos(Date.valueOf("2020-10-01"), "SN01-2", "A4"),
          new SegmentToM1Pos(Date.valueOf("2020-10-02"), "SN01-3", "A4"),
          new SegmentToM1Pos(Date.valueOf("2020-10-05"), "SN04-1", "A4")
        )
      )
      assert(await(posTable.segmentIds(dateRange4, "A13")).isEmpty)
      assert(await(posTable.segmentIds(dateRange5, "A4")).isEmpty)

      assert(await(posTable.segmentPositions(dateRange1, "SN08-1")).isEmpty)
      assert(await(posTable.segmentPositions(dateRange5, "SN04-1")).isEmpty)
      assert(
        await(posTable.segmentPositions(dateRange2, "SN05-1")).toSet == Set(
          new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN05-1", "F5"),
          new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN05-1", "B23")
        )
      )
      assert(
        await(posTable.segmentPositions(dateRange2, "SN07-1")).toSet == Set(
          new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN07-1", "A2"),
          new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN07-1", "F82")
        )
      )
      assert(
        await(posTable.segmentPositions(dateRange2, "SN04-1")) == List(
          new SegmentToM1Pos(Date.valueOf("2020-10-08"), "SN04-1", "A4")
        )
      )

      assert(
        await(posTable.newlyInstalledSegments(Date.valueOf("2020-10-21"))).toSet == Set(
          new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN07-1", "A2"),
          new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN05-1", "F5"),
          new SegmentToM1Pos(Date.valueOf("2020-10-23"), "SN08-1", "A12"),
          new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN05-1", "B23"),
          new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN07-1", "F82")
        )
      )

      val currentPositions = await(posTable.currentPositions()).toSet
      assert(currentPositions.size == EswSegmentData.numSegments)
      assert(
        currentPositions.filter(_.maybeId.isDefined) == Set(
          new SegmentToM1Pos(Date.valueOf("2020-10-08"), "SN04-1", "A4"),
          new SegmentToM1Pos(Date.valueOf("2020-10-23"), "SN08-1", "A12"),
          new SegmentToM1Pos(Date.valueOf("2020-10-10"), "SN32-1", "A32"),
          new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN05-1", "B23"),
          new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN07-1", "F82")
        )
      )

      assert(
        await(posTable.currentSegmentPosition("SN07-1"))
          .contains(new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN07-1", "F82"))
      )

      assert(
        await(posTable.currentSegmentPosition("SN32-1"))
          .contains(new SegmentToM1Pos(Date.valueOf("2020-10-10"), "SN32-1", "A32"))
      )

      assert(
        await(posTable.currentSegmentPosition("SN04-1"))
          .contains(new SegmentToM1Pos(Date.valueOf("2020-10-08"), "SN04-1", "A4"))
      )

      assert(
        await(posTable.currentSegmentPosition("SN05-1"))
          .contains(new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN05-1", "B23"))
      )

      assert(
        await(posTable.positionsOnDate(Date.valueOf("2020-10-04"))).filter(_.maybeId.isDefined) == List(
          new SegmentToM1Pos(Date.valueOf("2020-10-02"), "SN01-3", "A4")
        )
      )
      assert(
        await(posTable.positionsOnDate(Date.valueOf("2020-10-21"))).toSet.filter(_.maybeId.isDefined) == Set(
          new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN07-1", "A2"),
          new SegmentToM1Pos(Date.valueOf("2020-10-08"), "SN04-1", "A4"),
          new SegmentToM1Pos(Date.valueOf("2020-10-10"), "SN32-1", "A32"),
          new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN05-1", "F5")
        )
      )

      assert(
        await(posTable.positionsOnDate(Date.valueOf("2020-10-23"))).toSet.filter(_.maybeId.isDefined) == Set(
          new SegmentToM1Pos(Date.valueOf("2020-10-08"), "SN04-1", "A4"),
          new SegmentToM1Pos(Date.valueOf("2020-10-10"), "SN32-1", "A32"),
          new SegmentToM1Pos(Date.valueOf("2020-10-23"), "SN08-1", "A12"),
          new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN05-1", "B23"),
          new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN07-1", "F82")
        )
      )

      assert(
        await(posTable.segmentPositionOnDate(Date.valueOf("2020-10-04"), "SN01-3"))
          .contains(new SegmentToM1Pos(Date.valueOf("2020-10-02"), "SN01-3", "A4"))
      )
      assert(
        await(posTable.segmentPositionOnDate(Date.valueOf("2020-10-21"), "SN07-1"))
          .contains(new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN07-1", "A2"))
      )

      assert(
        await(posTable.segmentAtPositionOnDate(Date.valueOf("2020-10-04"), "A4"))
          .contains(new SegmentToM1Pos(Date.valueOf("2020-10-02"), "SN01-3", "A4"))
      )
      assert(
        await(posTable.segmentAtPositionOnDate(Date.valueOf("2020-10-21"), "A2"))
          .contains(new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN07-1", "A2"))
      )
    }

  }

  test("Test with all segment positions") {
    async {
      assert(await(posTable.resetTables()))
      await(populateAllSegments(dateRange1.from))

      assert(await(posTable.mostRecentChange()) == dateRange1.from)

      assert(await(posTable.segmentPositions(dateRange1, "SN07-1")) == List(
        new SegmentToM1Pos(dateRange1.from, "SN07-1", "A7")))
      assert(await(posTable.segmentPositions(dateRange1, "SN82-1")) == List(
        new SegmentToM1Pos(dateRange1.from, "SN82-1", "A82")))

      assert(await(posTable.segmentIds(dateRange1, "A6")) == List(
        new SegmentToM1Pos(dateRange1.from, "SN06-1", "A6")))

      // Set some more segment positions (the previous positions are automatically inherited/removed as needed)
      await(populateSomeSegments())
      assert(await(posTable.segmentPositions(dateRange1, "SN07-1")) == List(
        new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN07-1", "A2")))
      // "SN02-1" was replaced with "SN07-1 (A2)"
      assert(await(posTable.segmentPositions(dateRange1, "SN02-1")).isEmpty)
      // Previous "SN07-1" position now empty
      assert(await(posTable.segmentIds(dateRange1, "A7")).isEmpty)
    }
  }

}
