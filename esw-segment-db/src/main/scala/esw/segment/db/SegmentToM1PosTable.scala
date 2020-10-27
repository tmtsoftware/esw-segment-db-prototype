package esw.segment.db

import java.sql.Date

import org.jooq.DSLContext
import csw.database.scaladsl.JooqExtentions._
import SegmentToM1PosTable._

import scala.async.Async._
import scala.concurrent.{ExecutionContext, Future}

object SegmentToM1PosTable {
  /**
   * Number of segments in the TMT primary mirror.
   * This is also the size of the positions array in the segment_to_m1_pos table.
   */
  private val numSegments = 492

  // Table and column names
  private val tableName = "segment_to_m1_pos"
  private val dateCol = "date"
  private val positionsCol = "positions"

  // Segment id for missing segments
  private val missingSegmentId = "null"

  /**
   * Position of a segment on a given date
   *
   * @param date    date of record
   * @param maybeId the segment id, if the segment is present, None if it is missing
   * @param pos     position of segment
   */
  case class SegmentToM1Pos(date: Date, maybeId: Option[String], pos: Int) {
    def this(date: Date, id: String, pos: Int) = {
      this(date, if (id.startsWith(missingSegmentId)) None else Some(id), pos)
    }
  }

  /**
   * A range of dates
   *
   * @param from start of the date range
   * @param to   end of the date range
   */
  case class DateRange(from: Date, to: Date)

  def currentDate(): Date = new Date(System.currentTimeMillis())
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
   * Returns the input parameter with the date modified to reflect the install date of the segment at
   * the given position. The select statement is rather complicated because it has to examine the row
   * before each row to see if the segment id changed, in order to get the date that the segment was
   * installed.
   *
   * @param date           Use the last install date before this one
   * @param segmentToM1Pos the segment position to use
   */
  private def withInstallDate(date: Date, segmentToM1Pos: SegmentToM1Pos): Future[SegmentToM1Pos] = async {
    if (segmentToM1Pos.maybeId.isEmpty)
      segmentToM1Pos
    else {
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
           | WHERE date <= '$date'
           | ORDER BY date DESC
           |) as w1
           |WHERE
           |  w1.id = '${segmentToM1Pos.maybeId.get}' AND w1.id IS DISTINCT FROM w1.next_id
           |ORDER BY date DESC
           |LIMIT 1;
           |"""
          .stripMargin)
        .fetchAsyncScala[Date])
      SegmentToM1Pos(queryResult.head, segmentToM1Pos.maybeId, segmentToM1Pos.pos)
    }
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
             |SET $positionsCol[${segmentToM1Pos.pos}] = '${segmentToM1Pos.maybeId.getOrElse(missingSegmentId)}'
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
   * @return a list of objects indicating the positions of the given segment id in the given date range (sorted by position)
   */
  def segmentPositions(dateRange: DateRange, segmentId: String): Future[List[SegmentToM1Pos]] = async {
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
      positions.zipWithIndex.find(segmentId == _._1).map(p => new SegmentToM1Pos(date, segmentId, p._2 + 1))
    }
    val fList = list.map(s => withInstallDate(dateRange.to, s))
    await(Future.sequence(fList)).distinct.sortWith(_.pos < _.pos)
  }

  /**
   * Gets a list of segment ids that were in the given position in the given date range.
   *
   * @param dateRange the range of dates to search
   * @param pos       the segment position to search for
   * @return a list of segments at the given position in the given date range (sorted by id)
   */
  def segmentIds(dateRange: DateRange, pos: Int): Future[List[SegmentToM1Pos]] = async {
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
      new SegmentToM1Pos(date, segmentId, pos)
    }
    val fList = list.map(s => withInstallDate(dateRange.to, s))
    await(Future.sequence(fList)).distinct.sortWith(_.maybeId.get < _.maybeId.get)
  }

  /**
   * Returns a list of segments that were installed since the given date
   *
   * @param since the cutoff date for newly installed segments
   */
  def newlyInstalledSegments(since: Date): Future[List[SegmentToM1Pos]] = async {
    val dateRange = DateRange(since, currentDate())
    val fList = (1 to numSegments).toList.map(pos => segmentIds(dateRange, pos))
    // await(Future.sequence(fList)).flatten.filter(_.date.after(since))
    await(Future.sequence(fList)).flatten.filter(_.date.getTime >= since.getTime)
  }

  /**
   * Returns the current segment positions, sorted by position
   * (Missing segments are not included in the returned list).
   */
  def currentPositions(): Future[List[SegmentToM1Pos]] = positionsOnDate(currentDate())

  /**
   * Gets the current segment position for the given segment id.
   *
   * @param segmentId the segment id to search for
   * @return Some object indicating the positions of the given segment, or None if the segment is not installed
   */
  def currentSegmentPosition(segmentId: String): Future[Option[SegmentToM1Pos]] = async {
    await(currentPositions()).find(_.maybeId.contains(segmentId))
  }

  /**
   * Gets the id of the segment currently in the given position.
   *
   * @param pos the segment position to search for
   * @return Some object indicating the id of the segment, or None if no segment is installed at that position
   */
  def currentSegmentAtPosition(pos: Int): Future[Option[SegmentToM1Pos]] = async {
    await(currentPositions()).find(_.pos == pos)
  }

  /**
   * Returns the segment positions as they were on the given date, sorted by position
   * (Missing segments are not included in the returned list).
   */
  def positionsOnDate(date: Date): Future[List[SegmentToM1Pos]] = async {
    val queryResult = await(dsl.resultQuery(
      s"""
         |SELECT $dateCol, $positionsCol
         |FROM $tableName
         |WHERE date <= '$date'
         |ORDER BY date DESC
         |LIMIT 1
         |""".stripMargin)
      .fetchAsyncScala[(Date, Array[String])])
    val list = queryResult.flatMap { result =>
      val date = result._1
      val positions = result._2
      positions
        .zipWithIndex
        .filter(p => !p._1.startsWith(missingSegmentId))
        .map(p => new SegmentToM1Pos(date, p._1, p._2 + 1))
    }
    val fList = list
      .map(s => withInstallDate(currentDate(), s))
    await(Future.sequence(fList)).distinct
  }

  /**
   * Gets the segment position for the given segment id on the given date.
   *
   * @param segmentId the segment id to search for
   * @param date      the date that the segment was in the position
   * @return Some object indicating the positions of the given segment, or None if the segment is not installed
   */
  def segmentPositionOnDate(segmentId: String, date: Date): Future[Option[SegmentToM1Pos]] = async {
    await(positionsOnDate(date)).find(_.maybeId.contains(segmentId))
  }

  /**
   * Gets the id of the segment that was installed in the given position on the given date.
   *
   * @param pos  the segment position to search for
   * @param date the date that the segment was in the position
   * @return Some object indicating the id of the segment, or None if no segment was installed at that position on the given date
   */
  def segmentAtPositionOnDate(pos: Int, date: Date): Future[Option[SegmentToM1Pos]] = async {
    await(positionsOnDate(date)).find(_.pos == pos)
  }

}

