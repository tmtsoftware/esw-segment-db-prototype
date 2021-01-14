package esw.segment.db

import org.jooq.DSLContext
import csw.database.scaladsl.JooqExtentions._
import esw.segment.shared.EswSegmentData._
import SegmentToM1PosTable._
import esw.segment.shared.SegmentToM1Api

import java.sql.Date
import java.time.LocalDate
import scala.async.Async._
import scala.concurrent.{ExecutionContext, Future}

object SegmentToM1PosTable {

  // Table and column names
  private[db] val tableName  = "segment_to_m1_pos"
  private val dateCol        = "date"
  private val positionsCol   = "positions"
  private val installDateCol = "install_date"

  // Segment id for missing/empty segments
  val missingSegmentId = "------"

  // Segment id for segments where the state is unknown (when adding a new empty row)
  val unknownSegmentId = "??????"

  // Return None for missing or unknown segments
  def idOption(id: String): Option[String] = {
    if (id.startsWith(missingSegmentId) || id.startsWith(unknownSegmentId)) None else Some(id)
  }

  def quoted(s: String): String = "\"" + s + "\""

  private def makeSegmentToM1Pos(date: LocalDate, id: String, dbPos: Int): SegmentToM1Pos = {
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
  private def rowExists(date: LocalDate): Future[Boolean] =
    async {
      await(
        dsl
          .resultQuery(s"SELECT COUNT(*) FROM $tableName WHERE $dateCol = '$date'")
          .fetchAsyncScala[Int]
      ).head != 0
    }

  /**
   * Returns an array of install dates for all segment ids as of the given date as stored in the database,
   * or a list of with the given date, if there are no database rows yet.
   */
  private def getInstallDates(date: LocalDate): Future[Array[LocalDate]] =
    async {
      import scala.jdk.CollectionConverters._
      import scala.compat.java8.FutureConverters.CompletionStageOps

      await(
        dsl
          .resultQuery(s"""
                          |SELECT $installDateCol
                          |FROM $tableName
                          |WHERE $dateCol <= '$date'
                          |ORDER BY $dateCol DESC
                          |LIMIT 1
                          |""".stripMargin)
          .fetchAsync()
          .toScala
          .map(_.asScala.map(_.into(classOf[Object]).asInstanceOf[Array[Date]]).toArray)
      ).headOption
        .map(a => a.map(_.toLocalDate))
        .getOrElse((1 to totalSegments).map(_ => date).toArray)
    }

  /**
   * Returns an array of all segment ids as of the given date as stored in the database,
   * or a list of unknownSegmentId, if there are no database rows yet.
   */
  private def getPositions(date: LocalDate): Future[Array[String]] =
    async {
      import scala.jdk.CollectionConverters._
      import scala.compat.java8.FutureConverters.CompletionStageOps

      await(
        dsl
          .resultQuery(s"""
                          |SELECT $positionsCol
                          |FROM $tableName
                          |WHERE $dateCol <= '$date'
                          |ORDER BY $dateCol DESC
                          |LIMIT 1
                          |""".stripMargin)
          .fetchAsync()
          .toScala
          .map(_.asScala.map(_.into(classOf[Object]).asInstanceOf[Array[String]]).toArray)
      ).headOption
        .getOrElse((1 to totalSegments).map(_ => unknownSegmentId).toArray)
    }

  /**
   * Adds a row with the given date and copies the segment positions from the latest entry,
   * if found, or else a row of nulls.
   *
   * @param date the date to search for
   * @return true if successful
   */
  private def addRow(date: LocalDate): Future[Boolean] =
    async {
      val positions          = await(getPositions(date))
      val installDates       = await(getInstallDates(date))
      val allSegmentIdsStr   = positions.map(quoted).mkString(",")
      val allInstallDatesStr = installDates.map(d => quoted(d.toString)).mkString(",")
      val result = await(
        dsl
          .query(s"""
               |INSERT INTO $tableName($dateCol, $positionsCol, $installDateCol)
               |VALUES ('${date.toString}', '{$allSegmentIdsStr}', '{$allInstallDatesStr}')""".stripMargin)
          .executeAsyncScala()
      ) == 1

//      val posList = (1 to totalSegments).toList.map(pos => SegmentToM1Pos(installDates(pos-1), idOption(positions(pos-1)), toPosition(pos)))
//      result && await(updateAllPositionsAfter(posList))

      result
    }

  /**
   * Adds a row with only the given date and no positions and returns true if successful
   *
   * @param date the date to search for
   */
  private def addEmptyRow(date: LocalDate): Future[Boolean] =
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
           |SELECT w1.$dateCol
           |FROM (
           | SELECT
           |  $dateCol,
           |  $positionsCol[$dbPos] as id,
           |  LAG($positionsCol[$dbPos]) OVER (ORDER BY $dateCol) as next_id
           | FROM
           |  segment_to_m1_pos
           | WHERE $dateCol <= '${segmentToM1Pos.date}'
           | ORDER BY $dateCol DESC
           |) as w1
           |WHERE
           |  (w1.id = '${segmentToM1Pos.maybeId.getOrElse(missingSegmentId)}'
           |  OR w1.id = '${segmentToM1Pos.maybeId.getOrElse(unknownSegmentId)}')
           |  AND w1.id IS DISTINCT FROM w1.next_id
           |ORDER BY $dateCol DESC
           |LIMIT 1
           |""".stripMargin)
          .fetchAsyncScala[Date]
      )
      if (queryResult.nonEmpty)
        SegmentToM1Pos(queryResult.head.toLocalDate, segmentToM1Pos.maybeId, segmentToM1Pos.position)
      else {
        println(
          s"XXX Empty at withInstallDate(): date: ${segmentToM1Pos.date}, pos: ${segmentToM1Pos.position}, id: ${segmentToM1Pos.maybeId}"
        )
        segmentToM1Pos
      }
    }

  /**
   * Set the given segment ids as removed on the given date.
   * (If you know the segment position, you can also call setPosition with segmentToM1Pos.maybeId set to None).
   *
   * @param date       date for a row
   * @param segmentIds list of segment ids to remove
   * @return true if all ok
   */
  private def removeSegmentIds(date: LocalDate, segmentIds: List[String]): Future[Boolean] =
    async {
      if (segmentIds.nonEmpty) {
        val dateRange        = DateRange(date, date)
        val currentPositions = await(Future.sequence(segmentIds.map(id => segmentPositions(dateRange, id))).map(_.flatten))
        val results          = await(Future.sequence(currentPositions.map(p => setPosition(SegmentToM1Pos(date, None, p.position)))))
        results.forall(p => p)
      }
      else true
    }

//  /**
//   * Calls updatePositionsAfter() recursively on each position in the list
//   * @param posList the positions being inserted
//   * @return true if ok
//   */
//  private def updateAllPositionsAfter(posList: List[SegmentToM1Pos]): Future[Boolean] = async {
//    if (posList.isEmpty) true else {
//      val result = await(updatePositionsAfter(posList.head, posList.head.date))
//      result && await(updateAllPositionsAfter(posList.tail))
//    }
//  }

  /**
   * Update any "unknown" segment ids in rows after the given pos (up to the first known value)
   * @param segmentToM1Pos the position being inserted
   * @param installDate the initial install date for the segment
   * @return true if OK
   */
  private def updatePositionsAfter(segmentToM1Pos: SegmentToM1Pos, installDate: LocalDate): Future[Boolean] =
    async {
      // If inserting before the most recent row, need to update any following "unknown" segment-ids for this pos
      val date = await(mostRecentChange(currentDate()))
      if (segmentToM1Pos.date.compareTo(date) < 0) {
        val dateRange = DateRange(segmentToM1Pos.date, date)
        val list = await(getSegmentIds(dateRange, segmentToM1Pos.position, includeEmpty = true, withInstallDate = false))
          .drop(1)
          .takeWhile(_.maybeId.isEmpty)
        if (list.nonEmpty) {
          val dateRange2 = DateRange(list.head.date, list.reverse.head.date)
          await(
            dsl
              .query(s"""
                   |UPDATE $tableName
                   |SET $positionsCol[${segmentToM1Pos.dbPos}] = '${segmentToM1Pos.maybeId.getOrElse(missingSegmentId)}',
                   |$installDateCol[${segmentToM1Pos.dbPos}] = '$installDate'
                   |WHERE $positionsCol[${segmentToM1Pos.dbPos}] = '$unknownSegmentId'
                   |AND $dateCol >= '${dateRange2.from}' AND $dateCol <= '${dateRange2.to}'
                   |""".stripMargin)
              .executeAsyncScala()
          )
        }
      }
      true
    }

  private def updatePosition(segmentToM1Pos: SegmentToM1Pos): Future[Boolean] =
    async {
      await(
        dsl
          .query(s"""
                  |UPDATE $tableName
                  |SET $positionsCol[${segmentToM1Pos.dbPos}] = '${segmentToM1Pos.maybeId.getOrElse(missingSegmentId)}'
                  |WHERE $dateCol = '${segmentToM1Pos.date}'
                  |""".stripMargin)
          .executeAsyncScala()
      ) == 1
    }

  /**
   * Update the install date for the segment
   * @param segmentToM1Pos the position being inserted
   * @param installDate the initial install date for the segment
   * @return true if OK
   */
  private def updateInstallDate(segmentToM1Pos: SegmentToM1Pos, installDate: LocalDate): Future[Boolean] =
    async {
      await(
        dsl
          .query(s"""
                  |UPDATE $tableName
                  |SET $installDateCol[${segmentToM1Pos.dbPos}] = '$installDate'
                  |WHERE $dateCol = '${segmentToM1Pos.date}'
                  |""".stripMargin)
          .executeAsyncScala()
      ) == 1
    }

  /**
   * Update the installDates in the current and following rows after the positions were inserted.
   * (Use recursion to avoid async and thread restrictions.)
   */
  private def updateAllAfterInsert(posList: List[SegmentToM1Pos]): Future[Boolean] =
    async {
      if (posList.isEmpty) true
      else {
        val result = await(updateAfterInsert(posList.head))
        result && await(updateAllAfterInsert(posList.tail))
      }
    }

  /**
   * Update the installDate in the current and following rows after a position was inserted
   */
  private def updateAfterInsert(segmentToM1Pos: SegmentToM1Pos): Future[Boolean] =
    async {
      val installDate = await(withInstallDate(segmentToM1Pos)).date
      await(updateInstallDate(segmentToM1Pos, installDate)) &&
      await(updatePositionsAfter(segmentToM1Pos, installDate))
    }

  override def setPosition(segmentToM1Pos: SegmentToM1Pos): Future[Boolean] =
    async {
      // Make sure the row exists
      (await(rowExists(segmentToM1Pos.date)) || await(addRow(segmentToM1Pos.date))) &&
      await(removeSegmentIds(segmentToM1Pos.date, segmentToM1Pos.maybeId.toList)) &&
      await(updatePosition(segmentToM1Pos)) &&
      await(updateAfterInsert(segmentToM1Pos))
    }

  // XXX TODO Overwrite row with only the given positions?
//  override def setPositions(config: MirrorConfig): Future[Boolean] =
//    async {
//      //      val posList = config.segments.map(p => SegmentToM1Pos(date, p.segmentId, p.position))
//      //      await(setPositions(posList))
//      val date      = config.getDate
//      val map       = config.segments.map(p => p.position -> p.segmentId).toMap
//      val positions = (1 to totalSegments).toList.map(EswSegmentData.toPosition)
//      val segIds    = positions.map(p => map.get(p).flatten)
//      await(setAllPositions(date, segIds))
//    }

  override def setPositions(config: MirrorConfig): Future[Boolean] =
    async {
      val posList = config.segments.map(p => SegmentToM1Pos(config.date, p.segmentId, p.position))
      await(setPositions(posList))
    }

  // Sets the positions in the list recursively in order to avoid running out of threads and since
  // async/await can't be used in a loop
  private def setPositions(posList: List[SegmentToM1Pos]): Future[Boolean] =
    async {
      if (posList.isEmpty) true
      else {
        val result = await(setPosition(posList.head))
        result && await(setPositions(posList.tail))
      }
    }

  override def setAllPositions(date: LocalDate, allSegmentIds: List[Option[String]]): Future[Boolean] =
    async {
      // Make sure the row exists
      val rowStatus = allSegmentIds.size == totalSegments && (await(rowExists(date)) || await(addEmptyRow(date)))
      if (rowStatus) {
        val allSegmentIdsStr = allSegmentIds.map(p => s"${quoted(p.getOrElse(missingSegmentId))}").mkString(",")
        val status = await(
          dsl
            .query(s"""
               |UPDATE $tableName
               |SET $positionsCol = '{$allSegmentIdsStr}'
               |WHERE $dateCol = '$date'
               |""".stripMargin)
            .executeAsyncScala()
        ) == 1
        val list = allSegmentIds.zipWithIndex
          .map(p => SegmentToM1Pos(date, p._1, toPosition(p._2 + 1)))
        status && await(updateAllAfterInsert(list))
      }
      else rowStatus
    }

  override def segmentPositions(dateRange: DateRange, segmentId: String): Future[List[SegmentToM1Pos]] =
    async {
      val queryResult = await(
        dsl
          .resultQuery(
            s"""
         |SELECT $positionsCol, $installDateCol
         |FROM $tableName
         |WHERE $dateCol >= '${dateRange.from}' AND $dateCol <= '${dateRange.to}'
         |""".stripMargin
          )
          .fetchAsyncScala[(Array[String], Array[Date])]
      )
      val list = queryResult.flatMap { result =>
        val (dbPositions, installDates) = result
        dbPositions.zipWithIndex
          .find(segmentId == _._1)
          .map(p => makeSegmentToM1Pos(installDates(p._2).toLocalDate, segmentId, p._2 + 1))
      }
      sortByDate(list.distinct)
    }

  /**
   * Gets a list of segment ids that were in the given position (A1 to F82) in the given date range.
   *
   * @param dateRange the range of dates to search
   * @param position the segment position to search for (A1 to F82)
   * @param includeEmpty also return empty positions (default: return only positions with a segment-id)
   * @param withInstallDate if true, set the returned date fields to the installDate, otherwise the row's date
   * @return a list of segments at the given position in the given date range (sorted by date)
   */
  private def getSegmentIds(
      dateRange: DateRange,
      position: String,
      includeEmpty: Boolean,
      withInstallDate: Boolean
  ): Future[List[SegmentToM1Pos]] =
    async {
      val dbPos = toDbPosition(position)
      val cond  = if (includeEmpty) "" else s"AND $positionsCol[$dbPos] NOT IN ('$missingSegmentId', '$unknownSegmentId')"
      val queryResult = await(
        dsl
          .resultQuery(s"""
                          |SELECT $dateCol, $positionsCol[$dbPos], $installDateCol[$dbPos]
                          |FROM $tableName
                          |WHERE $dateCol >= '${dateRange.from}'
                          |AND $dateCol <= '${dateRange.to}'
                          |$cond
                          |ORDER BY $dateCol
                          |""".stripMargin)
          .fetchAsyncScala[(Date, String, Date)]
      )
      val list = queryResult.map { result =>
        val date      = if (withInstallDate) result._3.toLocalDate else result._1.toLocalDate
        val segmentId = result._2
        makeSegmentToM1Pos(date, segmentId, dbPos)
      }
      list.distinct
    }

  override def segmentIds(dateRange: DateRange, position: String): Future[List[SegmentToM1Pos]] =
    async {
      val list = await(getSegmentIds(dateRange, position, includeEmpty = false, withInstallDate = true))
      sortByDate(list.distinct)
    }

  override def allSegmentIds(position: String): Future[List[SegmentToM1Pos]] =
    async {
      val dateRange = DateRange(LocalDate.EPOCH, LocalDate.now())
      val list      = await(getSegmentIds(dateRange, position, includeEmpty = true, withInstallDate = true))
      sortByDate(list.distinct, desc = true)
    }

  override def newlyInstalledSegments(since: LocalDate): Future[List[SegmentToM1Pos]] =
    async {
      val dateRange = DateRange(since, currentDate())
      val fList     = (1 to totalSegments).toList.map(pos => segmentIds(dateRange, toPosition(pos)))
      await(Future.sequence(fList)).flatten.filter(_.date.compareTo(since) >= 0)
    }

  override def segmentExchanges(since: LocalDate): Future[List[MirrorConfig]] =
    async {
      val date     = await(mostRecentChange(since))
      val nextDate = await(nextChange(date))
      val list     = await(positionsOnDate(date)).filter(_.date.compareTo(date) == 0)
      val m        = MirrorConfig(date, list.map(s => SegmentConfig(s.position, s.maybeId)))
      if (nextDate == date) List(m) else m :: await(segmentExchanges(nextDate))
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

  override def positionsOnDate(date: LocalDate): Future[List[SegmentToM1Pos]] =
    async {
      val queryResult = await(
        dsl
          .resultQuery(s"""
         |SELECT $positionsCol, $installDateCol
         |FROM $tableName
         |WHERE $dateCol <= '$date'
         |ORDER BY $dateCol DESC
         |LIMIT 1
         |""".stripMargin)
          .fetchAsyncScala[(Array[String], Array[Date])]
      )
      if (queryResult.isEmpty) {
        // If the results are empty, return a row with empty ids
        (1 to totalSegments).toList.map(pos => SegmentToM1Pos(date, None, toPosition(pos)))
      }
      else {
        val list = queryResult.flatMap { result =>
          val (dbPositions, installDates) = result
          dbPositions.zipWithIndex
            .map(p => makeSegmentToM1Pos(installDates(p._2).toLocalDate, p._1, p._2 + 1))
        }
        sortByDate(list.distinct)
      }
    }

  override def mostRecentChange(date: LocalDate): Future[LocalDate] =
    async {
      await(
        dsl
          .resultQuery(s"""
         |SELECT $dateCol
         |FROM $tableName
         |WHERE $dateCol <= '$date'
         |ORDER BY $dateCol DESC
         |LIMIT 1
         |""".stripMargin)
          .fetchAsyncScala[Date]
      ).headOption.map(_.toLocalDate).getOrElse(currentDate())
    }

  override def nextChange(date: LocalDate): Future[LocalDate] =
    async {
      val lastDate = await(mostRecentChange(currentDate()))
      await(
        dsl
          .resultQuery(s"""
                        |SELECT $dateCol
                        |FROM $tableName
                        |WHERE $dateCol > '$date'
                        |ORDER BY $dateCol
                        |LIMIT 1
                        |""".stripMargin)
          .fetchAsyncScala[Date]
      ).headOption.map(_.toLocalDate).getOrElse(lastDate)
    }

  override def prevChange(date: LocalDate): Future[LocalDate] =
    async {
      await(
        dsl
          .resultQuery(s"""
                        |SELECT $dateCol
                        |FROM $tableName
                        |WHERE $dateCol < '$date'
                        |ORDER BY $dateCol DESC
                        |LIMIT 1
                        |""".stripMargin)
          .fetchAsyncScala[Date]
      ).headOption.map(_.toLocalDate).getOrElse(date)
    }

  override def segmentPositionOnDate(date: LocalDate, segmentId: String): Future[Option[SegmentToM1Pos]] =
    async {
      await(positionsOnDate(date)).find(_.maybeId.contains(segmentId))
    }

  override def segmentAtPositionOnDate(date: LocalDate, position: String): Future[Option[SegmentToM1Pos]] =
    async {
      val dbPos = toDbPosition(position)
      await(positionsOnDate(date)).find(_.dbPos == dbPos)
    }

  def resetSegmentToM1PosTable(): Future[Boolean] =
    async {
      await(dsl.truncate(tableName).executeAsyncScala()) >= 0
    }
}
