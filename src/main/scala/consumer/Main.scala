package consumer

import cats.effect.{Async, ExitCode, IO, IOApp, Ref}
import cats.syntax.all._
import org.http4s.{Header, Headers, Method, Request}
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.implicits._
import fs2.Stream
import fs2.io.net.Network
import fs2.io.stdout
import fs2.text.utf8
import io.circe.Json
import org.typelevel.ci.CIString
import org.typelevel.jawn.Facade
import org.typelevel.jawn.fs2._
import io.circe.generic.auto._

class Wikimedia[F[_]: Async: Network](lastTimestamp: Ref[F, Option[String]]):
  implicit val facade: Facade[Json] =
    io.circe.jawn.CirceSupportParser(None, false).facade

  private def extractTimestamp(json: Json): Option[String] =
    json.hcursor.downField("meta").downField("dt").as[String].toOption

  private def isCanary(json: Json): Boolean =
    json.hcursor
      .downField("meta")
      .downField("domain")
      .as[String]
      .toOption
      .contains("canary")

  private def buildRequest(since: Option[String]): Request[F] =
    val base = uri"https://stream.wikimedia.org/v2/stream/mediawiki.page-create"
    val uri = since match
      case Some(ts) => base.withQueryParam("since", ts)
      case None     => base

    Request[F](
      Method.GET,
      uri,
      headers = Headers(
        Header.Raw(CIString("Accept"), "application/json"),
        Header.Raw(CIString("User-Agent"), "WikimediaScalaKafkaProducer/1.0")
      )
    )

  private def jsonStream(req: Request[F]): Stream[F, Json] =
    for
      client <- Stream.resource(EmberClientBuilder.default[F].build)
      json <- client.stream(req).flatMap(_.body.chunks.parseJsonStream)
    yield json

  def eventStream(): Stream[F, Json] =
    Stream
      .eval(lastTimestamp.get)
      .flatMap(since =>
        jsonStream(buildRequest(since))
          .filterNot(isCanary)
          .evalTap { json =>
            extractTimestamp(json).traverse_(ts => lastTimestamp.set(Some(ts)))
          }
      )

  def run(): F[Unit] =
    eventStream()
      .map(_.spaces2 + "\n")
      .through(utf8.encode)
      .through(stdout)
      .compile
      .drain

object Main extends IOApp:
  def run(args: List[String]): IO[ExitCode] =
    for
      ref <- Ref.of[IO, Option[String]](None)
      _ <- new Wikimedia[IO](ref).run()
    yield ExitCode.Success
