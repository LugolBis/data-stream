package producer

import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.generic.semiauto.deriveEncoder
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

trait Dto:
  def getKey(): String
  def getValue(): Dto
  def toJson: Json

case class Terminal[T](value: T) extends Dto:
  def getKey(): String = value.toString()
  def getValue(): Dto = this
  def toJson: Json = Json.fromString(value.toString())

extension (t: (String, String))
  def toDto(): Dto = new Dto:
    def getKey(): String = t._1
    def getValue(): Dto = Terminal(t._2)
    def toJson: Json =
      Json.fromValues(List(Json.fromString(t._1), Json.fromString(t._2)))

case class MetaData(uri: String, domain: String, dt: String) extends Dto:
  def getKey(): String = uri
  def getValue(): Dto = (domain, dt).toDto()
  def toJson: Json =
    Json.fromFields(
      Map(
        "uri" -> Json.fromString(uri),
        "domain" -> Json.fromString(domain),
        "dt" -> Json.fromString(dt)
      )
    )

object MetaData:
  def fromJson(json: Json): Option[MetaData] =
    for
      uri <- json.hcursor.downField("meta").downField("uri").as[String].toOption
      domain <- json.hcursor
        .downField("meta")
        .downField("domain")
        .as[String]
        .toOption
      dt <- json.hcursor.downField("meta").downField("dt").as[String].toOption
    yield MetaData(uri.decodeURL(), domain, dt)

extension (str: String)
  def decodeURL(): String =
    URLDecoder.decode(str, StandardCharsets.UTF_8)
