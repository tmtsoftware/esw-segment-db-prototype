package esw.segment.jira

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpMethods, HttpRequest}
import akka.http.scaladsl.unmarshalling.Unmarshal
import esw.segment.shared.JiraSegmentData

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContextExecutor, Future}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials, `Content-Type`}
import spray.json._

//noinspection TypeAnnotation
object JiraClient {
  import SprayJsonSupport._
  import DefaultJsonProtocol._

  private val jiraBaseUri   = "https://tmt-project.atlassian.net"
  private val jiraIssueUri  = s"$jiraBaseUri/rest/api/latest/issue"
  private val jiraSearchUri = s"$jiraBaseUri/rest/api/latest/search"
//  private val jiraBrowseUri = s"$jiraBaseUri/browse"

  if (!(sys.env.contains("JIRA_USER") && sys.env.contains("JIRA_API_TOKEN"))) {
    throw new RuntimeException("Please set JIRA_USER and JIRA_API_TOKEN environment variables.")
  }
  private val jiraUser     = sys.env("JIRA_USER")
  private val jiraApiToken = sys.env("JIRA_API_TOKEN")

  private val customFieldMap = Map(
    "sector"                         -> "customfield_12000",
    "segmentType"                    -> "customfield_11909",
    "partNumber"                     -> "customfield_11905",
    "originalPartnerBlankAllocation" -> "customfield_11907",
    "itemLocation"                   -> "customfield_11906",
    "riskOfLoss"                     -> "customfield_11912"
  )

  private val customFieldsStr = customFieldMap.values.mkString(",")

  private def issueUri(issueNumber: Int) = s"$jiraIssueUri/M1ST-$issueNumber?fields=${customFieldsStr},components,status,summary"

  private case class CustomField(self: String, value: String, id: String)

  private implicit val CustomFieldFormat = jsonFormat3(CustomField)

  private case class CustomFieldWithChild(self: String, value: String, id: String, child: CustomField)

  private implicit val CustomFieldWithChildFormat = jsonFormat4(CustomFieldWithChild)

  private case class JiraStatusCategory(
      self: String,
      id: Int,
      key: String,
      colorName: String,
      name: String
  )

  private implicit val JiraStatusCategoryFormat = jsonFormat5(JiraStatusCategory)

  private case class JiraStatus(
      self: String,
      description: String,
      iconUrl: String,
      name: String,
      id: String,
      statusCategory: JiraStatusCategory
  )

  private implicit val JiraStatusFormat = jsonFormat6(JiraStatus)

  private case class JiraComponent(self: String, id: String, name: String)

  private implicit val JiraComponentFormat = jsonFormat3(JiraComponent)

  private case class JiraFields(
      summary: String,
      components: List[JiraComponent],
      customfield_11912: CustomField,
      customfield_11906: CustomField,
      customfield_12000: CustomField,
      customfield_11905: CustomFieldWithChild,
      customfield_11907: CustomField,
      customfield_11909: CustomField,
      status: JiraStatus
  ) {
    def sector = customfield_12000

    def segmentType = customfield_11909

    def partNumber = customfield_11905.child.value

    def originalPartnerBlankAllocation = customfield_11907

    def itemLocation = customfield_11906

    def riskOfLoss = customfield_11912
  }

  private implicit val JiraFieldsFormat = jsonFormat9(JiraFields)

  private case class JiraData(
      expand: String,
      id: String,
      self: String,
      key: String,
      fields: JiraFields
  )

  private implicit val JiraDataFormat = jsonFormat5(JiraData)

  // {"startAt":0,"maxResults":0,"total":577,"issues":[]}
  private case class IssueCount(startAt: Int, maxResults: Int, total: Int, issues: List[String])
  private implicit val IssueCountFormat = jsonFormat4(IssueCount)

  private val authHeaders = List(Authorization(BasicHttpCredentials(jiraUser, jiraApiToken)))

  /**
   * Gets the JIRA segment data for the given segment number
   *
   * @param issueNumber the number of the JIRA issue (sequential, starting with 1 until number of issues)
   */
  def getJiraSegmentData(
      issueNumber: Int
  )(implicit typedSystem: ActorSystem[SpawnProtocol.Command], ec: ExecutionContextExecutor): Future[JiraSegmentData] =
    async {
      val uri                            = issueUri(issueNumber)
      val request                        = HttpRequest(HttpMethods.GET, uri = uri, headers = authHeaders)
      val response                       = await(Http().singleRequest(request))
      val jiraData                       = await(Unmarshal(response).to[JiraData])
      val segmentId                      = jiraData.fields.summary.replace("M1 Segment ", "")
      val jiraTask                       = jiraData.key
      val sector                         = jiraData.fields.sector.value.toIntOption.getOrElse(-1)
      val segmentType                    = jiraData.fields.segmentType.value.toIntOption.getOrElse(-1)
      val partNumber                     = jiraData.fields.partNumber
      val originalPartnerBlankAllocation = jiraData.fields.originalPartnerBlankAllocation.value
      val itemLocation                   = jiraData.fields.itemLocation.value
      val riskOfLoss                     = jiraData.fields.riskOfLoss.value
      val components                     = jiraData.fields.components.headOption.map(_.name).getOrElse("Unknown")
      val status                         = jiraData.fields.status.name
      JiraSegmentData(
        segmentId,
        jiraTask,
        sector,
        segmentType,
        partNumber,
        originalPartnerBlankAllocation,
        itemLocation,
        riskOfLoss,
        components,
        status
      )
    }

  /**
   * Gets the JIRA segment data for the given segment number
   *
   * @param issueNumber the number of the JIRA issue (sequential, starting with 1 until number of issues)
   */
  def recursiveGetJiraSegmentData(
      issueNumber: Int,
      result: List[JiraSegmentData]
  )(implicit typedSystem: ActorSystem[SpawnProtocol.Command], ec: ExecutionContextExecutor): Future[List[JiraSegmentData]] =
    async {
      val r = await(getJiraSegmentData(issueNumber)) :: result
      if (issueNumber == 1) {
        r
      } else {
        println(s"XXX Got issueNumber $issueNumber: ${r.head.jiraTask}")
        await(recursiveGetJiraSegmentData(issueNumber - 1, r))
      }
    }

  /**
   * Gets the JIRA data for all issues
   */
  def getAllJiraSegmentData(
  )(implicit typedSystem: ActorSystem[SpawnProtocol.Command], ec: ExecutionContextExecutor): Future[List[JiraSegmentData]] =
    async {
      val uri        = s"${jiraSearchUri}?jql=project=SE-M1SEG&maxResults=0"
      val request    = HttpRequest(HttpMethods.GET, uri = uri, headers = authHeaders)
      val response   = await(Http().singleRequest(request))
      val issueCount = await(Unmarshal(response).to[IssueCount]).total
      await(recursiveGetJiraSegmentData(issueCount, Nil))
    }

}
