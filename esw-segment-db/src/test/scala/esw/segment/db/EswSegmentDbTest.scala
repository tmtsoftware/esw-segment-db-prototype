package esw.segment.db

import java.sql.Date

import esw.segment.db.SegmentToM1PosTable.{DateRange, SegmentToM1Pos}
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration._
import scala.async.Async.{async, await}
import scala.concurrent.Await
import scala.util.{Failure, Success}

class EswSegmentDbTest extends AnyFunSuite {

  private val wiring = new Wiring()

  import wiring.ec

  private val segmentDb = wiring.segmentDb
  private val log = wiring.log

  test("Database access") {
    log.info("Inserting data in table: segment_to_m1_pos")
    val doneF = async {
      assert(await(segmentDb.setPosition(SegmentToM1Pos(Date.valueOf("2020-10-01"), "SN0002", 4))))
      assert(await(segmentDb.setPosition(SegmentToM1Pos(Date.valueOf("2020-10-02"), "SN0003", 4))))
      assert(await(segmentDb.setPosition(SegmentToM1Pos(Date.valueOf("2020-10-03"), "SN0003", 4))))
      assert(await(segmentDb.setPosition(SegmentToM1Pos(Date.valueOf("2020-10-04"), "SN0003", 4))))
      assert(await(segmentDb.setPosition(SegmentToM1Pos(Date.valueOf("2020-10-05"), "SN0004", 4))))
      assert(await(segmentDb.setPosition(SegmentToM1Pos(Date.valueOf("2020-10-06"), "SN0004", 4))))
      assert(await(segmentDb.setPosition(SegmentToM1Pos(Date.valueOf("2020-10-07"), "null", 4))))
      assert(await(segmentDb.setPosition(SegmentToM1Pos(Date.valueOf("2020-10-08"), "SN0004", 4))))
      assert(await(segmentDb.setPosition(SegmentToM1Pos(Date.valueOf("2020-10-09"), "SN0004", 4))))
      assert(await(segmentDb.setPosition(SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0004", 4))))
      assert(await(segmentDb.setPosition(SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0007", 2))))
      assert(await(segmentDb.setPosition(SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0005", 5))))
      assert(await(segmentDb.setPosition(SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN0004", 4))))
      assert(await(segmentDb.setPosition(SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN0005", 23))))
      assert(await(segmentDb.setPosition(SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN0007", 123))))

      val dateRange1 = DateRange(Date.valueOf("2020-10-21"), Date.valueOf("2020-10-21"))
      val dateRange2 = DateRange(Date.valueOf("2020-10-21"), Date.valueOf("2020-10-22"))
      val dateRange3 = DateRange(Date.valueOf("2020-10-05"), Date.valueOf("2020-10-06"))
      val dateRange4 = DateRange(Date.valueOf("2020-10-01"), Date.valueOf("2020-10-06"))

      assert(await(segmentDb.getSegmentPositions(dateRange1, "SN0007")) == List(
        SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0007", 2)))
      assert(await(segmentDb.getSegmentIds(dateRange1, 2)) == List(
        SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0007", 2)))
      assert(await(segmentDb.getSegmentPositions(dateRange1, "SN0005")) == List(
        SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0005", 5)))
      assert(await(segmentDb.getSegmentIds(dateRange1, 5)) == List(
        SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0005", 5)))
      assert(await(segmentDb.getSegmentIds(dateRange1, 8)).isEmpty)
      assert(await(segmentDb.getSegmentIds(dateRange1, 4)) == List(
        SegmentToM1Pos(Date.valueOf("2020-10-08"), "SN0004", 4)))
      assert(await(segmentDb.getSegmentIds(dateRange3, 4)) == List(
        SegmentToM1Pos(Date.valueOf("2020-10-05"), "SN0004", 4)))
      assert(await(segmentDb.getSegmentIds(dateRange4, 4)) == List(
        SegmentToM1Pos(Date.valueOf("2020-10-01"), "SN0002", 4),
        SegmentToM1Pos(Date.valueOf("2020-10-02"), "SN0003", 4),
        SegmentToM1Pos(Date.valueOf("2020-10-05"), "SN0004", 4)
      ))

      assert(await(segmentDb.getSegmentPositions(dateRange1, "SN0008")).isEmpty)
      assert(await(segmentDb.getSegmentPositions(dateRange2, "SN0005")) == List(
        SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0005", 5),
        SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN0005", 23)))
      assert(await(segmentDb.getSegmentPositions(dateRange2, "SN0007")) == List(
        SegmentToM1Pos(Date.valueOf("2020-10-21"), "SN0007", 2),
        SegmentToM1Pos(Date.valueOf("2020-10-22"), "SN0007", 123)))
      assert(await(segmentDb.getSegmentPositions(dateRange2, "SN0004")) == List(
        SegmentToM1Pos(Date.valueOf("2020-10-08"), "SN0004", 4)))

    }
    Await.result(doneF, 1000.seconds)
  }
}
