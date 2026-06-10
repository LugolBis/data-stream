package consumer

import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp:
  def run(args: List[String]): IO[ExitCode] =
    val bootstrapServers = sys.env.getOrElse(
      "KAFKA_BOOTSTRAP_SERVERS",
      "kafka-broker:29092"
    )

    for _ <- new KafkaRdr[IO](
        "new-page",
        new Console[IO],
        bootstrapServers
      ).run()
    yield ExitCode.Success
