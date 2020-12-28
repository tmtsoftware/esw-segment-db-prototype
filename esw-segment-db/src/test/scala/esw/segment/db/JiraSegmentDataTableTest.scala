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
//      list.foreach(s => println(s"XXX Avail for $testPos: $s"))
      assert(List("SN-032", "SN-034", "SN-035", "SN-036", "SN-037", "SN-038", "SN-050") == list.sorted)
    }
  }

  test("Get planned positions") {
    async {
      val list = await(jiraSegmentDataTable.plannedPositions())
        .sortWith { (p, q) =>
          val s = p.position.head.compareTo(q.position.head)
          if (s == 0)
            p.position.tail.toInt.compareTo(q.position.tail.toInt) < 0
          else s < 0
        }
//      list.foreach(p => println(s"${p.position}: ${p.maybeId.getOrElse("----")}"))
      assert(list.size == 492)
    }
  }
}
