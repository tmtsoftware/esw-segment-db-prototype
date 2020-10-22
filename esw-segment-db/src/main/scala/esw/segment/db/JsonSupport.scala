package esw.segment.db

import java.sql.Date

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import esw.segment.db.SegmentToM1PosTable.SegmentToM1Pos
import spray.json.{DefaultJsonProtocol, DeserializationException, JsNumber, JsValue, JsonFormat}


//noinspection TypeAnnotation
trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit object DateFormat extends JsonFormat[Date] {
    def write(obj: Date) = JsNumber(obj.getTime)

    def read(json: JsValue) = json match {
      case JsNumber(time) => new Date(time.toLong)
      case _ => throw DeserializationException("Date expected")
    }
  }

  implicit val segmentToM1PosFormat = jsonFormat3(SegmentToM1Pos)
}
