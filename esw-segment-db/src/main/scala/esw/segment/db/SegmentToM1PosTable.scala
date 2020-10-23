package esw.segment.db

import java.sql.Date

import org.jooq.DSLContext
import csw.database.scaladsl.JooqExtentions._
import SegmentToM1PosTable._

import scala.async.Async._
import scala.concurrent.{ExecutionContext, Future}

object SegmentToM1PosTable {
  val numSegments = 492

  val tableName = "segment_to_m1_pos"
  val dateCol = "date"
  val positionsCol = "positions"

  /**
   * Position of a segment on a given date
   *
   * @param date date of record
   * @param id   segment id
   * @param pos  position of segment
   */
  case class SegmentToM1Pos(date: Date, id: String, pos: Int)

  /**
   * A range of dates
   *
   * @param from start of the date range
   * @param to   end of the date range
   */
  case class DateRange(from: Date, to: Date)

}

/**
 * Provides operations on the segment_to_m1_pos database table.
 */
class SegmentToM1PosTable(dsl: DSLContext)(implicit ec: ExecutionContext) {

  /**
   * Returns true if there is an entry for the given date in the table
   *
   * @param date the date to search for
   */
  private def rowExists(date: Date): Future[Boolean] = async {
    await(dsl.resultQuery(s"SELECT COUNT(*) FROM $tableName WHERE $dateCol = '$date'")
      .fetchAsyncScala[Int]).head != 0
  }

  /**
   * Adds a row with only the date and returns true if successful
   *
   * @param date the date to search for
   */
  private def addRow(date: Date): Future[Boolean] = async {
    val positions = (1 to numSegments).map(_ => "\"null\"").toList.mkString(",")
    await(
      dsl
        .query(s"INSERT INTO $tableName($dateCol, $positionsCol) VALUES ('${date.toString}', '{$positions}')")
        .executeAsyncScala()
    ) == 1
  }

  /**
   * Return the input parameter with the date modified to reflect the install date of the segment at
   * the given position.
   */
  private def withInstallDate(dateRange: DateRange, segmentToM1Pos: SegmentToM1Pos): Future[SegmentToM1Pos] = async {
    val queryResult = await(dsl.resultQuery(
      s"""
         |SELECT w1.date
         |FROM (
         | SELECT
         |  date,
         |  positions[${segmentToM1Pos.pos}] as id,
         |  LAG(positions[${segmentToM1Pos.pos}]) OVER (ORDER BY date) as next_id
         | FROM
         |  segment_to_m1_pos
         | WHERE date <= '${dateRange.to}'
         | ORDER BY date DESC
         |) as w1
         |WHERE
         |  w1.id = '${segmentToM1Pos.id}' AND w1.id IS DISTINCT FROM w1.next_id
         |ORDER BY date DESC
         |LIMIT 1;
         |"""
        .stripMargin)
      .fetchAsyncScala[Date])
    SegmentToM1Pos(queryResult.head, segmentToM1Pos.id, segmentToM1Pos.pos)
  }

  /**
   * Sets or updates the date and position of the given segment in the table and returns true if successful
   *
   * @param segmentToM1Pos holds the date, id and pos
   */
  def setPosition(segmentToM1Pos: SegmentToM1Pos): Future[Boolean] = async {
    // Make sure the row exists
    val rowStatus = await(rowExists(segmentToM1Pos.date)) || await(addRow(segmentToM1Pos.date))

    if (rowStatus) {
      await(dsl
        .query(
          s"""
             |UPDATE $tableName
             |SET $positionsCol[${segmentToM1Pos.pos}] = '${segmentToM1Pos.id}'
             |WHERE $dateCol = '${segmentToM1Pos.date}'
             |""".stripMargin)
        .executeAsyncScala()) == 1
    } else rowStatus
  }

  /**
   * Gets a list of segments positions for the given segment id in the given date range.
   *
   * @param dateRange the range of dates to search
   * @param segmentId the segment id to search for
   * @return a list of objects indicating the positions of the given segment id in the given date range
   */
  def getSegmentPositions(dateRange: DateRange, segmentId: String): Future[List[SegmentToM1Pos]] = async {
    val queryResult = await(dsl.resultQuery(
      s"""
         |SELECT $dateCol, $positionsCol
         |FROM $tableName
         |WHERE $dateCol >= '${dateRange.from}' AND $dateCol <= '${dateRange.to}'
         |"""
        .stripMargin
    ).fetchAsyncScala[(Date, Array[String])])
    val list = queryResult.flatMap { result =>
      val date = result._1
      val positions = result._2
      positions.zipWithIndex.find(segmentId == _._1).map(p => SegmentToM1Pos(date, segmentId, p._2 + 1))
    }
    val fList = list.map(s => withInstallDate(dateRange, s))
    await(Future.sequence(fList)).distinct
  }

  /**
   * Gets a list of segment ids that were in the given position in the given date range.
   *
   * @param dateRange the range of dates to search
   * @param pos       the segment position to search for
   * @return a list of segments at the given position in the given date range
   */
  def getSegmentIds(dateRange: DateRange, pos: Int): Future[List[SegmentToM1Pos]] = async {
    val queryResult = await(dsl.resultQuery(
      s"""
         |SELECT $dateCol, $positionsCol[$pos]
         |FROM $tableName
         |WHERE $dateCol >= '${dateRange.from}' AND $dateCol <= '${dateRange.to}' AND $positionsCol[$pos] != 'null'
         |""".stripMargin)
      .fetchAsyncScala[(Date, String)])
    val list = queryResult.map { result =>
      val date = result._1
      val segmentId = result._2
      SegmentToM1Pos(date, segmentId, pos)
    }
    val fList = list.map(s => withInstallDate(dateRange, s))
    await(Future.sequence(fList)).distinct
  }

  //  def getNewlyInstalledSegments(since: Date): Future[List[SegmentToM1Pos]] = async {
  //    val queryResult = await(dsl.resultQuery(
  //      s"""
  //         |SELECT $dateCol, $positionsCol[$pos]
  //         |FROM $tableName
  //         |WHERE $dateCol >= '${dateRange.from}' AND $dateCol <= '${dateRange.to}'
  //         |""".stripMargin)
  //      .fetchAsyncScala[(Date, String)])
  //    queryResult.map { result =>
  //      val date = result._1
  //      val segmentId = result._2
  //      SegmentToM1Pos(date, segmentId, pos)
  //    }
  //  }

  //  def getCurrentPositions(): Future[List[SegmentToM1Pos]] = async {
  //  }
  //
  //  def getCurrentPositions(): Future[List[SegmentToM1Pos]] = async {
  //  }

  // XXX add method to get entire row for current date, or a given date
  // XXX add method to get all rows added since a given date
}

