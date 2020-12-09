package esw.segment.shared

import esw.segment.shared.EswSegmentData.SegmentToM1Pos

import scala.concurrent.Future

trait JiraSegmentDataApi {
  /**
   * Returns the segment positions as defined in the JIRA issues
   * (Missing segments are also included in the returned list).
   */
  def currentPositions(): Future[List[SegmentToM1Pos]]

}
