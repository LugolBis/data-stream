package producer

import cats.effect.{ExitCode, IO, IOApp, Ref}
import admin.Admin

object Main extends IOApp:
  val topicSizeLimit: Option[Double] = Some(5d)

  def run(args: List[String]): IO[ExitCode] =
    for
      ref <- Ref.of[IO, Option[String]](Some("2026-01-01T00:00:00.00Z"))
      _ <- new KafkaWr[IO](
        "new-page",
        topicSizeLimit,
        new Wikimedia[IO](ref),
        "kafka-broker:29092"
      ).run()
      size <- Admin[IO]("kafka-broker:29092").topicSizeGb("new-page")
      _ <- IO.println(f"Total size of the topic 'new-page' : $size%.8f GB")
    yield ExitCode.Success
