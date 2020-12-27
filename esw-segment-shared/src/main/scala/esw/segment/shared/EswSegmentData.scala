package esw.segment.shared

import java.sql.Date
import scala.language.implicitConversions

object EswSegmentData {

  val numSectors        = 6
  val segmentsPerSector = 82

  /**
   * Number of segments in the TMT primary mirror.
   * This is also the size of the positions array in the segment_to_m1_pos table.
   */
  val numSegments: Int = numSectors * segmentsPerSector

  /**
   * The TMT requires a total of 574 segments comprising 7 sets of the 82 unique
   * segments. 492 of these segments will form M1 and remaining 82 extra segments are used
   * to facilitate re-coating of the primary mirror and for use as spares.
   */
  val totalSegments: Int = numSegments + segmentsPerSector

  /**
   * Convert a segment position like F32 to a one based index in an array of all segments
   */
  def toDbPosition(loc: String): Int = {
    val sectorOffset = loc.head - 'A'
    val n          = loc.tail.toInt
    assert(sectorOffset >= 0 && sectorOffset < numSectors && n >= 1 && n <= segmentsPerSector)
    sectorOffset * segmentsPerSector + n
  }

  /**
   * Convert a one based index in an array of all segments to a segment position like F32 or A2
   */
  def toPosition(dbPos: Int): String = {
    assert(dbPos >= 1 && dbPos <= numSegments)
    val sectorOffset = (dbPos - 1) / 82
    val sector       = ('A' + sectorOffset).toChar
    val num          = (dbPos - 1) % segmentsPerSector + 1
    s"$sector$num"
  }

  implicit def toSqlDate(date: java.util.Date): java.sql.Date = {
    new java.sql.Date(date.getTime)
  }

  /**
   * Position of a segment on a given date
   *
   * @param date    date of record
   * @param maybeId the segment id, if the segment is present, None if it is missing
   * @param position  position of segment (For example: A32, B19, F82)
   */
  case class SegmentToM1Pos(date: Date, maybeId: Option[String], position: String) {
    def this(date: Date, id: String, position: String) = {
      this(date, Some(id), position)
    }
    def this(date: Date, id: String, dbPos: Int) = {
      this(date, Some(id), toPosition(dbPos))
    }

    /**
     * Internal one based index in array of all 492 segment positions
     */
    def dbPos: Int = toDbPosition(position)
  }

  /**
   * Positions of a number of segments on a given date.
   *
   * @param date      date of record
   * @param positions a list of pairs of (maybe-segment-id, segment-position) for zero or more segments
   *                  to be set/updated for the given date.
   *                  The value for maybe-segment is Some(segment-id) or None, if a segment is missing.
   *                  The positions are from A1 to F82.
   */
  case class SegmentToM1Positions(date: Date, positions: List[(Option[String], String)])

  /**
   * All of the segment positions for the given date.
   * The first item in the positions list is taken to be the segment position 1 and so on.
   *
   * @param date         the date corresponding to the positions
   * @param allPositions list of all 492 segment positions from A1 to F82 (Missing segments should be None,
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
