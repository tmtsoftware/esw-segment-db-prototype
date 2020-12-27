package esw.segment.db

import JiraSegmentDataTableTest._
import org.scalatest.funsuite.AsyncFunSuite

import scala.async.Async.{async, await}

object JiraSegmentDataTableTest {
  import DbWiring._
  val wiring                                          = new DbWiring(testDbName)
  lazy val jiraSegmentDataTable: JiraSegmentDataTable = wiring.jiraSegmentDataTable

  private def progress(percent: Int): Unit = {
    println(s"Progress: $percent%")
  }
}

class JiraSegmentDataTableTest extends AsyncFunSuite {

  test("Test sync with JIRA") {
    async {
      val testPos = "A32"
      val list = {
        val x = await(jiraSegmentDataTable.availableSegmentIdsForPos(testPos))
        if (x.nonEmpty) x
        else {
          await(jiraSegmentDataTable.syncWithJira(progress))
          await(jiraSegmentDataTable.availableSegmentIdsForPos(testPos))
        }
      }
      list.foreach(s => println(s"XXX Avail for $testPos: $s"))
      assert(list.nonEmpty)
    }
  }
}
