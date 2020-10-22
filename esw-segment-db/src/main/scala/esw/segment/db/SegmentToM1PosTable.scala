package esw.segment.db

import java.sql.Timestamp

import org.jooq.DSLContext
import csw.database.scaladsl.JooqExtentions._
import SegmentToM1PosTable._

import scala.async.Async._
import scala.concurrent.{ExecutionContext, Future}

object SegmentToM1PosTable {
  val numSegments = 492

  /**
   * A range of time
   *
   * @param from start of the time range
   * @param to   end of the time range
   */
  case class TimeRange(from: Timestamp, to: Timestamp)

}

/**
 * Provides operations on the segment_to_m1_pos database table.
 */
class SegmentToM1PosTable(dsl: DSLContext)(implicit ec: ExecutionContext) {

  /**
   * Returns true if there is an entry for the given timestamp in the table
   *
   * @param timestamp the timestamp to search for
   */
  private def rowExists(timestamp: Timestamp): Future[Boolean] = async {
    await(dsl.resultQuery(s"SELECT COUNT(*) FROM segment_to_m1_pos WHERE timestamp = '$timestamp'")
      .fetchAsyncScala[Int]).head != 0
  }

  /**
   * Adds a row with only the timestamp and returns true if successful
   *
   * @param timestamp the timestamp to search for
   */
  private def addRow(timestamp: Timestamp): Future[Boolean] = async {
    await(
      dsl
        .query(s"INSERT INTO segment_to_m1_pos(timestamp) VALUES ('${timestamp.toString}')")
        .executeAsyncScala()
    ) == 1
  }

  /**
   * Sets or updates the timestamp and position of the given segment in the segment_to_m1_pos table and returns true if successful
   *
   * @param segmentToM1Pos holds the timestamp, id and pos
   */
  def setPosition(segmentToM1Pos: SegmentToM1Pos): Future[Boolean] = async {
    // Make sure the row exists
    val rowStatus = await(rowExists(segmentToM1Pos.timestamp)) || await(addRow(segmentToM1Pos.timestamp))

    if (rowStatus) {
      await(dsl
        .query(s"UPDATE segment_to_m1_pos SET positions[${segmentToM1Pos.pos}] = '${segmentToM1Pos.id}'")
        .executeAsyncScala()) == 1
    } else rowStatus
  }

  /**
   * Gets a list of segments positions for the given segment id in the given time range.
   *
   * @param timeRange the range of time to search
   * @param segmentId the segment id to search for
   * @return a list of objects indicating the positions of the given segment id in the given time range
   */
  def getSegmentPositions(timeRange: TimeRange, segmentId: String): Future[List[SegmentToM1Pos]] = async {
    val queryResult = await(dsl.resultQuery(
      s"""
         |SELECT timestamp, positions
         |FROM segment_to_m1_pos
         |WHERE timestamp >= '${timeRange.from}' && timestamp <= ${timeRange.to}
         |"""
        .stripMargin)
      .fetchAsyncScala[(Timestamp, Array[String])])
    queryResult.flatMap { result =>
      val timestamp = result._1
      val positions = result._2
      positions.zipWithIndex.find(segmentId == _._1).map(p => SegmentToM1Pos(timestamp, segmentId, p._2))
    }
  }

  /**
   * Gets a list of segments that were in the given position in the given time range.
   *
   * @param timeRange the range of time to search
   * @param pos       the segment position to search for
   * @return a list of segments at the given position in the given time range
   */
  def getSegmentPositions(timeRange: TimeRange, pos: Int): Future[List[SegmentToM1Pos]] = async {
    val queryResult = await(dsl.resultQuery(
      s"""
         |SELECT timestamp, positions[$pos]
         |FROM segment_to_m1_pos
         |WHERE timestamp >= '${timeRange.from}' && timestamp <= ${timeRange.to}
         |""".stripMargin)
      .fetchAsyncScala[(Timestamp, String)])
    queryResult.map { result =>
      val timestamp = result._1
      val segmentId = result._2
      SegmentToM1Pos(timestamp, segmentId, pos)
    }
  }

  // XXX add method to get entire row for current time, or a given time
  // XXX add method to get all rows added since a given time
}

