package esw.segment.shared

import esw.segment.shared.EswSegmentData.{DateRange, MirrorConfig, SegmentToM1Pos}

import java.time.LocalDate
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
   * @param config      the configuration for the mirror (date and segmentid assignments)
   * @return true if there were no problems
   */
  def setPositions(config: MirrorConfig): Future[Boolean]

  /**
   * Sets all segment ids (including spares) for the given date and returns true if successful.
   *
   * @param date         the date corresponding to the positions
   * @param allSegmentIds list of all segment ids (In order for segments A1 to G82, Missing segments should be None,
   *                     present segments should be Some(segment-id))
   * @return true if all is OK, false if the number of positions is less than 492 or the table row
   *         could not be added or updated
   */
  def setAllPositions(date: LocalDate, allSegmentIds: List[Option[String]]): Future[Boolean]

  /**
   * Gets a list of segments positions for the given segment id in the given date range.
   *
   * @param dateRange the range of dates to search
   * @param segmentId the segment id to search for
   * @return a list of objects indicating the positions of the given segment id in the given date range (sorted by date)
   */
  def segmentPositions(dateRange: DateRange, segmentId: String): Future[List[SegmentToM1Pos]]

  /**
   * Gets a list of segment ids that were in the given position (A1 to F82) in the given date range.
   *
   * @param dateRange the range of dates to search
   * @param position the segment position to search for (A1 to F82)
   * @return a list of segments at the given position in the given date range (sorted by date)
   */
  def segmentIds(dateRange: DateRange, position: String): Future[List[SegmentToM1Pos]]

  /**
   * Gets a list of segment ids that were in the given position (A1 to F82).
   *
   * @param position the segment position to search for (A1 to F82)
   * @return a list of segments at the given position in the given date range (sorted by date (desc))
   */
  def allSegmentIds(position: String): Future[List[SegmentToM1Pos]]

  /**
   * Returns a list of segments that were installed since the given date
   *
   * @param since the cutoff date for newly installed segments
   */
  def newlyInstalledSegments(since: LocalDate): Future[List[SegmentToM1Pos]]

  /**
   * Returns a list of segment exchanges since the given date.
   * Each object in the returned list contains the date and the list of position/segmentId that changed.
   *
   * @param since starting date
   */
  def segmentExchanges(since: LocalDate): Future[List[MirrorConfig]]

  /**
   * Returns the current segment positions
   * (Missing segments are also included in the returned list).
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
   * @param position the segment position to search for (A1 to F82)
   * @return Some object indicating the id of the segment, or None if no segment is installed at that position
   */
  def currentSegmentAtPosition(position: String): Future[Option[SegmentToM1Pos]]

  /**
   * Returns the segment positions as they were on the given date, sorted by date
   * (Missing segments are also included in the returned list).
   */
  def positionsOnDate(date: LocalDate): Future[List[SegmentToM1Pos]]

  /**
   * Returns the most recent date that segments were changed up to the given date, or the current date,
   * if there are no segments installed yet.
   */
  def mostRecentChange(date: LocalDate): Future[LocalDate]

  /**
   * Returns the next date after the given one where segments were changed, or the most recent change date,
   * if there are no newer changes.
   */
  def nextChange(date: LocalDate): Future[LocalDate]

  /**
   * Returns the previous date before the given one where segments were changed, or the first date,
   * if there are no older changes.
   */
  def prevChange(date: LocalDate): Future[LocalDate]

  /**
   * Gets the segment position for the given segment id on the given date.
   *
   * @param date      the date that the segment was in the position
   * @param segmentId the segment id to search for
   * @return Some object indicating the positions of the given segment, or None if the segment is not installed
   */
  def segmentPositionOnDate(date: LocalDate, segmentId: String): Future[Option[SegmentToM1Pos]]

  /**
   * Gets the id of the segment that was installed in the given position on the given date.
   *
   * @param date the date that the segment was in the position
   * @param position  the segment position to search for
   * @return Some object indicating the id of the segment, or None if no segment was installed at that position on the given date
   */
  def segmentAtPositionOnDate(date: LocalDate, position: String): Future[Option[SegmentToM1Pos]]

  /**
   * Drops and recreates the database tables (for testing)
   *
   * @return true if OK
   */
  def resetSegmentToM1PosTable(): Future[Boolean]
}