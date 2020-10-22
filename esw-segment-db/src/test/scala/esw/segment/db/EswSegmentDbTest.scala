package esw.segment.db

import java.sql.Date

import esw.segment.db.SegmentToM1PosTable.{DateRange, SegmentToM1Pos}
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration._
import scala.async.Async.{async, await}
import scala.concurrent.Await

class EswSegmentDbTest extends AnyFunSuite {

  private val wiring = new Wiring()
  import wiring.ec
  private val segmentDb = wiring.segmentDb
  private val log = wiring.log

  test("Database access") {
    log.info("Inserting data in table: segment_to_m1_pos")
    val date1 = Date.valueOf("2020-10-21")
    val date2 = Date.valueOf("2020-10-22")
    val dateRange1 = DateRange(date1, date1)
    val dateRange2 = DateRange(date1, date2)
    val s5 = SegmentToM1Pos(date1, "SN0005", 4)
    val s7 = SegmentToM1Pos(date1, "SN0007", 2)
    val s5b = SegmentToM1Pos(date2, "SN0005", 23)
    val s7b = SegmentToM1Pos(date2, "SN0007", 123)
    val doneF = async {
      assert(await(segmentDb.setPosition(s7)))
      assert(await(segmentDb.setPosition(s5)))
      assert(await(segmentDb.setPosition(s5b)))
      assert(await(segmentDb.setPosition(s7b)))

      assert(await(segmentDb.getSegmentPositions(dateRange1, "SN0007")) == List(s7))
      assert(await(segmentDb.getSegmentIds(dateRange1, 2)) == List(s7))
      assert(await(segmentDb.getSegmentPositions(dateRange1, "SN0005")) == List(s5))
      assert(await(segmentDb.getSegmentIds(dateRange1, 4)) == List (s5))
      assert(await(segmentDb.getSegmentIds(dateRange1, 8)).isEmpty)
      assert(await(segmentDb.getSegmentPositions(dateRange1, "SN0008")).isEmpty)

      assert(await(segmentDb.getSegmentPositions(dateRange2, "SN0005")) == List(s5, s5b))
      assert(await(segmentDb.getSegmentPositions(dateRange2, "SN0007")) == List(s7, s7b))

    }
    Await.ready(doneF, 10.seconds)
  }
}
