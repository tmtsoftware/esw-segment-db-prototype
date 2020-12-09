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
    assert(issueData.jiraKey == "M1ST-72")
    assert(issueData.sector == 6)
    assert(issueData.segmentType == 9)
    assert(issueData.partNumber == "001-01002 TMT M1 Meniscus Segment Blank")
    assert(issueData.originalPartnerBlankAllocation == "Japan")
    assert(issueData.itemLocation == "Canon")
    assert(issueData.riskOfLoss == "NINS")
    // XXX These values might change!
    assert(issueData.components == "In-Work Roundel")
    assert(issueData.status == "In Other Storage")
  }

  private def test177(issueData: JiraSegmentData) = async {
    assert(issueData.segmentId == "SN-177")
    assert(issueData.jiraKey == "M1ST-177")
    assert(issueData.sector == 1)
    assert(issueData.segmentType == 38)
    assert(issueData.partNumber == "001-01002 TMT M1 Meniscus Segment Blank")
    assert(issueData.itemLocation == "Coherent")
    assert(issueData.originalPartnerBlankAllocation == "US")
    assert(issueData.riskOfLoss == "TIO")
    // XXX These values might change!
    assert(issueData.components == "In-Work Roundel")
    assert(issueData.status == "In Progress")
    assert(issueData.workPackages == "JP0010 - NINS - NAOJ - JFY16 - M1 Segment Blanks,  TMT.OPT.CON.16.001.CCR01")
    assert(issueData.acceptanceCertificates == "TMT.PMO.CON.17.007 (Blank)")
    assert(issueData.acceptanceDateBlank == "2017-03-29")
  }

  test("Test getting all JIRA segment data") {
    async {
      val data = await(JiraClient.getAllJiraSegmentData())
      assert(data.size >= 577)

      val issueData72 = data.find(_.jiraKey == "M1ST-72").get
      await(test72(issueData72))

      val issueData177 = data.find(_.jiraKey == "M1ST-177").get
      await(test177(issueData177))
    }
  }

  test("Test getting segment data from single issue") {
    async {
      val issueData72 = await(JiraClient.getJiraSegmentData(72))
      await(test72(issueData72))

      val issueData177 = await(JiraClient.getJiraSegmentData(177))
      await(test177(issueData177))
    }
  }
}
