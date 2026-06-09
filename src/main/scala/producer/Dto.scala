package producer

import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.generic.semiauto.deriveEncoder
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

trait Dto:
  def getKey(): String
  def getValue(): Dto

extension (t: (String, String))
  def toDto(): Dto = new Dto:
    def getKey(): String = t._1
    def getValue(): Dto = t.toDto()

case class MetaData(uri: String, domain: String, dt: String) extends Dto:
  implicit val encoder: Encoder[MetaData] = deriveEncoder[MetaData]
  implicit val decoder: Decoder[MetaData] =
    Decoder.instance[MetaData] { c =>
      for {
        uri <- c.downField("meta").downField("uri").as[String]
        domain <- c.downField("meta").downField("domain").as[String]
        dt <- c.downField("meta").downField("dt").as[String]
      } yield new MetaData(uri.decodeURL(), domain, dt)
    }

  def getKey(): String = uri
  def getValue(): Dto = (domain, dt).toDto()

extension (str: String)
  def decodeURL(): String =
    URLDecoder.decode(str, StandardCharsets.UTF_8)
