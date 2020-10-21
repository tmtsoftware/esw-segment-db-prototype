package esw.segment.db

import java.sql.Timestamp

/**
 * Position of a segment at a given time
 *
 * @param timestamp date/time of record
 * @param id segment id
 * @param pos position of segment
 */
case class SegmentToM1Pos(timestamp: Timestamp, id: String, pos: Int)

