import java.net.URI
import org.apache.kafka.clients.consumer.{KafkaConsumer, ConsumerConfig, ConsumerRecords}
import java.time.Duration
import java.util.{Collections, Properties}
import software.amazon.awssdk.services.dynamodb.model.{AttributeValue, GetItemRequest}
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import sttp.client3._
import scala.jdk.CollectionConverters._

case class Event(
    customerId: String,
    eventId: String,
    eventType: String,
    timestamp: String,
    eventDetails: EventDetails,
    riskAssessment: Option[RiskAssessment],
    metadata: Metadata
)

object ServingApp {
  def main(args: Array[String]): Unit = {
    // Kafka Consumer configuration
    val consumerProps = new Properties()
    consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "serving-group")
    consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")

    val consumer = new KafkaConsumer[String, String](consumerProps)
    consumer.subscribe(Collections.singletonList("processed-events"))

    // DynamoDB client
    val dynamoDbClient = DynamoDbClient.builder().build()

    // HTTP client for Decision Service API
    val backend = HttpURLConnectionBackend()

    while (true) {
      val records: ConsumerRecords[String, String] = consumer.poll(Duration.ofMillis(100))
      records.forEach { record =>
        val customerId = record.key()
        val eventJson = record.value()

        // Fetch data from DynamoDB using customerId (assuming it's stored in DynamoDB)
        val customerData = fetchFromDynamoDB(customerId, dynamoDbClient)

        // Call external Decision Service API with event data
        val response = callExternalAPI(eventJson, customerData)

        println(s"API Response for customer $customerId: $response")
      }
    }
  }

  def fetchFromDynamoDB(customerId: String, dynamoDbClient: DynamoDbClient): Map[String, String] = {
    val request = GetItemRequest.builder()
      .tableName("CustomersTable")
      .key(Map("customerId" -> AttributeValue.builder().s(customerId).build()).asJava)
      .build()

    val result = dynamoDbClient.getItem(request)
    if (result.hasItem) {
      result.item().asScala.toMap.map { case (k, v) => k -> v.s() }
    } else {
      Map.empty[String, String]
    }
  }

  def callExternalAPI(eventJson: String, dynamoData: Map[String, String]): String = {
    // Decision Service API URL
    val decisionServiceUrl = "https://decisionservice.api/endpoint"

    // Combine event data and DynamoDB data into a single JSON payload
    val requestPayload = s"""{
      "event": $eventJson,
      "dynamoData": ${dynamoData.map { case (k, v) => s""""$k": "$v"""" }.mkString(",")}
    }"""

    // Make HTTP POST request to Decision Service API
    val request = basicRequest
      .body(requestPayload)
      .post(uri"$decisionServiceUrl")
      .contentType("application/json")

    val response = request.send(HttpURLConnectionBackend())
    response.body match {
      case Right(successResponse) => successResponse
      case Left(errorResponse) => errorResponse
    }
  }
}
