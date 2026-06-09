package producer

import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.generic.semiauto.deriveEncoder
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

trait Dto:
  def getKey(): String
  def getValue(): Dto

case class Terminal[T](value: T) extends Dto:
  def getKey(): String = value.toString()
  def getValue(): Dto = this

extension (t: (String, String))
  def toDto(): Dto = new Dto:
    def getKey(): String = t._1
    def getValue(): Dto = Terminal(t._2)

case class MetaData(uri: String, domain: String, dt: String) extends Dto:
  def getKey(): String = uri
  def getValue(): Dto = (domain, dt).toDto()

object MetaData:
  implicit val encoder: Encoder[MetaData] = deriveEncoder[MetaData]
  implicit val decoder: Decoder[MetaData] =
    Decoder.instance[MetaData] { c =>
      for {
        uri <- c.downField("meta").downField("uri").as[String]
        domain <- c.downField("meta").downField("domain").as[String]
        dt <- c.downField("meta").downField("dt").as[String]
      } yield new MetaData(uri.decodeURL(), domain, dt)
    }

extension (str: String)
  def decodeURL(): String =
    URLDecoder.decode(str, StandardCharsets.UTF_8)
