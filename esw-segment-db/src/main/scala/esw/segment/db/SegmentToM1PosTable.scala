package esw.segment.db

import java.sql.Date

import org.jooq.DSLContext
import csw.database.scaladsl.JooqExtentions._
import esw.segment.shared.EswSegmentData._
import SegmentToM1PosTable._
import esw.segment.shared.SegmentToM1Api

import scala.async.Async._
import scala.concurrent.{ExecutionContext, Future}

object SegmentToM1PosTable {

  // Table and column names
  private[db] val tableName = "segment_to_m1_pos"
  private val dateCol       = "date"
  private val positionsCol  = "positions"

  // Segment id for missing segments
  private val missingSegmentId = "------"

  // Segment id for segments where the state is unknown (when adding a new empty row)
  private val unknownSegmentId = "??????"

  // Return None for missing or unknown segments
  private def idOption(id: String): Option[String] = {
    if (id.startsWith(missingSegmentId) || id.startsWith(unknownSegmentId)) None else Some(id)
  }

  private def quoted(s: String) = "\"" + s + "\""

  private def currentDate(): Date = new Date(System.currentTimeMillis())

  private def makeSegmentToM1Pos(date: Date, id: String, dbPos: Int): SegmentToM1Pos = {
    val position = toPosition(dbPos)
    SegmentToM1Pos(date, idOption(id), position)
  }
}

/**
 * Provides operations on the segment_to_m1_pos database table.
 */
class SegmentToM1PosTable(dsl: DSLContext)(implicit ec: ExecutionContext) extends SegmentToM1Api {

  /**
   * Returns true if there is an entry for the given date in the table
   *
   * @param date the date to search for
   */
  private def rowExists(date: Date): Future[Boolean] =
    async {
      await(
        dsl
          .resultQuery(s"SELECT COUNT(*) FROM $tableName WHERE $dateCol = '$date'")
          .fetchAsyncScala[Int]
      ).head != 0
    }

  /**
   * Returns an array of all 492 segment ids as of the given date as stored in the database,
   * or a list of unknown segment id entries, if there are no database rows yet.
   */
  private def rawPositions(date: Date): Future[Array[String]] =
    async {
      import scala.jdk.CollectionConverters._
      import scala.compat.java8.FutureConverters.CompletionStageOps

      await(
        dsl
          .resultQuery(s"""
         |SELECT $positionsCol
         |FROM $tableName
         |WHERE date <= '$date'
         |ORDER BY date DESC
         |LIMIT 1
         |""".stripMargin)
          .fetchAsync()
          .toScala
          .map(_.asScala.map(_.into(classOf[Object]).asInstanceOf[Array[String]]).toArray)
      ).headOption
        .getOrElse((1 to numSegments).map(_ => unknownSegmentId).toArray)
    }

  /**
   * Adds a row with the given date and copies the segment positions from the latest entry,
   * if found, or else a row of nulls.
   *
   * @param date the date to search for
   * @return true if successful
   */
  private def addRow(date: Date): Future[Boolean] =
    async {
      val allSegmentIdsStr = await(rawPositions(date)).map(quoted).mkString(",")
      await(
        dsl
          .query(s"INSERT INTO $tableName($dateCol, $positionsCol) VALUES ('${date.toString}', '{$allSegmentIdsStr}')")
          .executeAsyncScala()
      ) == 1
    }

  /**
   * Adds a row with only the given date and no positions and returns true if successful
   *
   * @param date the date to search for
   */
  private def addEmptyRow(date: Date): Future[Boolean] =
    async {
      await(
        dsl
          .query(s"INSERT INTO $tableName($dateCol) VALUES ('${date.toString}')")
          .executeAsyncScala()
      ) == 1
    }

  /**
   * Returns the input parameter with the date modified to reflect the install date of the segment at
   * the given position. The select statement is rather complicated because it has to examine the row
   * before each row to see if the segment id changed, in order to get the date that the segment was
   * installed.
   *
   * @param segmentToM1Pos the segment position to use
   */
  private def withInstallDate(segmentToM1Pos: SegmentToM1Pos): Future[SegmentToM1Pos] =
    async {
      val dbPos = segmentToM1Pos.dbPos
      val queryResult = await(
        dsl
          .resultQuery(s"""
           |SELECT w1.date
           |FROM (
           | SELECT
           |  date,
           |  positions[$dbPos] as id,
           |  LAG(positions[$dbPos]) OVER (ORDER BY date) as next_id
           | FROM
           |  segment_to_m1_pos
           | WHERE date <= '${segmentToM1Pos.date}'
           | ORDER BY date DESC
           |) as w1
           |WHERE
           |  (w1.id = '${segmentToM1Pos.maybeId.getOrElse(missingSegmentId)}'
           |  OR w1.id = '${segmentToM1Pos.maybeId.getOrElse(unknownSegmentId)}')
           |  AND w1.id IS DISTINCT FROM w1.next_id
           |ORDER BY date DESC
           |LIMIT 1
           |""".stripMargin)
          .fetchAsyncScala[Date]
      )
      SegmentToM1Pos(queryResult.head, segmentToM1Pos.maybeId, segmentToM1Pos.position)
    }

  /**
   * Set the given segment ids as removed on the given date.
   * (If you know the segment position, you can also call setPosition with segmentToM1Pos.maybeId set to None).
   *
   * @param date       date for a row
   * @param segmentIds list of segment ids to remove
   * @return true if all ok
   */
  private def removeSegmentIds(date: Date, segmentIds: List[String]): Future[Boolean] =
    async {
      if (segmentIds.nonEmpty) {
        val dateRange        = DateRange(date, date)
        val currentPositions = await(Future.sequence(segmentIds.map(id => segmentPositions(dateRange, id))).map(_.flatten))
        val results          = await(Future.sequence(currentPositions.map(p => setPosition(SegmentToM1Pos(date, None, p.position)))))
        results.forall(p => p)
      }
      else true
    }

  /**
   * Update any "unknown" segment ids in rows after the given pos (up to the first known value)
   * @param segmentToM1Pos the position being inserted
   * @return true if OK
   */
  private def updatePositionsAfter(segmentToM1Pos: SegmentToM1Pos): Future[Boolean] =
    async {
      // If inserting before the most recent row, need to update any following "unknown" segment-ids for this pos
      val date = await(mostRecentChange(currentDate()))
      if (segmentToM1Pos.date.before(date)) {
        val dateRange = DateRange(segmentToM1Pos.date, date)
        val list = await(rawSegmentIds(dateRange, segmentToM1Pos.position, includeEmpty = true))
          .drop(1)
          .takeWhile(_.maybeId.isEmpty)
        if (list.nonEmpty) {
          val dateRange2 = DateRange(list.head.date, list.reverse.head.date)
          await(
            dsl
              .query(s"""
                   |UPDATE $tableName
                   |SET $positionsCol[${segmentToM1Pos.dbPos}] = '${segmentToM1Pos.maybeId.getOrElse(missingSegmentId)}'
                   |WHERE $positionsCol[${segmentToM1Pos.dbPos}] = '$unknownSegmentId'
                   |AND $dateCol >= '${dateRange2.from}' AND $dateCol <= '${dateRange2.to}'
                   |""".stripMargin)
              .executeAsyncScala()
          )
        }
      }
      true
    }

  private def sortByDate(list: List[SegmentToM1Pos], desc: Boolean = false) = {
    if (desc)
      list.sortWith((p, q) => q.date.before(p.date))
    else
      list.sortWith((p, q) => p.date.before(q.date))
  }

  override def setPosition(segmentToM1Pos: SegmentToM1Pos): Future[Boolean] =
    async {
      // Make sure the row exists
      val rowStatus = await(rowExists(segmentToM1Pos.date)) || await(addRow(segmentToM1Pos.date))

      if (rowStatus) {
        await(removeSegmentIds(segmentToM1Pos.date, segmentToM1Pos.maybeId.toList))
        val insertStatus = await(
          dsl
            .query(s"""
             |UPDATE $tableName
             |SET $positionsCol[${segmentToM1Pos.dbPos}] = '${segmentToM1Pos.maybeId.getOrElse(missingSegmentId)}'
             |WHERE $dateCol = '${segmentToM1Pos.date}'
             |""".stripMargin)
            .executeAsyncScala()
        ) == 1
        val updateStatus = await(updatePositionsAfter(segmentToM1Pos))
        insertStatus && updateStatus
      }
      else rowStatus
    }

  override def setPositions(date: Date, positions: List[(Option[String], String)]): Future[Boolean] =
    async {
      val dbPositions = positions.map(p => (p._1, toDbPosition(p._2)))
      val rowStatus   = await(rowExists(date)) || await(addRow(date))
      if (rowStatus) {
        val ids = dbPositions.flatMap(_._1)
        removeSegmentIds(date, ids)
        val setExpr = dbPositions
          .map { p =>
            val dbPos   = p._2
            val maybeId = p._1
            s"$positionsCol[$dbPos] = '${maybeId.getOrElse(missingSegmentId)}'"
          }
          .mkString(", ")
        await(
          dsl
            .query(s"""
             |UPDATE $tableName
             |SET $setExpr
             |WHERE $dateCol = '$date'
             |""".stripMargin)
            .executeAsyncScala()
        ) == 1
      }
      else rowStatus
    }

  override def setAllPositions(date: Date, allSegmentIds: List[Option[String]]): Future[Boolean] =
    async {
      // Make sure the row exists
      val rowStatus = allSegmentIds.size == numSegments && (await(rowExists(date)) || await(addEmptyRow(date)))
      if (rowStatus) {
        val allSegmentIdsStr = allSegmentIds.map(p => s"${quoted(p.getOrElse(missingSegmentId))}").mkString(",")
        await(
          dsl
            .query(s"""
               |UPDATE $tableName
               |SET $positionsCol = '{$allSegmentIdsStr}'
               |WHERE $dateCol = '$date'
               |""".stripMargin)
            .executeAsyncScala()
        ) == 1
      }
      else rowStatus
    }

  override def segmentPositions(dateRange: DateRange, segmentId: String): Future[List[SegmentToM1Pos]] =
    async {
      val queryResult = await(
        dsl
          .resultQuery(
            s"""
         |SELECT $dateCol, $positionsCol
         |FROM $tableName
         |WHERE $dateCol >= '${dateRange.from}' AND $dateCol <= '${dateRange.to}'
         |""".stripMargin
          )
          .fetchAsyncScala[(Date, Array[String])]
      )
      val list = queryResult.flatMap { result =>
        val date        = result._1
        val dbPositions = result._2
        dbPositions.zipWithIndex.find(segmentId == _._1).map(p => makeSegmentToM1Pos(date, segmentId, p._2 + 1))
      }
      val fList = list.map(s => withInstallDate(s))
      sortByDate(await(Future.sequence(fList)).distinct)
    }

  /**
   * Gets a list of segment ids that were in the given position (A1 to F82) in the given date range.
   * (This internal version returns objects with the row's date (not the installation date for a segment)
   *
   * @param dateRange the range of dates to search
   * @param position the segment position to search for (A1 to F82)
   * @param includeEmpty also return empty positions (default: return only positions with a segment-id)
   * @return a list of segments at the given position in the given date range (sorted by date)
   */
  private def rawSegmentIds(dateRange: DateRange, position: String, includeEmpty: Boolean = false): Future[List[SegmentToM1Pos]] =
    async {
      val dbPos = toDbPosition(position)
      val cond  = if (includeEmpty) "" else s"AND $positionsCol[$dbPos] NOT IN ('$missingSegmentId', '$unknownSegmentId')"
      val queryResult = await(
        dsl
          .resultQuery(s"""
                          |SELECT $dateCol, $positionsCol[$dbPos]
                          |FROM $tableName
                          |WHERE $dateCol >= '${dateRange.from}'
                          |AND $dateCol <= '${dateRange.to}'
                          |$cond
                          |""".stripMargin)
          .fetchAsyncScala[(Date, String)]
      )
      val list = queryResult.map { result =>
        val date      = result._1
        val segmentId = result._2
        makeSegmentToM1Pos(date, segmentId, dbPos)
      }
      sortByDate(list.distinct)
    }

  override def segmentIds(dateRange: DateRange, position: String): Future[List[SegmentToM1Pos]] =
    async {
      val list  = await(rawSegmentIds(dateRange, position))
      val fList = list.map(s => withInstallDate(s))
      sortByDate(await(Future.sequence(fList)).distinct)
    }

  override def allSegmentIds(position: String): Future[List[SegmentToM1Pos]] =
    async {
      val dateRange = DateRange(new Date(0), currentDate())
      val list      = await(rawSegmentIds(dateRange, position, includeEmpty = true))
      val fList     = list.map(s => withInstallDate(s))
      sortByDate(await(Future.sequence(fList)).distinct, desc = true)
    }

  override def newlyInstalledSegments(since: Date): Future[List[SegmentToM1Pos]] =
    async {
      val dateRange = DateRange(since, currentDate())
      val fList     = (1 to numSegments).toList.map(pos => segmentIds(dateRange, toPosition(pos)))
      // await(Future.sequence(fList)).flatten.filter(_.date.after(since))
      await(Future.sequence(fList)).flatten.filter(_.date.getTime >= since.getTime)
    }

  override def currentPositions(): Future[List[SegmentToM1Pos]] = positionsOnDate(currentDate())

  override def currentSegmentPosition(segmentId: String): Future[Option[SegmentToM1Pos]] =
    async {
      await(currentPositions()).find(_.maybeId.contains(segmentId))
    }

  override def currentSegmentAtPosition(position: String): Future[Option[SegmentToM1Pos]] =
    async {
      val dbPos = toDbPosition(position)
      await(currentPositions()).find(_.dbPos == dbPos)
    }

  override def positionsOnDate(date: Date): Future[List[SegmentToM1Pos]] =
    async {
      val queryResult = await(
        dsl
          .resultQuery(s"""
         |SELECT $dateCol, $positionsCol
         |FROM $tableName
         |WHERE date <= '$date'
         |ORDER BY date DESC
         |LIMIT 1
         |""".stripMargin)
          .fetchAsyncScala[(Date, Array[String])]
      )
      if (queryResult.isEmpty) {
        // If the results are empty, return a row with empty ids
        (1 to numSegments).toList.map(pos => SegmentToM1Pos(date, None, toPosition(pos)))
      }
      else {
        val list = queryResult.flatMap { result =>
          val date        = result._1
          val dbPositions = result._2
          dbPositions.zipWithIndex
            .map(p => makeSegmentToM1Pos(date, p._1, p._2 + 1))
        }
        val fList = list
          .map(s => withInstallDate(s))
        sortByDate(await(Future.sequence(fList)).distinct)
      }
    }

  override def mostRecentChange(date: Date): Future[Date] =
    async {
      await(
        dsl
          .resultQuery(s"""
         |SELECT $dateCol
         |FROM $tableName
         |WHERE date <= '$date'
         |ORDER BY date DESC
         |LIMIT 1
         |""".stripMargin)
          .fetchAsyncScala[Date]
      ).headOption.getOrElse(currentDate())
    }

  override def segmentPositionOnDate(date: Date, segmentId: String): Future[Option[SegmentToM1Pos]] =
    async {
      await(positionsOnDate(date)).find(_.maybeId.contains(segmentId))
    }

  override def segmentAtPositionOnDate(date: Date, position: String): Future[Option[SegmentToM1Pos]] =
    async {
      val dbPos = toDbPosition(position)
      await(positionsOnDate(date)).find(_.dbPos == dbPos)
    }

  override def availableSegmentIdsForPos(position: String): Future[List[String]] =
    async {
      // TODO: Filter out segment-ids already in use
      compatibleSegmentIdsForPos(position.tail.toInt)
    }

  override def resetTables(): Future[Boolean] =
    async {
      await(dsl.truncate(SegmentToM1PosTable.tableName).executeAsyncScala()) >= 0
    }
}
