package producer

import cats.effect.Async
import cats.Parallel
import fs2.io.net.Network
import fs2.Stream
import cats.effect._
import cats.syntax.all._
import io.circe.syntax._
import fs2.kafka._
import fs2.kafka.consumer.KafkaConsumeChunk.CommitNow

trait DtoProducer[F[_]: Async]:
  def eventStream(): Stream[F, Dto]
