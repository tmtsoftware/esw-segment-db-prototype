package esw.segment.db

import esw.segment.shared.SegmentToM1ApiTestBase
import EswSegmentToM1PosTableTest.*

object EswSegmentToM1PosTableTest {
  import DbWiring.*
  val wiring = new DbWiring(testDbName)
  lazy val posTable: SegmentToM1PosTable = wiring.segmentToM1PosTable
}

class EswSegmentToM1PosTableTest extends SegmentToM1ApiTestBase(posTable)
