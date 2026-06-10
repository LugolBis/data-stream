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

class KafkaWr[F[_]: Async: Parallel](
    topic: String,
    dtoProducer: DtoProducer[F],
    bootstrapServers: String
):

  private val valueSerializer: Serializer[F, Dto] =
    Serializer[F, String].contramap(_.toJson.toString)

  private val producerSettings: ProducerSettings[F, String, Dto] =
    ProducerSettings[F, String, Dto](
      keySerializer = Serializer[F, String],
      valueSerializer = valueSerializer
    ).withBootstrapServers(bootstrapServers)

  def run(): F[Unit] =
    KafkaProducer
      .stream(producerSettings)
      .flatMap(producer =>
        dtoProducer
          .eventStream()
          .map(dto => ProducerRecord(topic, dto.getKey(), dto.getValue()))
          .evalMap(record =>
            producer
              .produce(ProducerRecords.one(record))
              .flatten
              .void
              .handleErrorWith(e =>
                Async[F]
                  .delay(println(s"[KafkaWr] Error produced : ${e.getMessage}"))
              )
          )
      )
      .compile
      .drain
      .handleErrorWith(e =>
        Async[F].delay(println(s"[KafkaWr] Stream failed : ${e.getMessage}"))
          *> Async[F].raiseError(e)
      )
