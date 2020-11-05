package esw.segment.shared

import java.sql.Date
import scala.language.implicitConversions

object EswSegmentData {

  /**
   * Number of segments in the TMT primary mirror.
   * This is also the size of the positions array in the segment_to_m1_pos table.
   */
  val numSegments = 492


  /**
   * The TMT requires a total of 574 segments comprising 7 sets of the 82 unique
   * segments. 492 of these segments will form M1 and remaining 82 extra segments are used
   * to facilitate re-coating of the primary mirror and for use as spares.
   */
  val totalSegments = 574

  implicit def toSqlDate(date: java.util.Date): java.sql.Date = {
    new java.sql.Date(date.getTime)
  }

  /**
   * Position of a segment on a given date
   *
   * @param date    date of record
   * @param maybeId the segment id, if the segment is present, None if it is missing
   * @param pos     position of segment
   */
  case class SegmentToM1Pos(date: Date, maybeId: Option[String], pos: Int) {
    def this(date: Date, id: String, pos: Int) = {
      this(date, Some(id), pos)
    }
  }

  /**
   * Positions of a number of segments on a given date.
   *
   * @param date      date of record
   * @param positions a list of pairs of (segment-id, maybe-position) for zero or more segments
   *                  to be set/updated for the given date.
   *                  index+1 is the position. The value is Some(segment-id) or None, if
   *                  a segment is missing.
   */
  case class SegmentToM1Positions(date: Date, positions: List[(Option[String], Int)])

  /**
   * All of the segment positions for the given date.
   * The first item in the positions list is taken to be the segment position 1 and so on.
   *
   * @param date         the date corresponding to the positions
   * @param allPositions list of all 492 segment positions (Missing segments should be None,
   *                     present segments should be Some(segment-id)
   */
  case class AllSegmentPositions(date: Date, allPositions: List[Option[String]])

  /**
   * A range of dates
   *
   * @param from start of the date range
   * @param to   end of the date range
   */
  case class DateRange(from: Date, to: Date)

}
