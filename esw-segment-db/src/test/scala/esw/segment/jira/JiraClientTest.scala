package esw.segment.jira

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import esw.segment.shared.JiraSegmentData
import org.scalatest.funsuite.AsyncFunSuite

import scala.concurrent.ExecutionContextExecutor
import scala.async.Async.{async, await}

class JiraClientTest extends AsyncFunSuite {
  implicit lazy val typedSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "EswSegmentDb")
  implicit lazy val ec: ExecutionContextExecutor                    = typedSystem.executionContext

  private def test72(issueData: JiraSegmentData) = async {
    assert(issueData.segmentId == "SN-072")
    assert(issueData.jiraTask == "M1ST-72")
    assert(issueData.sector == 6)
    assert(issueData.segmentType == 9)
    assert(issueData.partNumber == "001-01002 TMT M1 Meniscus Segment Blank")
    assert(issueData.itemLocation == "Canon")
    assert(issueData.riskOfLoss == "NINS")
    // XXX These values might change!
    assert(issueData.components == "In-Work Roundel")
    assert(issueData.status == "In Other Storage")
  }

  test("Test getting all JIRA segment data") {
    async {
      val data = await(JiraClient.getAllJiraSegmentData())
      assert(data.size >= 577)
      val issueData = data.find(_.jiraTask == "M1ST-72").get
      await(test72(issueData))
    }
  }

  test("Test getting segment data from single issue") {
    async {
      val issueData = await(JiraClient.getJiraSegmentData(72))
      await(test72(issueData))
    }
  }
}
