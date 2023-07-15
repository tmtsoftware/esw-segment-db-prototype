package esw.segment.db

import JiraSegmentDataTableTest.*
import esw.segment.shared.EswSegmentData.totalSegments
import org.scalatest.funsuite.AsyncFunSuite

import scala.async.Async.{async, await}

object JiraSegmentDataTableTest {
  import DbWiring.*
  val wiring                                          = new DbWiring(testDbName)
  lazy val jiraSegmentDataTable: JiraSegmentDataTable = wiring.jiraSegmentDataTable
}

class JiraSegmentDataTableTest extends AsyncFunSuite {

  test("Test sync with JIRA") {
    async {
      val list = await(jiraSegmentDataTable.availableSegmentIdsForPos("A32"))
      assert(List("SN-032", "SN-034", "SN-035", "SN-036", "SN-037", "SN-038", "SN-050") == list.sorted)
    }
  }

  test("Get planned positions") {
    async {
      val list = await(jiraSegmentDataTable.plannedPositions())
      assert(list.size == totalSegments)
    }
  }
}
