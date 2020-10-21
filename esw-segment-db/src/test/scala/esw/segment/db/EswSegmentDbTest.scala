package esw.segment.db

import java.sql.Timestamp
import java.time.Instant

import org.scalatest.funsuite.AnyFunSuite
import csw.database.scaladsl.JooqExtentions._
import SegmentDb._
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
    val timestamp = Timestamp.valueOf("2020-10-21 18:35:10")
    val s7 = SegmentToM1Pos(timestamp, "SN0007", 2)
    val s5 = SegmentToM1Pos(timestamp, "SN0005", 4)
    val doneF = async {
      assert(await(segmentDb.setSegmentToM1Pos(s7)))
      assert(await(segmentDb.setSegmentToM1Pos(s5)))
      assert(await(segmentDb.getSegmentToM1Pos(timestamp, "SN0007")).contains(s7))
      assert(await(segmentDb.getSegmentToM1Pos(timestamp, 2)).contains(s7))
      assert(await(segmentDb.getSegmentToM1Pos(timestamp, "SN0005")).contains(s5))
      assert(await(segmentDb.getSegmentToM1Pos(timestamp, 4)).contains(s5))
      assert(await(segmentDb.getSegmentToM1Pos(timestamp, 8)).isEmpty)
      assert(await(segmentDb.getSegmentToM1Pos(timestamp, "SN0008")).isEmpty)
    }
    Await.ready(doneF, 10.seconds)
  }
}
