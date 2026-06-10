package producer

import admin.Admin
import cats.effect.Async
import cats.Parallel
import fs2.io.net.Network
import fs2.Stream
import cats.effect._
import cats.syntax.all._
import io.circe.syntax._
import fs2.kafka._
import fs2.kafka.consumer.KafkaConsumeChunk.CommitNow
import scala.concurrent.duration.DurationInt

trait DtoProducer[F[_]: Async]:
  def eventStream(): Stream[F, Dto]

class KafkaWr[F[_]: Async: Parallel](
    topic: String,
    topicSizeLimit: Option[Double],
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
        val stream = dtoProducer
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

        topicSizeLimit match
          case None      => stream
          case Some(lim) =>
            stream.interruptWhen(
              Stream
                .fixedDelay[F](2.seconds)
                .evalMap(_ =>
                  Admin[F](bootstrapServers).topicSizeGb(topic).map(_ >= lim)
                )
            )
      )
      .compile
      .drain
      .handleErrorWith(e =>
        Async[F].delay(println(s"[KafkaWr] Stream failed : ${e.getMessage}"))
          *> Async[F].raiseError(e)
      )
