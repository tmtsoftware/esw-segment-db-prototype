package esw.segment.db

import java.sql.Date

import csw.logging.api.scaladsl.Logger
import esw.segment.db.SegmentToM1PosTable.{DateRange, SegmentToM1Pos}
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration._
import scala.async.Async.{async, await}
import scala.concurrent.{Await, Future}

class EswSegmentDbTest extends AnyFunSuite {
  // XXX TODO Create a new test database each time
  //     val wiring = new DbWiring("test_segment_db")
  val wiring = new DbWiring()

  import wiring.ec

  val posTable: SegmentToM1PosTable = wiring.segmentToM1PosTable
  val log: Logger = wiring.log

  private val dateRange1 = DateRange(Date.valueOf("2020-10-21"), Date.valueOf("2020-10-21"))
  private val dateRange2 = DateRange(Date.valueOf("2020-10-21"), Date.valueOf("2020-10-22"))
  private val dateRange3 = DateRange(Date.valueOf("2020-10-05"), Date.valueOf("2020-10-06"))
  private val dateRange4 = DateRange(Date.valueOf("2020-10-01"), Date.valueOf("2020-10-06"))
  private val dateRange5 = DateRange(Date.valueOf("2020-10-07"), Date.valueOf("2020-10-07"))

  private def populateSegmentToM1PosTable(): Future[Unit] = async {
    assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-01"), "SN0002", 4))))
    assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-02"), "SN0003", 4))))
    assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-03"), "SN0003", 4))))
    assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-04"), "SN0003", 4))))
    assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-05"), "SN0004", 4))))
    assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-06"), "SN0004", 4))))
    assert(await(posTable.setPosition(SegmentToM1Pos(Date.valueOf("2020-10-07"), None, 4))))
    assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-08"), "SN0004", 4))))
    assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-09"), "SN0004", 4))))
    assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0004", 4))))
    assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0007", 2))))
    assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0005", 5))))
    assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN0004", 4))))
    assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN0005", 23))))
    assert(await(posTable.setPosition(new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN0007", 123))))
  }

  test("Test segment_to_m1_pos table operations") {
    val doneF = async {
      await(populateSegmentToM1PosTable())

      assert(await(posTable.segmentPositions(dateRange1, "SN0007")) == List(
        new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0007", 2)))
      assert(await(posTable.segmentIds(dateRange1, 2)) == List(
        new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0007", 2)))
      assert(await(posTable.segmentPositions(dateRange1, "SN0005")) == List(
        new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0005", 5)))

      assert(await(posTable.segmentIds(dateRange1, 5)) == List(
        new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0005", 5)))
      assert(await(posTable.segmentIds(dateRange1, 8)).isEmpty)
      assert(await(posTable.segmentIds(dateRange1, 4)) == List(
        new SegmentToM1Pos(Date.valueOf("2020-10-08"), "SN0004", 4)))
      assert(await(posTable.segmentIds(dateRange3, 4)) == List(
        new SegmentToM1Pos(Date.valueOf("2020-10-05"), "SN0004", 4)))
      assert(await(posTable.segmentIds(dateRange4, 4)) == List(
        new SegmentToM1Pos(Date.valueOf("2020-10-01"), "SN0002", 4),
        new SegmentToM1Pos(Date.valueOf("2020-10-02"), "SN0003", 4),
        new SegmentToM1Pos(Date.valueOf("2020-10-05"), "SN0004", 4)
      ))
      assert(await(posTable.segmentIds(dateRange4, 13)).isEmpty)
      assert(await(posTable.segmentIds(dateRange5, 4)).isEmpty)

      assert(await(posTable.segmentPositions(dateRange1, "SN0008")).isEmpty)
      assert(await(posTable.segmentPositions(dateRange5, "SN0004")).isEmpty)
      assert(await(posTable.segmentPositions(dateRange2, "SN0005")) == List(
        new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0005", 5),
        new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN0005", 23)))
      assert(await(posTable.segmentPositions(dateRange2, "SN0007")) == List(
        new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0007", 2),
        new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN0007", 123)))
      assert(await(posTable.segmentPositions(dateRange2, "SN0004")) == List(
        new SegmentToM1Pos(Date.valueOf("2020-10-08"), "SN0004", 4)))

      assert(await(posTable.newlyInstalledSegments(Date.valueOf("2020-10-21"))) == List(
        new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0007", 2),
        new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0005", 5),
        new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN0005", 23),
        new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN0007", 123)
      ))

      assert(await(posTable.currentPositions()) == List(
        new SegmentToM1Pos(Date.valueOf("2020-10-08"), "SN0004", 4),
        new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN0005", 23),
        new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN0007", 123)
      ))

      assert(await(posTable.currentSegmentPosition("SN0007"))
        .contains(new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN0007", 123)))

      assert(await(posTable.currentSegmentPosition("SN0004"))
        .contains(new SegmentToM1Pos(Date.valueOf("2020-10-08"), "SN0004", 4)))

      assert(await(posTable.currentSegmentPosition("SN0005"))
        .contains(new SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN0005", 23)))

      assert(await(posTable.positionsOnDate(Date.valueOf("2020-10-04"))) == List(
        new SegmentToM1Pos(Date.valueOf("2020-10-02"), "SN0003", 4),
      ))
      assert(await(posTable.positionsOnDate(Date.valueOf("2020-10-21"))) == List(
        new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0007", 2),
        new SegmentToM1Pos(Date.valueOf("2020-10-08"), "SN0004", 4),
        new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0005", 5)
      ))

      assert(await(posTable.segmentPositionOnDate(Date.valueOf("2020-10-04"), "SN0003"))
        .contains(new SegmentToM1Pos(Date.valueOf("2020-10-02"), "SN0003", 4)))
      assert(await(posTable.segmentPositionOnDate(Date.valueOf("2020-10-21"), "SN0007"))
        .contains(new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0007", 2)))

      assert(await(posTable.segmentAtPositionOnDate(Date.valueOf("2020-10-04"), 4))
        .contains(new SegmentToM1Pos(Date.valueOf("2020-10-02"), "SN0003", 4)))
      assert(await(posTable.segmentAtPositionOnDate(Date.valueOf("2020-10-21"), 2))
        .contains(new SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0007", 2)))
    }
    Await.result(doneF, 1000.seconds)
  }
}
