package ingestion

import org.apache.flink.api.common.functions.MapFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.flink.streaming.api.functions.sink.SinkFunction
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import java.util.Properties

object FlinkIngestionApp {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    // Kafka configuration
    val properties = new Properties()
    properties.setProperty("group.id", "flink-consumer-group")
    properties.put("bootstrap.servers", "localhost:9092")
    properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    properties.put("fetch.message.max.bytes", "2000000000")
    // properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    // properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
    // properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)

    // Create Kafka consumer
    val kafkaConsumer = new FlinkKafkaConsumer[String]("raw-events", new SimpleStringSchema(), properties)

    // Read data from Kafka
    val stream = env.addSource(kafkaConsumer)

    // Parse JSON and filter valid data
    val transformedStream = stream
      .filter(/* Add filter criteria */)

    // Push data to another Kafka topic
    val kafkaProducer = new FlinkKafkaProducer[String](
      "ingested-events",
      new SimpleStringSchema(),
      properties
    )

    stream.addSink(kafkaProducer)  // using kafka-connector push to S3 (raw-data)
    env.execute("Flink Ingestion Job")
  }
}
