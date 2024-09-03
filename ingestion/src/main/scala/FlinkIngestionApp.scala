import org.apache.flink.api.common.functions.MapFunction
import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer, FlinkKafkaProducer}
import org.apache.flink.streaming.api.functions.sink.SinkFunction
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import java.util.Properties

object FlinkIngestionApp {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    // Configure a failure rate restart strategy with a maximum failure rate of 5 per hour and a delay of 30 seconds between restarts
    env.setRestartStrategy(RestartStrategies.failureRateRestart(
      5,                  // Maximum failure rate (failures per hour)
      org.apache.flink.api.common.time.Time.of(1, java.util.concurrent.TimeUnit.HOURS),  // Time window for the failure rate
      org.apache.flink.api.common.time.Time.of(30, java.util.concurrent.TimeUnit.SECONDS)  // Delay between restarts
    ))

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
    println("+++++Record++++")
    stream.print()

    // Parse JSON and filter valid data
    val transformedStream = stream
      // .filter(/* Add filter criteria */)

    // Push data to another Kafka topic
    val kafkaProducer = new FlinkKafkaProducer[String](
      "ingested-events",
      new SimpleStringSchema(),
      properties
    )

    transformedStream.addSink(kafkaProducer)  // using kafka-connector push to S3 (raw-data)
    env.execute("Flink Ingestion Job")
  }
}
