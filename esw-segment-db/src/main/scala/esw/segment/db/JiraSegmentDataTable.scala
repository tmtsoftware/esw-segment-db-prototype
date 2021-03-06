package esw.segment.db

import org.jooq.DSLContext
import csw.database.scaladsl.JooqExtentions._
import esw.segment.jira.JiraClient
import esw.segment.shared.{JiraSegmentData, JiraSegmentDataApi, JsonSupport}
import spray.json._

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}
import JiraSegmentDataTable._
import esw.segment.jira.JiraClient.{jiraBrowseUri, toPos}
import esw.segment.shared.EswSegmentData.SegmentToM1Pos
import esw.segment.shared.EswSegmentData._

import java.nio.charset.StandardCharsets

object JiraSegmentDataTable {
  // Table and column names
  private[db] val tableName = "jira_segment_data"

  private val segmentIdCol   = "segment_id"
  private val sectorCol      = "sector"
  private val segmentTypeCol = "segment_type"
  private val statusCol      = "status"
}

class JiraSegmentDataTable(dsl: DSLContext, jiraClient: JiraClient)(implicit ec: ExecutionContext)
    extends JiraSegmentDataApi
    with JsonSupport {

  // If the JIRA segment data table is empty, initialize it from a resource file,
  // which will be update periodically. You can still "sync with JIRA" to get the latest
  // data if needed, or used the command line client to generate a new resource file.
  private def maybeInitFromResourceFile(): Future[Boolean] =
    async {
      val count = await(segmentDataCount())
      if (count != 0) true
      else {
        val bytes       = getClass.getResourceAsStream("/jiraData.json").readAllBytes()
        val json        = new String(bytes, StandardCharsets.UTF_8)
        val segmentData = json.parseJson.convertTo[List[JiraSegmentData]]
        await(insertDb(segmentData))
      }
    }

  // Return count of JIRA segment data rows in the DB
  private def segmentDataCount(): Future[Int] =
    async {
      val queryResult = await(
        dsl
          .resultQuery(s"""
                          |SELECT COUNT(*)
                          |FROM $tableName
                          |WHERE $sectorCol >= 1 AND $sectorCol <= 7
                          |AND $segmentTypeCol >= 1 AND $segmentTypeCol <= 82
                          |AND $statusCol != 'Disposed'
                          |""".stripMargin)
          .fetchAsyncScala[Int]
      )
      queryResult.head
    }

  // Recursively insert the list items in the DB and return true if OK
  private def insertDb(list: List[JiraSegmentData]): Future[Boolean] = {
    async {
      if (list.isEmpty)
        true
      else {
        val item = list.head
        import item._
        val result = await(
          dsl
            .query(s"""INSERT INTO $tableName VALUES (
                      |'$segmentId',
                      |'$jiraKey',
                      |'$sector',
                      |'$segmentType',
                      |'$partNumber',
                      |'$originalPartnerBlankAllocation',
                      |'$itemLocation',
                      |'$riskOfLoss',
                      |'$components',
                      |'$status',
                      |'$workPackages',
                      |'$acceptanceCertificates',
                      |'$acceptanceDateBlank',
                      |'$shippingAuthorizations')
                      |""".stripMargin)
            .executeAsyncScala()
        ) == 1
        if (result) await(insertDb(list.tail)) else result
      }
    }
  }

  override def syncWithJira(progress: Int => Unit): Future[Boolean] =
    async {
      val data = await(jiraClient.getAllJiraSegmentData(progress))
      await(resetJiraSegmentDataTable()) && await(insertDb(data))
    }

  override def availableSegmentIdsForPos(position: String): Future[List[String]] =
    async {
      await(maybeInitFromResourceFile())
      val sector      = position.head - 'A' + 1
      val segmentType = position.tail.toInt
      val valid = sector >= 1 && sector <= numSectors + 1 && segmentType >= 1 && segmentType <= segmentsPerSector
      if (!valid) throw new IllegalArgumentException(s"Invalid segment position: '$position'")
      await(
        dsl
          .resultQuery(
            s"""
               |SELECT DISTINCT $segmentIdCol
               |FROM $tableName
               |WHERE $segmentTypeCol = '$segmentType' AND $statusCol != 'Disposed'
               |""".stripMargin
          )
          .fetchAsyncScala[String]
      )
    }

  override def plannedPositions(): Future[List[SegmentToM1Pos]] =
    async {
      await(maybeInitFromResourceFile())
      val date = currentDate()
      val queryResult = await(
        dsl
          .resultQuery(s"""
                        |SELECT DISTINCT $segmentIdCol, $sectorCol, $segmentTypeCol
                        |FROM $tableName
                        |WHERE $sectorCol >= 1 AND $sectorCol <= 7
                        |AND $segmentTypeCol >= 1 AND $segmentTypeCol <= 82
                        |AND $statusCol != 'Disposed'
                        |""".stripMargin)
          .fetchAsyncScala[(String, Int, Int)]
      )
      if (queryResult.isEmpty) {
        // If the results are empty, return a row with empty ids
        (1 to totalSegments).toList.map(pos => SegmentToM1Pos(date, None, toPosition(pos)))
      }
      else {
        queryResult.map { result =>
          val (segmentId, sector, segmentType) = result
          val pos                              = toPos(sector, segmentType)
          SegmentToM1Pos(date, Option(segmentId), pos)
        }
      }
    }

  override def segmentData(): Future[List[JiraSegmentData]] =
    async {
      await(maybeInitFromResourceFile())
      val queryResult = await(
        dsl
          .resultQuery(s"""
                        |SELECT *
                        |FROM $tableName
                        |WHERE $sectorCol >= 1 AND $sectorCol <= 7
                        |AND $segmentTypeCol >= 1 AND $segmentTypeCol <= 82
                        |AND $statusCol != 'Disposed'
                        |""".stripMargin)
          .fetchAsyncScala[
            (String, String, Int, Int, String, String, String, String, String, String, String, String, String, String)
          ]
      )
      queryResult.map { result =>
        val (
          segmentId,
          jiraKey,
          sector,
          segmentType,
          partNumber,
          originalPartnerBlankAllocation,
          itemLocation,
          riskOfLoss,
          components,
          status,
          workPackages,
          acceptanceCertificates,
          acceptanceDateBlank,
          shippingAuthorizations
        )           = result
        val pos     = JiraClient.toPos(sector, segmentType)
        val jiraUri = s"$jiraBrowseUri/$jiraKey"
        JiraSegmentData(
          pos,
          segmentId,
          jiraKey,
          jiraUri,
          sector,
          segmentType,
          partNumber,
          originalPartnerBlankAllocation,
          itemLocation,
          riskOfLoss,
          components,
          status,
          workPackages,
          acceptanceCertificates,
          acceptanceDateBlank,
          shippingAuthorizations
        )
      }
    }

  override def resetJiraSegmentDataTable(): Future[Boolean] =
    async {
      await(dsl.truncate(tableName).executeAsyncScala()) >= 0
    }
}
