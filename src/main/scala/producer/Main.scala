package producer

import cats.effect.{ExitCode, IO, IOApp, Ref}
import admin.Admin

object Main extends IOApp:
  val topicSizeLimit: Option[Double] =
    sys.env.get("PRODUCER_TOPIC_SIZE_LIMIT").flatMap(_.toDoubleOption)

  val startTimestamp: String = sys.env.getOrElse(
    "PRODUCER_TOPIC_START_TIMESTAMP",
    "2026-01-01T00:00:00.00Z"
  )

  val bootstrapServers = sys.env.getOrElse(
    "KAFKA_BOOTSTRAP_SERVERS",
    "kafka-broker:29092"
  )

  def run(args: List[String]): IO[ExitCode] =
    for
      ref <- Ref.of[IO, Option[String]](Some(startTimestamp))
      _ <- new KafkaWr[IO](
        "new-page",
        topicSizeLimit,
        new Wikimedia[IO](ref),
        bootstrapServers
      ).run()
      size <- Admin[IO](bootstrapServers).topicSizeGb("new-page")
      _ <- IO.println(f"Total size of the topic 'new-page' : $size%.8f GB")
    yield ExitCode.Success
