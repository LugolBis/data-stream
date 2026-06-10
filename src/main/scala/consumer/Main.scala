package consumer

import cats.effect.{ExitCode, IO, IOApp}
import _root_.consumer.consumer.LiveBarChart

object Main extends IOApp:
  def run(args: List[String]): IO[ExitCode] =
    val bootstrapServers = sys.env.getOrElse(
      "KAFKA_BOOTSTRAP_SERVERS",
      "kafka-broker:29092"
    )

    for _ <- new KafkaRdr[IO](
        "new-page",
        new LiveBarChart("4242"),
        bootstrapServers
      ).run()
    yield ExitCode.Success
