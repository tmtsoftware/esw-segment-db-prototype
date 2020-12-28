package esw.segment.jira

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import akka.http.scaladsl.unmarshalling.Unmarshal
import esw.segment.shared.JiraSegmentData

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContextExecutor, Future}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials}
import spray.json._

//noinspection TypeAnnotation
object JiraClient extends SprayJsonSupport with DefaultJsonProtocol with NullOptions {
  private val jiraBaseUri   = "https://tmt-project.atlassian.net"
  private val jiraIssueUri  = s"$jiraBaseUri/rest/api/latest/issue"
  private val jiraSearchUri = s"$jiraBaseUri/rest/api/latest/search"
  val jiraBrowseUri         = s"$jiraBaseUri/browse"

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
    "riskOfLoss"                     -> "customfield_11912",
    "workPackages"                   -> "customfield_11910",
    "acceptanceCertificates"         -> "customfield_11903",
    "acceptanceDateBlank"            -> "customfield_11908",
    "shippingAuthorizations"         -> "customfield_11911"
  )

  private val customFieldsStr = customFieldMap.values.mkString(",")

  private def issueUri(issueNumber: Int) = s"$jiraIssueUri/M1ST-$issueNumber?fields=$customFieldsStr,components,status,summary"

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
      customfield_11910: Option[String],
      customfield_11903: Option[String],
      customfield_11908: Option[String],
      customfield_11911: Option[String],
      status: JiraStatus
  ) {
    def sector = customfield_12000

    def segmentType = customfield_11909

    def partNumber = customfield_11905.child.value

    def originalPartnerBlankAllocation = customfield_11907

    def itemLocation = customfield_11906

    def riskOfLoss = customfield_11912

    def workPackages = customfield_11910.getOrElse("").trim()

    def acceptanceCertificates = customfield_11903.getOrElse("").trim()

    def acceptanceDateBlank = customfield_11908.getOrElse("").trim()

    def shippingAuthorizations = customfield_11911.getOrElse("").trim()
  }

  private implicit val JiraFieldsFormat = jsonFormat13(JiraFields)

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

  // Convert sector and segmentType from JIRA to position like F32
  def toPos(sector: Int, segmentType: Int): String = {
    val sectorName = ('A' + sector - 1).toChar
    s"${sectorName}$segmentType"
  }
}

class JiraClient()(implicit typedSystem: ActorSystem[SpawnProtocol.Command], ec: ExecutionContextExecutor) {
  import JiraClient._

  /**
   * Gets the JIRA segment data for the given segment number
   *
   * @param issueNumber the number of the JIRA issue (sequential, starting with 1 until number of issues)
   */
  def getJiraSegmentData(
      issueNumber: Int
  ): Future[JiraSegmentData] =
    async {
      val uri                            = issueUri(issueNumber)
      val request                        = HttpRequest(HttpMethods.GET, uri = uri, headers = authHeaders)
      val response                       = await(Http().singleRequest(request))
      val jiraData                       = await(Unmarshal(response).to[JiraData])
      val segmentId                      = jiraData.fields.summary.replace("M1 Segment ", "")
      val jiraKey                        = jiraData.key
      val jiraUri                        = s"$jiraBrowseUri/$jiraKey"
      val sector                         = jiraData.fields.sector.value.split(' ').head.toIntOption.getOrElse(-1)
      val segmentType                    = jiraData.fields.segmentType.value.toIntOption.getOrElse(-1)
      val pos                            = JiraClient.toPos(sector, segmentType)
      val partNumber                     = jiraData.fields.partNumber
      val originalPartnerBlankAllocation = jiraData.fields.originalPartnerBlankAllocation.value
      val itemLocation                   = jiraData.fields.itemLocation.value
      val riskOfLoss                     = jiraData.fields.riskOfLoss.value
      val components                     = jiraData.fields.components.headOption.map(_.name).getOrElse("Unknown")
      val status                         = jiraData.fields.status.name
      val workPackages                   = jiraData.fields.workPackages
      val acceptanceCertificates         = jiraData.fields.acceptanceCertificates
      val acceptanceDateBlank            = jiraData.fields.acceptanceDateBlank
      val shippingAuthorizations         = jiraData.fields.shippingAuthorizations

      JiraSegmentData(
        pos,
        segmentId,
        jiraKey,
        jiraUri,
        sector,
        segmentType,
        partNumber,
        originalPartnerBlankAllocation,
        itemLocation,
        riskOfLoss,
        components,
        status,
        workPackages,
        acceptanceCertificates,
        acceptanceDateBlank,
        shippingAuthorizations
      )
    }

  /**
   * Gets the JIRA segment data for the given segment number
   *
   * @param issueNumber the number of the JIRA issue (sequential, 1 until number of issues)
   * @param accResult the accumulated result
   */
  def recursiveGetJiraSegmentData(
      issueCount: Int,
      issueNumber: Int,
      accResult: List[JiraSegmentData],
      progress: Int => Unit
  ): Future[List[JiraSegmentData]] =
    async {
      // Fetch multiple issues at once to improve performance
      val blockSize    = 10
      val issueNumbers = (math.max(issueNumber - blockSize + 1, 1) to issueNumber).toList
      val firstIssue   = issueNumbers.head
      val fList        = issueNumbers.map(getJiraSegmentData)
      val list         = await(Future.sequence(fList))
      val result       = list ++ accResult
      progress(((issueCount - firstIssue + 1.0) / issueCount * 100).toInt)
      if (firstIssue == 1) {
        result
      }
      else {
        await(recursiveGetJiraSegmentData(issueCount, issueNumber - blockSize, result, progress))
      }
    }

  /**
   * Gets the JIRA data for all issues
   */
  def getAllJiraSegmentData(progress: Int => Unit): Future[List[JiraSegmentData]] =
    async {
      val uri        = s"$jiraSearchUri?jql=project=SE-M1SEG&maxResults=0"
      val request    = HttpRequest(HttpMethods.GET, uri = uri, headers = authHeaders)
      val response   = await(Http().singleRequest(request))
      val issueCount = await(Unmarshal(response).to[IssueCount]).total
      await(recursiveGetJiraSegmentData(issueCount, issueCount, Nil, progress))
    }

}
