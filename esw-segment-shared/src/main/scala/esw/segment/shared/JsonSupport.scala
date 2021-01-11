package esw.segment.shared

import java.sql.Date

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import esw.segment.shared.EswSegmentData._
import spray.json._

//noinspection TypeAnnotation
trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit object DateFormat extends RootJsonFormat[Date] {
    def write(obj: Date): JsNumber = JsNumber(obj.getTime)

    def read(json: JsValue): Date = json match {
      case JsNumber(time) => new Date(time.toLong)
      case _ => throw DeserializationException("Date expected")
    }
  }

  implicit val dateRangeFormat = jsonFormat2(DateRange)
  implicit val segmentToM1PosFormat = jsonFormat3(SegmentToM1Pos)
  implicit val allPositionsFormat = jsonFormat2(AllSegmentPositions)
  implicit val jiraSegmentDataFormat = jsonFormat16(JiraSegmentData)
  implicit val segmentConfigFormat = jsonFormat2(SegmentConfig)
  implicit val mirrorConfigFormat = jsonFormat2(MirrorConfig)
}
