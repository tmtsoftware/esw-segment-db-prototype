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
    val date1 = Date.valueOf("2020-10-21")
    val date2 = Date.valueOf("2020-10-22")
    val s5 = SegmentToM1Pos(date1, "SN0005", 5)
    val s7 = SegmentToM1Pos(date1, "SN0007", 2)
    val s5b = SegmentToM1Pos(date2, "SN0005", 23)
    val s7b = SegmentToM1Pos(date2, "SN0007", 123)
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
      assert(await(segmentDb.setPosition(SegmentToM1Pos(date1, "SN0004", 4))))
      assert(await(segmentDb.setPosition(SegmentToM1Pos(date2, "SN0004", 4))))
      assert(await(segmentDb.setPosition(s7)))
      assert(await(segmentDb.setPosition(s5)))
      assert(await(segmentDb.setPosition(s5b)))
      assert(await(segmentDb.setPosition(s7b)))

      val dateRange1 = DateRange(date1, date1)
      val dateRange2 = DateRange(date1, date2)
      assert(await(segmentDb.getSegmentPositions(dateRange1, "SN0007")) == List(s7))
      assert(await(segmentDb.getSegmentIds(dateRange1, 2)) == List(s7))
      assert(await(segmentDb.getSegmentPositions(dateRange1, "SN0005")) == List(s5))
      assert(await(segmentDb.getSegmentIds(dateRange1, 5)) == List (s5))
      assert(await(segmentDb.getSegmentIds(dateRange1, 8)).isEmpty)
      assert(await(segmentDb.getSegmentPositions(dateRange1, "SN0008")).isEmpty)

      assert(await(segmentDb.getSegmentPositions(dateRange2, "SN0005")) == List(s5, s5b))
      assert(await(segmentDb.getSegmentPositions(dateRange2, "SN0007")) == List(s7, s7b))

    }
//    doneF.onComplete {
//      case Success(_) =>
//        println("Test Passed")
//      case Failure(ex) =>
//        println("Test Failed")
//        ex.printStackTrace()
//    }
    Await.result(doneF, 1000.seconds)
  }
}
