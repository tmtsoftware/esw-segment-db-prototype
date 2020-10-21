package esw.segment.db

import java.sql.Timestamp

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, DeserializationException, JsNumber, JsValue, JsonFormat}

//noinspection TypeAnnotation
trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit object TimestampFormat extends JsonFormat[Timestamp] {
    def write(obj: Timestamp) = JsNumber(obj.getTime)

    def read(json: JsValue) = json match {
      case JsNumber(time) => new Timestamp(time.toLong)
      case _ => throw new DeserializationException("Date expected")
    }
  }

  implicit val segmentToM1PosFormat = jsonFormat3(SegmentToM1Pos)
}
