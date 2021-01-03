package esw.segment.db

import org.jooq.DSLContext
import csw.database.scaladsl.JooqExtentions._
import esw.segment.jira.JiraClient
import esw.segment.shared.{JiraSegmentData, JiraSegmentDataApi}

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}
import JiraSegmentDataTable._
import esw.segment.jira.JiraClient.{jiraBrowseUri, toPos}
import esw.segment.shared.EswSegmentData.SegmentToM1Pos
import esw.segment.shared.EswSegmentData._

object JiraSegmentDataTable {
  // Table and column names
  private[db] val tableName = "jira_segment_data"

  private val segmentIdCol                      = "segment_id"
  private val sectorCol                         = "sector"
  private val segmentTypeCol                    = "segment_type"
  private val statusCol                         = "status"
}

class JiraSegmentDataTable(dsl: DSLContext, jiraClient: JiraClient)(implicit ec: ExecutionContext) extends JiraSegmentDataApi {

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

  def syncWithJira(progress: Int => Unit): Future[Boolean] =
    async {
      val data = await(jiraClient.getAllJiraSegmentData(progress))
      await(insertDb(data))
    }

  override def availableSegmentIdsForPos(position: String): Future[List[String]] =
    async {
      val sector      = position.head - 'A' + 1
      val segmentType = position.tail.toInt
      assert(
        sector >= 1 && sector <= numSectors + 1
          && segmentType >= 1 && segmentType <= segmentsPerSector
      )
      await(
        dsl
          .resultQuery(
            s"""
               |SELECT $segmentIdCol
               |FROM $tableName
               |WHERE $segmentTypeCol = '$segmentType' AND $statusCol != 'Disposed'
               |""".stripMargin
          )
          .fetchAsyncScala[String]
      )
    }

  override def plannedPositions(): Future[List[SegmentToM1Pos]] =
    async {
      val date = currentDate()
      val queryResult = await(
        dsl
          .resultQuery(s"""
                        |SELECT $segmentIdCol, $sectorCol, $segmentTypeCol
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
