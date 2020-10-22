package esw.segment.db

import java.sql.Timestamp

import esw.segment.db.SegmentToM1PosTable.TimeRange
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
    val timestamp1 = Timestamp.valueOf("2020-10-21 18:35:10")
    val timestamp2 = Timestamp.valueOf("2020-10-22 10:17:12")
    val timeRange1 = TimeRange(timestamp1, timestamp1)
    val timeRange2 = TimeRange(timestamp1, timestamp2)
    val s5 = SegmentToM1Pos(timestamp1, "SN0005", 4)
    val s7 = SegmentToM1Pos(timestamp1, "SN0007", 2)
    val s5b = SegmentToM1Pos(timestamp2, "SN0005", 23)
    val s7b = SegmentToM1Pos(timestamp2, "SN0007", 123)
    val doneF = async {
      assert(await(segmentDb.setPosition(s7)))
      assert(await(segmentDb.setPosition(s5)))
      assert(await(segmentDb.setPosition(s5b)))
      assert(await(segmentDb.setPosition(s7b)))

      assert(await(segmentDb.getSegmentPositions(timeRange1, "SN0007")) == List(s7))
      assert(await(segmentDb.getSegmentPositions(timeRange1, 2)) == List(s7))
      assert(await(segmentDb.getSegmentPositions(timeRange1, "SN0005")) == List(s5))
      assert(await(segmentDb.getSegmentPositions(timeRange1, 4)) == List (s5))
      assert(await(segmentDb.getSegmentPositions(timeRange1, 8)).isEmpty)
      assert(await(segmentDb.getSegmentPositions(timeRange1, "SN0008")).isEmpty)

      assert(await(segmentDb.getSegmentPositions(timeRange2, "SN0005")) == List(s5, s5b))
      assert(await(segmentDb.getSegmentPositions(timeRange2, "SN0007")) == List(s7, s7b))

    }
    Await.ready(doneF, 10.seconds)
  }
}
