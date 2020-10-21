package esw.segment.db

import java.sql.Timestamp

import org.jooq.DSLContext
import csw.database.scaladsl.JooqExtentions._
import SegmentDb._

import scala.async.Async._
import scala.concurrent.{ExecutionContext, Future}

object SegmentDb {
  val numSegments = 492

}

class SegmentDb(dsl: DSLContext)(implicit ec: ExecutionContext) {

  // Returns true if there is an entry for the given timestamp in the segment_to_m1_pos table
  private def segmentToM1PosRowExists(timestamp: Timestamp): Future[Boolean] = async {
    await(dsl.resultQuery(s"SELECT COUNT(*) FROM segment_to_m1_pos WHERE timestamp = '$timestamp'")
      .fetchAsyncScala[Int]).head != 0
  }

  // Adds a row with only the timestamp and returns true if successful
  private def addSegmentToM1PosRow(timestamp: Timestamp): Future[Boolean] = async {
    await(
      dsl
        .query(s"INSERT INTO segment_to_m1_pos(timestamp) VALUES ('${timestamp.toString}')")
        .executeAsyncScala()
    ) == 1
  }

  /**
   * Sets the position of zero or more segments in the segment_to_m1_pos table and returns true if successful
   *
   * @param segmentToM1Pos holds the timestamp, id and pos
   */
  def setSegmentToM1Pos(segmentToM1Pos: SegmentToM1Pos): Future[Boolean] = async {
    // Make sure the row exists
    val rowStatus = await(segmentToM1PosRowExists(segmentToM1Pos.timestamp)) || await(addSegmentToM1PosRow(segmentToM1Pos.timestamp))

    if (rowStatus) {
      await(dsl
        .query(s"UPDATE segment_to_m1_pos SET positions[${segmentToM1Pos.pos}] = '${segmentToM1Pos.id}'")
        .executeAsyncScala()) == 1
    } else rowStatus
  }

  def getSegmentToM1Pos(timestamp: Timestamp, id: String): Future[Option[SegmentToM1Pos]] = async {
    val maybePositions = await(dsl.resultQuery(s"SELECT positions FROM segment_to_m1_pos WHERE timestamp = '$timestamp'")
      .fetchAsyncScala[Array[String]]).headOption
    maybePositions.flatMap(_.zipWithIndex.find(id == _._1).map(p => SegmentToM1Pos(timestamp, id, p._2)))
  }

  def getSegmentToM1Pos(timestamp: Timestamp, pos: Int): Future[Option[SegmentToM1Pos]] = async {
    val maybeId = await(dsl.resultQuery(s"SELECT positions[$pos] FROM segment_to_m1_pos WHERE timestamp = '$timestamp'")
      .fetchAsyncScala[String]).headOption
    maybeId.map(SegmentToM1Pos(timestamp, _, pos))
  }
}

