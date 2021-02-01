package esw.segment.shared

import java.time.LocalDate

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
  def toDbPosition(pos: String): Int = {
    val sectorOffset = pos.head - 'A'
    val n          = pos.tail.toInt
    val valid = (sectorOffset >= 0 && sectorOffset <= numSectors && n >= 1 && n <= segmentsPerSector)
    if (!valid) throw new IllegalArgumentException(s"Invalid segment position: '$pos'")
    sectorOffset * segmentsPerSector + n
  }

  def validatePosition(pos: String): Unit = {
      toDbPosition(pos)
  }

  /**
   * Convert a one based index in an array of all segments to a segment position like F32 or A2
   */
  def toPosition(dbPos: Int): String = {
    val valid = dbPos >= 1 && dbPos <= totalSegments
    if (!valid) throw new IllegalArgumentException(s"Invalid segment position index: '$dbPos'")
    val sectorOffset = (dbPos - 1) / 82
    val sector       = ('A' + sectorOffset).toChar
    val num          = (dbPos - 1) % segmentsPerSector + 1
    s"$sector$num"
  }

  def currentDate(): LocalDate = LocalDate.now()

  /**
   * Position of a segment on a given date
   *
   * @param date    date of record
   * @param maybeId the segment id, if the segment is present, None if it is missing
   * @param position  position of segment (For example: A32, B19, F82)
   */
  case class SegmentToM1Pos(date: LocalDate, maybeId: Option[String], position: String) {
    validatePosition(position)
    def this(date: LocalDate, id: String, position: String) = {
      this(date, Some(id), position)
    }
    def this(date: LocalDate, id: String, dbPos: Int) = {
      this(date, Some(id), toPosition(dbPos))
    }

    /**
     * Internal one based index in array of all 492 segment positions
     */
    def dbPos: Int = toDbPosition(position)
  }

  /**
   * All of the segment positions for the given date.
   * The first item in the positions list is taken to be the segment position 1 and so on.
   *
   * @param date         the date corresponding to the positions
   * @param allPositions list of all 574 segment positions from A1 to G82 (Missing segments should be None,
   *                     present segments should be Some(segment-id)
   */
  case class AllSegmentPositions(date: LocalDate, allPositions: List[Option[String]])

  /**
   * A range of dates
   *
   * @param from start of the date range
   * @param to   end of the date range
   */
  case class DateRange(from: LocalDate, to: LocalDate)


  def sortByDate(list: List[SegmentToM1Pos], desc: Boolean = false): List[SegmentToM1Pos] = {
    if (desc)
      list.sortWith((p, q) => q.date.compareTo(p.date) < 0)
    else
      list.sortWith((p, q) => p.date.compareTo(q.date) > 0)
  }

  /**
   * Segment data for import/export from web app
   * @param position segment poosition (A1 to G82)
   * @param segmentId SN-xxx for the position
   */
  case class SegmentConfig(position: String, segmentId: Option[String]) {
    validatePosition(position)
  }

  /**
   * Holds a number of segment-id assignments for the mirror
   * @param date the date for the configuration
   * @param segments list of segment assignments
   */
  case class MirrorConfig(date: LocalDate, segments: List[SegmentConfig])
}
