package producer

import cats.effect.{ExitCode, IO, IOApp, Ref}

object Main extends IOApp:
  val maxRecords = 10

  def run(args: List[String]): IO[ExitCode] =
    for
      ref <- Ref.of[IO, Option[String]](Some("2026-01-01T00:00:00.00Z"))
      _ <- new Wikimedia[IO](ref, Main.maxRecords).run()
    yield ExitCode.Success
