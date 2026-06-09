package producer

import io.circe.{Decoder, HCursor, Json}
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

case class MetaData(uri: String, domain: String, dt: String)
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
