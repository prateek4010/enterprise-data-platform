import io.circe.generic.auto._
import io.circe.parser._
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.{PutItemRequest, AttributeValue}
import java.util.Properties

case class Location(country: String, city: String, ipAddress: String)
case class Item(itemId: String, itemName: String, quantity: Int, price: Double)

// Case class for eventDetails
case class EventDetails(
    customerId: String,
    transactionId: Option[String],  // Optional because not all events have transactionId
    amount: Option[Double],         // Optional because not all events have amount
    currency: Option[String],       // Optional because not all events have currency
    paymentMethod: Option[String],  // Optional because not all events have paymentMethod
    location: Option[Location],     // Optional because not all events have location
    items: Option[List[Item]],      // Optional because not all events have items
    email: Option[String],          // For account_creation events
    phone: Option[String],          // For account_creation events
    accountCreationDate: Option[String],  // For account_creation events
    referralCode: Option[String],   // For account_creation events
    loginTime: Option[String],      // For login events
    deviceType: Option[String],     // For login events
    loginSuccess: Option[Boolean],  // For login events
    oldPasswordHash: Option[String],// For password_change events
    newPasswordHash: Option[String],// For password_change events
    previousMethod: Option[String], // For payment_method_update events
    updateTime: Option[String],     // For payment_method_update and profile_update events
    orderId: Option[String],        // For order_cancellation and refund events
    cancellationReason: Option[String], // For order_cancellation events
    cancellationTime: Option[String], // For order_cancellation events
    refundId: Option[String],       // For refund events
    refundAmount: Option[Double],   // For refund events
    refundReason: Option[String],   // For refund events
    updatedFields: Option[List[String]],  // For profile_update events
    previousValues: Option[Map[String, String]],  // For profile_update events
    newValues: Option[Map[String, String]],  // For profile_update events
    alertId: Option[String],        // For fraud_alert events
    alertType: Option[String],      // For fraud_alert events
    alertTime: Option[String],      // For fraud_alert events
    fraudDetails: Option[String],       // For fraud_alert events
    cartId: Option[String],         // For cart_abandonment events
    abandonmentTime: Option[String] // For cart_abandonment events
)

case class RiskAssessment(fraudScore: Option[Double], isHighRisk: Option[Boolean], riskFactors: Option[List[String]])
case class Metadata(source: String, campaign: Option[String])
case class Event(
    customerId: String,
    eventId: String,
    eventType: String,
    timestamp: String,
    eventDetails: EventDetails,
    riskAssessment: Option[RiskAssessment], // Optional because not all events have riskAssessment
    metadata: Metadata
)

object StorageLayerApp {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    env.enableCheckpointing(300000) // 5 minutes

    // Configure advanced options for checkpoints
    val checkpointConfig = env.getCheckpointConfig
    checkpointConfig.setCheckpointTimeout(60000) // 60 seconds
    checkpointConfig.setMinPauseBetweenCheckpoints(50000) // Minimum pause between checkpoints
    checkpointConfig.setMaxConcurrentCheckpoints(1) // Allow only one checkpoint at a time

    // Kafka consumer properties
    val kafkaProps = new Properties()
    kafkaProps.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    kafkaProps.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "flink-storage-group")

    // Create a Kafka consumer for the 'processed-events' topic
    val kafkaConsumer = new FlinkKafkaConsumer[String]("processed-events", new SimpleStringSchema(), kafkaProps)

    // DynamoDB client
    val dynamoDbClient = DynamoDbClient.builder().build()

    // Read the Kafka stream
    val stream = env.addSource(kafkaConsumer)

    // Process the stream, extract event details, and push to DynamoDB
    stream.map(jsonString => {
      decode[Event](jsonString) match {
        case Right(event) => saveToDynamoDB(event, dynamoDbClient)
        case Left(error) => println(s"Failed to parse JSON: $error")
      }
    })

    env.execute("Flink Storage Layer")
  }

  // Function to save event to DynamoDB
  def saveToDynamoDB(event: Event, dynamoDbClient: DynamoDbClient): Unit = {
    val updateItemRequest = UpdateItemRequest.builder()
        .tableName("CustomersTable")
        .key(Map(
            "customerId" -> AttributeValue.builder().s(event.customerId).build()
        ).asJava)
        .updateExpression("SET customerId = :customerId, eventType = :eventType, eventDetails = :eventDetails, riskAssessment = :riskAssessment, #ts = :timestamp")
        .expressionAttributeValues(Map(
            "customerId" -> AttributeValue.builder().s(event.customerId).build(),
            "eventType" -> AttributeValue.builder().s(event.eventType).build(),
            "eventDetails" -> AttributeValue.builder().s(event.eventDetails).build(),
            "riskAssessment" -> AttributeValue.builder().s(event.riskAssessment).build(),
            "timestamp" -> AttributeValue.builder().s(event.timestamp).build()
        ).asJava)
        .expressionAttributeNames(Map("#ts" -> "timestamp").asJava)  // If "timestamp" is a reserved keyword, use #ts as an alias
        .build()

    dynamoDbClient.updateItem(updateItemRequest)
    println(s"Saved event ${event.customerId} to DynamoDB")
  }
}
