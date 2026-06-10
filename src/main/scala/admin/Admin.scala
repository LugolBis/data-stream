package admin

import cats.effect.{Async, Resource}
import org.apache.kafka.clients.admin.{AdminClient, AdminClientConfig}

import java.util.Properties
import scala.jdk.CollectionConverters._

class Admin[F[_]: Async](bootstrapServers: String):
  private val adminClientResource: Resource[F, AdminClient] =
    val props = new Properties()
    props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    Resource.make(
      Async[F].delay(AdminClient.create(props))
    )(client => Async[F].delay(client.close()))

  def topicSizeGb(topic: String): F[Double] =
    adminClientResource.use { client =>
      Async[F].blocking {
        client
          .describeLogDirs(
            client
              .describeCluster()
              .nodes()
              .get()
              .asScala
              .map(n => Integer.valueOf(n.id()))
              .toSeq
              .asJava
          )
          .allDescriptions()
          .get()
          .asScala
          .values
          .flatMap(_.asScala.values)
          .flatMap(_.replicaInfos().asScala)
          .filter { case (tp, _) => tp.topic() == topic }
          .toMap
          .values
          .map(_.size())
          .sum
          .toDouble / math.pow(1024, 3)
      }
    }
