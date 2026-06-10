package consumer

import cats.effect.Async
import fs2.kafka.CommittableConsumerRecord
import fs2.Stream
import fs2.io.stdout

class Console[F[_]: Async] extends DtoConsumer[F]:
  def processRecords(
      stream: Stream[F, CommittableConsumerRecord[F, String, String]]
  ): F[Unit] =
    stream
      .map(_.record)
      .map(record =>
        println(s"[${record.headers}] -> ${record.key} -> ${record.value}")
      )
      .compile
      .drain
