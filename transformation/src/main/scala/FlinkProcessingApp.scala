import org.apache.flink.api.common.functions.MapFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.flink.streaming.api.functions.sink.SinkFunction
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import java.util.Properties

object FlinkProcessingApp {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    env.enableCheckpointing(300000) // 5 minutes

    // Configure advanced options for checkpoints
    val checkpointConfig = env.getCheckpointConfig
    checkpointConfig.setCheckpointTimeout(60000) // 60 seconds
    checkpointConfig.setMinPauseBetweenCheckpoints(50000) // Minimum pause between checkpoints
    checkpointConfig.setMaxConcurrentCheckpoints(1) // Allow only one checkpoint at a time

    // Kafka configuration
    val properties = new Properties()
    properties.setProperty("group.id", "flink-consumer-group")
    properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
    properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)

    // Create Kafka consumer
    val kafkaConsumer = new FlinkKafkaConsumer[String]("ingested-events", new SimpleStringSchema(), properties)

    // Read data from Kafka
    val stream = env.addSource(kafkaConsumer)

    val processedStream = inputStream
      .map { eventString =>
        // Parse the JSON string into an Event case class
        decode[Event](eventString) match {
          case Right(event) =>
            // Convert the event to a tuple of (customer_id, event)
            (event.eventDetails.customerId, event)
          case Left(error) =>
            throw new RuntimeException(s"Failed to parse event: $error")
        }
      }
      .map { case (customerId, event) =>
        // Convert the event back to a JSON string with customer_id as the key
        new ProducerRecord[String, String]("processed-events", event.eventDetails.customerId, event.asJson.noSpaces)
      }

    // Push data to another Kafka topic
    val kafkaProducer = new FlinkKafkaProducer[String](
      "processed-events",
      new SimpleStringSchema(),
      properties
    )

    processedStream.addSink(kafkaProducer)  // using kafka-connector push to S3
    env.execute("Flink Ingestion Job")
  }
}
