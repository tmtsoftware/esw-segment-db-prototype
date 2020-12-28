package esw.segment.db

import org.jooq.DSLContext
import csw.database.scaladsl.JooqExtentions._
import esw.segment.jira.JiraClient
import esw.segment.shared.{EswSegmentData, JiraSegmentData, JiraSegmentDataApi}

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}
import JiraSegmentDataTable._
import esw.segment.shared.EswSegmentData.SegmentToM1Pos
import esw.segment.shared.EswSegmentData._

import java.sql.Date

//noinspection ScalaUnusedSymbol
object JiraSegmentDataTable {
  // Table and column names
  private[db] val tableName = "jira_segment_data"

  private val dateCol                           = "date"
  private val segmentIdCol                      = "segment_id"
  private val jiraKeyCol                        = "jira_key"
  private val sectorCol                         = "sector"
  private val segmentTypeCol                    = "segment_type"
  private val partNumberCol                     = "part_number"
  private val originalPartnerBlankAllocationCol = "original_partner_blank_allocation"
  private val itemLocationCol                   = "item_location"
  private val riskOfLossCol                     = "risk_of_loss"
  private val componentsCol                     = "components"
  private val statusCol                         = "status"
  private val workPackagesCol                   = "work_packages"
  private val acceptanceCertificatesCol         = "acceptance_certificates"
  private val acceptanceDateBlankCol            = "acceptance_date_blank"

  // Convert sector and sectorType from JIRA to position like F32
  private def toPos(sector: Int, sectorType: Int): String  = {
    val sectorName = ('A' + sector - 1).toChar
    s"${sectorName}$sectorType"
  }
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
        sector >= 1 && sector <= numSectors
          && segmentType >= 1 && segmentType <= segmentsPerSector
      )
      await(
        dsl
          .resultQuery(
            s"""
               |SELECT $segmentIdCol
               |FROM $tableName
               |WHERE $segmentTypeCol = '${segmentType}' AND $statusCol != 'Disposed'
               |""".stripMargin
          )
          .fetchAsyncScala[String]
      )
    }

  override def plannedPositions(): Future[List[SegmentToM1Pos]] = async {
    val date = currentDate()
    val queryResult = await(
      dsl
        .resultQuery(s"""
                        |SELECT $segmentIdCol, $sectorCol, $segmentTypeCol
                        |FROM $tableName
                        |WHERE $sectorCol >= 1 AND $sectorCol <= 6
                        |AND $segmentTypeCol >= 1 AND $segmentTypeCol <= 82
                        |AND $statusCol != 'Disposed'
                        |""".stripMargin)
        .fetchAsyncScala[(String, Int, Int)]
    )
    if (queryResult.isEmpty) {
      // If the results are empty, return a row with empty ids
      (1 to numSegments).toList.map(pos => SegmentToM1Pos(date, None, toPosition(pos)))
    }
    else {
      queryResult.map { result =>
        val (segmentId, sector, segmentType)  = result
        val pos = toPos(sector, segmentType)
        SegmentToM1Pos(date, Option(segmentId), pos)
      }
    }
  }
}
