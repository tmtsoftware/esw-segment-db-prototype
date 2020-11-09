// These should match the JSON output of the classes on the server side in EswSegmentData.scala

export class SegmentData {
  // TODO: Configure
  static baseUri = "http://localhost:9192"
}

export interface SegmentToM1Pos {
  date: number
  maybeId?: string
  position: string
}

// case class AllSegmentPositions(date: Date, allPositions: List[Option[String]])
export interface AllSegmentPositions {
  date: number
  // null entry means missing segment
  allPositions: Array<string>
}

export interface DateRange {
  from: number
  to: number
}

// XXX SegmentToM1Positions: {"date":1603231200000,"positions":[["SN000X","F32"],[null,"F33"]]}
// case class SegmentToM1Positions(date: Date, positions: List[(Option[String], String)])
export interface SegmentToM1Positions {
  date: number
  // Array of pairs of (segment-id, pos) null id means missing segment
  positions: Array<Array<string>>
}

