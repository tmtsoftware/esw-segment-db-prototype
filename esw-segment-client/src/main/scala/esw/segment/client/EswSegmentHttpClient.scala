package esw.segment.client

import esw.segment.shared.{EswSegmentData, JiraSegmentData, JiraSegmentDataApi, JsonSupport, SegmentToM1Api}
import EswSegmentData._
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, Uri}
import spray.json._
import akka.http.scaladsl.unmarshalling.Unmarshal
import EswSegmentClientOptions._

import java.time.LocalDate
import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * HTTP Client for ESW Segment DB HTTP Server
 */
//noinspection DuplicatedCode
class EswSegmentHttpClient(host: String = "localhost", port: Int = defaultPort)(implicit
    actorSystem: ActorSystem,
    ec: ExecutionContextExecutor
) extends SegmentToM1Api
    with JiraSegmentDataApi
    with JsonSupport {

  private val baseUri = s"http://$host:$port"

  private def postSet(uri: Uri, json: String): Future[Boolean] =
    async {
      val entity   = HttpEntity(ContentTypes.`application/json`, json)
      val request  = HttpRequest(HttpMethods.POST, uri = uri, entity = entity)
      val response = await(Http().singleRequest(request))
      response.status == OK
    }

  private def postGetList(uri: Uri, json: String): Future[List[SegmentToM1Pos]] =
    async {
      val entity   = HttpEntity(ContentTypes.`application/json`, json)
      val request  = HttpRequest(HttpMethods.POST, uri = uri, entity = entity)
      val response = await(Http().singleRequest(request))
      await(Unmarshal(response).to[List[SegmentToM1Pos]])
    }

  private def postGetDate(uri: Uri, date: LocalDate) =
    async {
      val json     = date.toJson.compactPrint
      val entity   = HttpEntity(ContentTypes.`application/json`, json)
      val request  = HttpRequest(HttpMethods.POST, uri = uri, entity = entity)
      val response = await(Http().singleRequest(request))
      await(Unmarshal(response).to[LocalDate])
    }

  private def postGetOption(uri: Uri, json: String): Future[Option[SegmentToM1Pos]] =
    async {
      val entity   = HttpEntity(ContentTypes.`application/json`, json)
      val request  = HttpRequest(HttpMethods.POST, uri = uri, entity = entity)
      val response = await(Http().singleRequest(request))
      await(Unmarshal(response).to[Option[SegmentToM1Pos]])
    }

  private def getOption(uri: Uri): Future[Option[SegmentToM1Pos]] =
    async {
      val request  = HttpRequest(HttpMethods.GET, uri = uri)
      val response = await(Http().singleRequest(request))
      await(Unmarshal(response).to[Option[SegmentToM1Pos]])
    }

  // --

  override def setPosition(segmentToM1Pos: SegmentToM1Pos): Future[Boolean] = {
    postSet(Uri(s"$baseUri/setPosition"), segmentToM1Pos.toJson.compactPrint)
  }

  override def setPositions(config: MirrorConfig): Future[Boolean] = {
    postSet(Uri(s"$baseUri/setPositions"), config.toJson.compactPrint)
  }

  override def setAllPositions(date: LocalDate, allSegmentIds: List[Option[String]]): Future[Boolean] = {
    postSet(Uri(s"$baseUri/setAllPositions"), AllSegmentPositions(date, allSegmentIds).toJson.compactPrint)
  }

  override def segmentPositions(dateRange: DateRange, segmentId: String): Future[List[SegmentToM1Pos]] = {
    postGetList(Uri(s"$baseUri/segmentPositions/$segmentId"), dateRange.toJson.compactPrint)
  }

  override def segmentIds(dateRange: DateRange, position: String): Future[List[SegmentToM1Pos]] = {
    postGetList(Uri(s"$baseUri/segmentIds/$position"), dateRange.toJson.compactPrint)
  }

  override def allSegmentIds(position: String): Future[List[SegmentToM1Pos]] =
    async {
      val request  = HttpRequest(HttpMethods.GET, uri = Uri(s"$baseUri/allSegmentIds/$position"))
      val response = await(Http().singleRequest(request))
      await(Unmarshal(response).to[List[SegmentToM1Pos]])
    }

  override def newlyInstalledSegments(since: LocalDate): Future[List[SegmentToM1Pos]] = {
    postGetList(Uri(s"$baseUri/newlyInstalledSegments"), since.toJson.compactPrint)
  }

  override def segmentExchanges(since: LocalDate): Future[List[MirrorConfig]] =
    async {
      val uri      = Uri(s"$baseUri/segmentExchanges")
      val json     = since.toJson.compactPrint
      val entity   = HttpEntity(ContentTypes.`application/json`, json)
      val request  = HttpRequest(HttpMethods.POST, uri = uri, entity = entity)
      val response = await(Http().singleRequest(request))
      await(Unmarshal(response).to[List[MirrorConfig]])
    }

  override def positionsOnDate(date: LocalDate): Future[List[SegmentToM1Pos]] = {
    postGetList(Uri(s"$baseUri/positionsOnDate"), date.toJson.compactPrint)
  }

  override def mostRecentChange(date: LocalDate): Future[LocalDate] = {
    postGetDate(Uri(s"$baseUri/mostRecentChange"), date)
  }

  override def nextChange(date: LocalDate): Future[LocalDate] = {
    postGetDate(Uri(s"$baseUri/nextChange"), date)
  }

  override def prevChange(date: LocalDate): Future[LocalDate] = {
    postGetDate(Uri(s"$baseUri/prevChange"), date)
  }

  override def segmentPositionOnDate(date: LocalDate, segmentId: String): Future[Option[SegmentToM1Pos]] = {
    postGetOption(Uri(s"$baseUri/segmentPositionOnDate/$segmentId"), date.toJson.compactPrint)
  }

  override def segmentAtPositionOnDate(date: LocalDate, position: String): Future[Option[SegmentToM1Pos]] = {
    postGetOption(Uri(s"$baseUri/segmentAtPositionOnDate/$position"), date.toJson.compactPrint)
  }

  override def availableSegmentIdsForPos(position: String): Future[List[String]] = {
    async {
      val uri      = Uri(s"$baseUri/availableSegmentIdsForPos/$position")
      val request  = HttpRequest(HttpMethods.GET, uri = uri)
      val response = await(Http().singleRequest(request))
      await(Unmarshal(response).to[List[String]])
    }
  }

  override def syncWithJira(progress: Int => Unit): Future[Boolean] = {
    async {
      // XXX TODO FIXME
      false
    }
  }

  override def currentPositions(): Future[List[SegmentToM1Pos]] =
    async {
      val uri      = Uri(s"$baseUri/currentPositions")
      val request  = HttpRequest(HttpMethods.GET, uri = uri)
      val response = await(Http().singleRequest(request))
      await(Unmarshal(response).to[List[SegmentToM1Pos]])
    }

  override def plannedPositions(): Future[List[SegmentToM1Pos]] =
    async {
      val uri      = Uri(s"$baseUri/plannedPositions")
      val request  = HttpRequest(HttpMethods.GET, uri = uri)
      val response = await(Http().singleRequest(request))
      await(Unmarshal(response).to[List[SegmentToM1Pos]])
    }

  override def segmentData(): Future[List[JiraSegmentData]] =
    async {
      val uri      = Uri(s"$baseUri/segmentData")
      val request  = HttpRequest(HttpMethods.GET, uri = uri)
      val response = await(Http().singleRequest(request))
      await(Unmarshal(response).to[List[JiraSegmentData]])
    }

  override def currentSegmentPosition(segmentId: String): Future[Option[SegmentToM1Pos]] = {
    getOption(Uri(s"$baseUri/currentSegmentPosition/$segmentId"))
  }

  override def currentSegmentAtPosition(position: String): Future[Option[SegmentToM1Pos]] = {
    getOption(Uri(s"$baseUri/currentSegmentAtPosition/$position"))
  }

  override def resetJiraSegmentDataTable(): Future[Boolean] =
    async {
      val uri      = Uri(s"$baseUri/resetJiraSegmentDataTable")
      val request  = HttpRequest(HttpMethods.POST, uri = uri)
      val response = await(Http().singleRequest(request))
      response.status == OK
    }

  override def resetSegmentToM1PosTable(): Future[Boolean] =
    async {
      val uri      = Uri(s"$baseUri/resetSegmentToM1PosTable")
      val request  = HttpRequest(HttpMethods.POST, uri = uri)
      val response = await(Http().singleRequest(request))
      response.status == OK
    }

  /**
   * Drops and recreates the database tables (for testing)
   */
  def resetTables(): Future[Boolean] =
    async {
      val uri      = Uri(s"$baseUri/resetTables")
      val request  = HttpRequest(HttpMethods.POST, uri = uri)
      val response = await(Http().singleRequest(request))
      response.status == OK
    }
}
