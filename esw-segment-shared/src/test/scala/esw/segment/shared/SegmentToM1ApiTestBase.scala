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

  private def populateSomeSegments(): Future[Unit] = async {
    assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-01"), "SN0002", "A4"))))
    assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-02"), "SN0003", "A4"))))
    assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-03"), "SN0003", "A4"))))
    assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-04"), "SN0003", "A4"))))
    assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-05"), "SN0004", "A4"))))
    assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-06"), "SN0004", "A4"))))
    assert(await(posTable.setPosition(SegmentToM1Pos(Date.valueOf("2020-10-07"), None, "A4"))))
    assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-08"), "SN0004", "A4"))))
    assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-09"), "SN0004", "A4"))))
    assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0004", "A4"))))
    assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0007", "A2"))))
    assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0005", "F5"))))
    assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN0004", "A4"))))
    assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN0005", "B23"))))
    assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN0007", "F82"))))
    assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-23"), "SN0008", "A12"))))
  }

  private def populateAllSegments(date: Date): Future[Unit] = async {
    val positions = (1 to numSegments).toList.map(n => Some(f"SN$n%04d"))
    assert(await(posTable.setAllPositions(date, positions)))
  }

  test("Test with all segment positions") {
    async {
      assert(await(posTable.resetTables()))
      await(populateAllSegments(dateRange1.from))

      assert(await(posTable.segmentPositions(dateRange1, "SN0007")) == List(
        new SegmentToM1Pos(dateRange1.from, "SN0007", "A7")))
      assert(await(posTable.segmentPositions(dateRange1, "SN0492")) == List(
        new SegmentToM1Pos(dateRange1.from, "SN0492", "F82")))

      assert(await(posTable.segmentIds(dateRange1, "F5")) == List(
        new SegmentToM1Pos(dateRange1.from, "SN0415", "F5")))
      assert(await(posTable.segmentIds(dateRange1, "A6")) == List(
        new SegmentToM1Pos(dateRange1.from, "SN0006", "A6")))

      // Set some more segment positions (the previous positions are automatically inherited/removed as needed)
      await(populateSomeSegments())
      assert(await(posTable.segmentPositions(dateRange1, "SN0007")) == List(
        new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0007", "A2")))
      // "SN0002" was replaced with "SN0007"
      assert(await(posTable.segmentPositions(dateRange1, "SN0002")).isEmpty)
      // Previous "SN0007" position now empty
      assert(await(posTable.segmentIds(dateRange1, "A7")).isEmpty)
    }
  }

  test("Test with single segment positions") {
    async {
      assert(await(posTable.resetTables()))
      await(populateSomeSegments())

      assert(await(posTable.segmentPositions(dateRange1, "SN0007")) == List(
        new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0007", "A2")))
      assert(await(posTable.segmentIds(dateRange1, "A2")) == List(
        new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0007", "A2")))
      assert(await(posTable.segmentPositions(dateRange1, "SN0005")) == List(
        new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0005", "F5")))

      assert(await(posTable.segmentIds(dateRange1, "F5")) == List(
        new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0005", "F5")))
      assert(await(posTable.segmentIds(dateRange1, "A8")).isEmpty)
      assert(await(posTable.segmentIds(dateRange1, "A4")) == List(
        new SegmentToM1Pos(Date.valueOf("2020-10-08"), "SN0004", "A4")))
      assert(await(posTable.segmentIds(dateRange3, "A4")) == List(
        new SegmentToM1Pos(Date.valueOf("2020-10-05"), "SN0004", "A4")))
      assert(await(posTable.segmentIds(dateRange4, "A4")).toSet == Set(
        new SegmentToM1Pos(Date.valueOf("2020-10-01"), "SN0002", "A4"),
        new SegmentToM1Pos(Date.valueOf("2020-10-02"), "SN0003", "A4"),
        new SegmentToM1Pos(Date.valueOf("2020-10-05"), "SN0004", "A4")
      ))
      assert(await(posTable.segmentIds(dateRange4, "A13")).isEmpty)
      assert(await(posTable.segmentIds(dateRange5, "A4")).isEmpty)

      assert(await(posTable.segmentPositions(dateRange1, "SN0008")).isEmpty)
      assert(await(posTable.segmentPositions(dateRange5, "SN0004")).isEmpty)
      assert(await(posTable.segmentPositions(dateRange2, "SN0005")).toSet == Set(
        new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0005", "F5"),
        new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN0005", "B23")))
      assert(await(posTable.segmentPositions(dateRange2, "SN0007")).toSet == Set(
        new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0007", "A2"),
        new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN0007", "F82")))
      assert(await(posTable.segmentPositions(dateRange2, "SN0004")) == List(
        new SegmentToM1Pos(Date.valueOf("2020-10-08"), "SN0004", "A4")))

      assert(await(posTable.newlyInstalledSegments(Date.valueOf("2020-10-21"))).toSet == Set(
        new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0007", "A2"),
        new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0005", "F5"),
        new SegmentToM1Pos(Date.valueOf("2020-10-23"), "SN0008", "A12"),
        new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN0005", "B23"),
        new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN0007", "F82"),
      ))

      assert(await(posTable.currentPositions()).toSet == Set(
        new SegmentToM1Pos(Date.valueOf("2020-10-08"), "SN0004", "A4"),
        new SegmentToM1Pos(Date.valueOf("2020-10-23"), "SN0008", "A12"),
        new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN0005", "B23"),
        new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN0007", "F82")
      ))

      assert(await(posTable.currentSegmentPosition("SN0007"))
        .contains(new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN0007", "F82")))

      assert(await(posTable.currentSegmentPosition("SN0004"))
        .contains(new SegmentToM1Pos(Date.valueOf("2020-10-08"), "SN0004", "A4")))

      assert(await(posTable.currentSegmentPosition("SN0005"))
        .contains(new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN0005", "B23")))

      assert(await(posTable.positionsOnDate(Date.valueOf("2020-10-04"))) == List(
        new SegmentToM1Pos(Date.valueOf("2020-10-02"), "SN0003", "A4"),
      ))
      assert(await(posTable.positionsOnDate(Date.valueOf("2020-10-21"))).toSet == Set(
        new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0007", "A2"),
        new SegmentToM1Pos(Date.valueOf("2020-10-08"), "SN0004", "A4"),
        new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0005", "F5")
      ))

      assert(await(posTable.positionsOnDate(Date.valueOf("2020-10-23"))).toSet == Set(
        new SegmentToM1Pos(Date.valueOf("2020-10-08"), "SN0004", "A4"),
        new SegmentToM1Pos(Date.valueOf("2020-10-23"), "SN0008", "A12"),
        new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN0005", "B23"),
        new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN0007", "F82")
      ))

      assert(await(posTable.segmentPositionOnDate(Date.valueOf("2020-10-04"), "SN0003"))
        .contains(new SegmentToM1Pos(Date.valueOf("2020-10-02"), "SN0003", "A4")))
      assert(await(posTable.segmentPositionOnDate(Date.valueOf("2020-10-21"), "SN0007"))
        .contains(new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0007", "A2")))

      assert(await(posTable.segmentAtPositionOnDate(Date.valueOf("2020-10-04"), "A4"))
        .contains(new SegmentToM1Pos(Date.valueOf("2020-10-02"), "SN0003", "A4")))
      assert(await(posTable.segmentAtPositionOnDate(Date.valueOf("2020-10-21"), "A2"))
        .contains(new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0007", "A2")))
    }
  }

}
