package consumer

import cats.effect.Async
import cats.Parallel
import fs2.io.net.Network
import fs2.Stream
import cats.effect._
import cats.syntax.all._
import io.circe.syntax._
import fs2.kafka._
import fs2.kafka.consumer.KafkaConsumeChunk.CommitNow

trait DtoConsumer[F[_]: Async]:
  def processRecords(
      stream: Stream[F, CommittableConsumerRecord[F, String, String]]
  ): F[Unit]

class KafkaRdr[F[_]: Async: Parallel](
    topic: String,
    dtoConsumer: DtoConsumer[F],
    bootstrapServers: String
):
  private val consumerSettings: ConsumerSettings[F, String, String] =
    ConsumerSettings(
      keyDeserializer = Deserializer[F, String],
      valueDeserializer = Deserializer[F, String]
    ).withAutoOffsetReset(AutoOffsetReset.Earliest)
      .withBootstrapServers(bootstrapServers)
      .withGroupId("group")

  def run(): F[Unit] =
    dtoConsumer.processRecords(
      KafkaConsumer
        .stream(consumerSettings)
        .subscribeTo(topic)
        .records
    )
