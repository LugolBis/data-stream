package consumer

import cats.effect.{Async, Ref}
import cats.syntax.all.*
import com.comcast.ip4s.*
import fs2.Stream
import fs2.concurrent.Topic
import fs2.kafka.CommittableConsumerRecord
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.`Content-Type`
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import org.http4s.StaticFile
import java.awt.Desktop
import java.net.URI
import io.circe.Json
import io.circe.parser.parse
import scala.io.Source

class LiveBarChart[F[_]: Async](serverPort: String) extends DtoConsumer[F]:
  def processRecords(
      stream: Stream[F, CommittableConsumerRecord[F, String, String]]
  ): F[Unit] =
    for
      stateRef <- Ref.of[F, Map[String, Int]](Map.empty)
      updateTopic <- Topic[F, Map[String, Int]]
      _ <- serverResource(stateRef, updateTopic).use { _ =>
        openBrowser >> ingestStream(stream, stateRef, updateTopic)
      }
    yield ()

  private def ingestStream(
      stream: Stream[F, CommittableConsumerRecord[F, String, String]],
      stateRef: Ref[F, Map[String, Int]],
      updateTopic: Topic[F, Map[String, Int]]
  ): F[Unit] =
    stream
      .map(_.record)
      .evalMap { record =>
        val key =
          parse(record.value).toOption
            .flatMap(_.asArray)
            .flatMap(_.headOption)
            .flatMap(_.asString)
            .flatMap(_.split('.').headOption)
            .map(_.toUpperCase)
            .getOrElse("Unknown")
        stateRef
          .updateAndGet(map => map.updated(key, map.getOrElse(key, 0) + 1))
          .flatMap(updateTopic.publish1)
          .void
      }
      .compile
      .drain

  private def serverResource(
      stateRef: Ref[F, Map[String, Int]],
      updateTopic: Topic[F, Map[String, Int]]
  ) =
    val dsl = new Http4sDsl[F] {}
    import dsl.*

    def routes(wsb: WebSocketBuilder2[F]): HttpRoutes[F] =
      HttpRoutes.of[F] {
        // Dashboard :
        case GET -> Root =>
          Ok(htmlPage).map(
            _.withContentType(`Content-Type`(MediaType.text.html))
          )

        case req @ GET -> "css" /: file =>
          StaticFile
            .fromResource(s"/css/$file", Some(req))
            .getOrElseF(NotFound())

        case req @ GET -> "js" /: file =>
          StaticFile
            .fromResource(s"/js/$file", Some(req))
            .getOrElseF(NotFound())

        // WebSocket :
        case GET -> Root / "ws" =>
          for
            snapshot <- stateRef.get
            toClient = (Stream.emit(snapshot) ++ updateTopic.subscribe(100))
              .map(state => WebSocketFrame.Text(encodeState(state)))
            response <- wsb.build(toClient, _.drain)
          yield response
      }

    EmberServerBuilder
      .default[F]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"4242")
      .withHttpWebSocketApp(wsb => routes(wsb).orNotFound)
      .build

  private def openBrowser: F[Unit] =
    Async[F].delay {
      if Desktop.isDesktopSupported &&
        Desktop.getDesktop.isSupported(Desktop.Action.BROWSE)
      then
        Desktop.getDesktop.browse(URI.create(s"http://localhost:$serverPort"))
    }

  private def encodeState(state: Map[String, Int]): String =
    val sorted = state.toList.sortBy(-_._2) // descending by value
    val labels =
      sorted.map((k, _) => "\"" + escapeJson(k) + "\"").mkString("[", ",", "]")
    val values = sorted.map(_._2).mkString("[", ",", "]")
    s"""{"labels":$labels,"values":$values}"""

  private def escapeJson(s: String): String =
    s.replace("\\", "\\\\")
      .replace("\"", "\\\"")
      .replace("\n", "\\n")
      .replace("\r", "\\r")
      .replace("\t", "\\t")

  private val htmlPage: String = Source.fromResource("index.html").mkString
