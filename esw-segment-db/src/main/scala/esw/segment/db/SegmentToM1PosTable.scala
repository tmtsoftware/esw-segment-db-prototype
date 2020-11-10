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

  private def quoted(s: String) = "\"" + s + "\""

  private def currentDate(): Date = new Date(System.currentTimeMillis())

  private def makeSegmentToM1Pos(date: Date, id: String, dbPos: Int): SegmentToM1Pos = {
    val position = toPosition(dbPos)
    SegmentToM1Pos(date, if (id.startsWith(missingSegmentId)) None else Some(id), position)
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
   * Returns an array of all 492 current segment ids list as stored in the database,
   * or a list of missingSegmentId entries, if there are no database rows yet.
   */
  private def rawCurrentPositions(): Future[Array[String]] =
    async {
      import scala.jdk.CollectionConverters._
      import scala.compat.java8.FutureConverters.CompletionStageOps

      await(
        dsl
          .resultQuery(s"""
         |SELECT $positionsCol
         |FROM $tableName
         |WHERE date <= '${currentDate()}'
         |ORDER BY date DESC
         |LIMIT 1
         |""".stripMargin)
          // XXX FIXME: CSW's fetchAsyncScala does not handle array results!
          .fetchAsync()
          .toScala
          .map(_.asScala.map(_.into(classOf[Object]).asInstanceOf[Array[String]]).toArray)
      ).headOption
        .getOrElse((1 to numSegments).map(_ => missingSegmentId).toArray)
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
      val allSegmentIdsStr = await(rawCurrentPositions()).map(quoted).mkString(",")
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
   * @param date           Use the last install date before this one
   * @param segmentToM1Pos the segment position to use
   */
  private def withInstallDate(date: Date, segmentToM1Pos: SegmentToM1Pos): Future[SegmentToM1Pos] =
    async {
      if (segmentToM1Pos.maybeId.isEmpty)
        segmentToM1Pos
      else {
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
           | WHERE date <= '$date'
           | ORDER BY date DESC
           |) as w1
           |WHERE
           |  w1.id = '${segmentToM1Pos.maybeId.get}' AND w1.id IS DISTINCT FROM w1.next_id
           |ORDER BY date DESC
           |LIMIT 1;
           |""".stripMargin)
            .fetchAsyncScala[Date]
        )
        SegmentToM1Pos(queryResult.head, segmentToM1Pos.maybeId, segmentToM1Pos.position)
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

  override def setPosition(segmentToM1Pos: SegmentToM1Pos): Future[Boolean] =
    async {
      // Make sure the row exists
      val rowStatus = await(rowExists(segmentToM1Pos.date)) || await(addRow(segmentToM1Pos.date))

      if (rowStatus) {
        await(removeSegmentIds(segmentToM1Pos.date, segmentToM1Pos.maybeId.toList))
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
      val fList = list.map(s => withInstallDate(dateRange.to, s))
      await(Future.sequence(fList)).distinct.sortWith(_.dbPos < _.dbPos)
    }

  override def segmentIds(dateRange: DateRange, position: String): Future[List[SegmentToM1Pos]] =
    async {
      val dbPos = toDbPosition(position)
      val queryResult = await(
        dsl
          .resultQuery(s"""
         |SELECT $dateCol, $positionsCol[$dbPos]
         |FROM $tableName
         |WHERE $dateCol >= '${dateRange.from}' AND $dateCol <= '${dateRange.to}' AND $positionsCol[$dbPos] != '$missingSegmentId'
         |""".stripMargin)
          .fetchAsyncScala[(Date, String)]
      )
      val list = queryResult.map { result =>
        val date      = result._1
        val segmentId = result._2
        makeSegmentToM1Pos(date, segmentId, dbPos)
      }
      val fList = list.map(s => withInstallDate(dateRange.to, s))
      await(Future.sequence(fList)).distinct.sortWith(_.maybeId.get < _.maybeId.get)
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
      val list = queryResult.flatMap { result =>
        val date        = result._1
        val dbPositions = result._2
        dbPositions.zipWithIndex
          .filter(p => !p._1.startsWith(missingSegmentId))
          .map(p => makeSegmentToM1Pos(date, p._1, p._2 + 1))
      }
      val fList = list
        .map(s => withInstallDate(currentDate(), s))
      await(Future.sequence(fList)).distinct
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
