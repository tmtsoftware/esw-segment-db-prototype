package esw.segment.shared

import esw.segment.shared.EswSegmentData.SegmentToM1Pos

import scala.concurrent.Future

trait JiraSegmentDataApi {

  /**
   * Syncs the database with the JIRA tasks
   * @param progress a function called withe the percent done (can take a while)
   * @return true when done, if OK
   */
  def syncWithJira(progress: Int => Unit): Future[Boolean]

  /**
   * Returns the segment positions as defined in the JIRA issues
   * (Missing segments are also included in the returned list).
   */
  def plannedPositions(): Future[List[SegmentToM1Pos]]

  /**
   * Gets a list of segment-ids that can be installed at the given position
   * @param position A1 to F82
   * @return a list of ids of spare or available segments compatible with the given position
   */
  def availableSegmentIdsForPos(position: String): Future[List[String]]

  /**
   * Truncates the database table
   */
  def resetJiraSegmentDataTable(): Future[Boolean]
}
