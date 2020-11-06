// These should match the JSON output of the classes on the server side in EswSegmentData.scala

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

export class SegmentData {
  // TODO: Configure
  static baseUri = "http://localhost:9192"

  // Get the current mirror positions from the server
  static currentPositions() {
    const uri = `${SegmentData.baseUri}/currentPositions`
    fetch(uri)
      .then(response => response.json())
      .then(data => {
        const currentPositions: Array<SegmentToM1Pos> = data
        for (const p of currentPositions) {
          console.log(`XXX ${p.date}, ${p.maybeId}, ${p.position}`)
        }
      });
  }
}
