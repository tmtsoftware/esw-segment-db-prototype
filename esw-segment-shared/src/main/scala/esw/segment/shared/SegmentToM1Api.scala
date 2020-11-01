package esw.segment.shared

import java.sql.Date

import esw.segment.shared.EswSegmentData.{DateRange, SegmentToM1Pos}

import scala.concurrent.Future

trait SegmentToM1Api {

  /**
   * Sets or updates the date and position of the given segment in the table and returns true if successful
   *
   * @param segmentToM1Pos holds the date, id and pos
   */
  def setPosition(segmentToM1Pos: SegmentToM1Pos): Future[Boolean]

  /**
   * Sets or updates the positions of the given segments for the given date in the table and returns true if successful.
   *
   * @param date      the date corresponding to the positions
   * @param positions a list of pairs of (segment-id, position) for zero or more segments to be set/updated
   *                  for the given date
   * @return true if there were no problems
   */
  def setPositions(date: Date, positions: List[(Option[String], Int)]): Future[Boolean]

  /**
   * Sets all of the segment positions for the given date and returns true if successful.
   * The first item in the positions list is taken to be the segment position 1 and so on.
   *
   * @param date         the date corresponding to the positions
   * @param allPositions list of all 492 segment positions (Missing segments should be None,
   *                     present segments should be Some(segment-id)
   * @return true if all is OK, false if the number of positions is less than 492 or the table row
   *         could not be added or updated
   */
  def setAllPositions(date: Date, allPositions: List[Option[String]]): Future[Boolean]

  /**
   * Gets a list of segments positions for the given segment id in the given date range.
   *
   * @param dateRange the range of dates to search
   * @param segmentId the segment id to search for
   * @return a list of objects indicating the positions of the given segment id in the given date range (sorted by position)
   */
  def segmentPositions(dateRange: DateRange, segmentId: String): Future[List[SegmentToM1Pos]]

  /**
   * Gets a list of segment ids that were in the given position in the given date range.
   *
   * @param dateRange the range of dates to search
   * @param pos       the segment position to search for
   * @return a list of segments at the given position in the given date range (sorted by id)
   */
  def segmentIds(dateRange: DateRange, pos: Int): Future[List[SegmentToM1Pos]]

  /**
   * Returns a list of segments that were installed since the given date
   *
   * @param since the cutoff date for newly installed segments
   */
  def newlyInstalledSegments(since: Date): Future[List[SegmentToM1Pos]]

  /**
   * Returns the current segment positions, sorted by position
   * (Missing segments are not included in the returned list).
   */
  def currentPositions(): Future[List[SegmentToM1Pos]]

  /**
   * Gets the current segment position for the given segment id.
   *
   * @param segmentId the segment id to search for
   * @return Some object indicating the positions of the given segment, or None if the segment is not installed
   */
  def currentSegmentPosition(segmentId: String): Future[Option[SegmentToM1Pos]]

  /**
   * Gets the id of the segment currently in the given position.
   *
   * @param pos the segment position to search for
   * @return Some object indicating the id of the segment, or None if no segment is installed at that position
   */
  def currentSegmentAtPosition(pos: Int): Future[Option[SegmentToM1Pos]]

  /**
   * Returns the segment positions as they were on the given date, sorted by position
   * (Missing segments are not included in the returned list).
   */
  def positionsOnDate(date: Date): Future[List[SegmentToM1Pos]]

  /**
   * Gets the segment position for the given segment id on the given date.
   *
   * @param date      the date that the segment was in the position
   * @param segmentId the segment id to search for
   * @return Some object indicating the positions of the given segment, or None if the segment is not installed
   */
  def segmentPositionOnDate(date: Date, segmentId: String): Future[Option[SegmentToM1Pos]]

  /**
   * Gets the id of the segment that was installed in the given position on the given date.
   *
   * @param date the date that the segment was in the position
   * @param pos  the segment position to search for
   * @return Some object indicating the id of the segment, or None if no segment was installed at that position on the given date
   */
  def segmentAtPositionOnDate(date: Date, pos: Int): Future[Option[SegmentToM1Pos]]

  /**
   * Drops and recreates the database tables (for testing)
   *
   * @return true if OK
   */
  def resetTables(): Future[Boolean]
}