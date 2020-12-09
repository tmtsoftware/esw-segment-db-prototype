package esw.segment.db

import org.jooq.DSLContext
import csw.database.scaladsl.JooqExtentions._
import esw.segment.shared.JiraSegmentDataApi

import scala.concurrent.ExecutionContext

object JiraSegmentDataTable {
  // Table and column names
  private[db] val tableName  = "jira_segment_data"

  private val dateCol        = "date"
  private val segmentIdCol = "segment_id"
  private val jiraKeyCol = "jira_key"
  private val sectorCol = "sector"
  private val segmentTypeCol = "segment_type"
  private val partNumberCol = "part_number"
  private val partnerCol = "partner"
  private val itemLocationCol = "item_location"
  private val riskOfLossCol = "risk_of_loss"
  private val componentsCol = "components"
  private val statusCol = "status"

}

class JiraSegmentDataTable(dsl: DSLContext)(implicit ec: ExecutionContext) extends JiraSegmentDataApi {

}
