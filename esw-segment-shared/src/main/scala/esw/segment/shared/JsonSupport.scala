package esw.segment.shared

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import esw.segment.shared.EswSegmentData._
import spray.json._

import java.time.LocalDate

//noinspection TypeAnnotation
trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit object DateFormat extends RootJsonFormat[LocalDate] {
    def write(obj: LocalDate): JsString = JsString(obj.toString)

    def read(json: JsValue): LocalDate = json match {
      case JsString(date) => LocalDate.parse(date)
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
