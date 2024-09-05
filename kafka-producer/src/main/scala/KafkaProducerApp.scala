// kafka-producer/src/main/scala/KafkaProducerApp.scala

import java.util.Properties
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord, Callback, RecordMetadata}
import org.apache.kafka.common.serialization.StringSerializer
import scala.io.Source
import scala.util.{Failure, Success, Try}
import io.circe._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._


object KafkaProducerApp {

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
    eventId: String,
    eventType: String,
    timestamp: String,
    eventDetails: EventDetails,
    riskAssessment: Option[RiskAssessment], // Optional because not all events have riskAssessment
    metadata: Metadata
  )

  object EventJsonProtocol {
    implicit val locationDecoder: Decoder[Location] = deriveDecoder
    implicit val itemDecoder: Decoder[Item] = deriveDecoder
    implicit val eventDetailsDecoder: Decoder[EventDetails] = deriveDecoder
    implicit val riskAssessmentDecoder: Decoder[RiskAssessment] = deriveDecoder
    implicit val metadataDecoder: Decoder[Metadata] = deriveDecoder
    implicit val eventDecoder: Decoder[Event] = deriveDecoder
    implicit val locationEncoder: Encoder[Location] = deriveEncoder
    implicit val itemEncoder: Encoder[Item] = deriveEncoder
    implicit val eventDetailsEncoder: Encoder[EventDetails] = deriveEncoder
    implicit val riskAssessmentEncoder: Encoder[RiskAssessment] = deriveEncoder
    implicit val metadataEncoder: Encoder[Metadata] = deriveEncoder
    implicit val eventEncoder: Encoder[Event] = deriveEncoder
  }

  import KafkaProducerApp.EventJsonProtocol._

  def main(args: Array[String]): Unit = {
    // Configure Kafka Producer
    val props = new Properties()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
    props.put("fetch.message.max.bytes", "2000000000")
    props.put("linger.ms", "0") 
    props.put("acks", "all") 
    
    val producer = new KafkaProducer[String, String](props)

    // Load JSON file from classpath
    val jsonString = Try {
      val source = Source.fromResource("events1.json")
      try source.getLines().mkString finally source.close()
    } match {
      case Success(content) => content
      case Failure(exception) =>
        println(s"Error reading JSON file: ${exception.getMessage}")
        return
    }

    // Parse JSON and produce to Kafka
    parse(jsonString) match {
      case Right(json) =>
        json.as[List[Event]] match {
          case Right(events) =>
            events.foreach { event =>
              println("============")
              println(event)
              val record = new ProducerRecord[String, String]("raw-events", 0, event.eventId, event.asJson.noSpaces)
              println("++|++Record++|+")
              producer.send(record, (metadata, exception) => {
                  if (exception != null) {
                    println(s"Send failed: ${exception.getMessage}")
                  } else {
                    println(s"Sent record to topic ${metadata.topic()} partition ${metadata.partition()} with offset ${metadata.offset()}")
                  }
                }
              )
              println(s"${record}")
              println("+++++++++\n\n")
            }
            println(s"Produced ${events.size} records")
          case Left(error) =>
            print(jsonString)
            println(s"Failed to decode JSON to Event list: $error")
        }
      case Left(error) =>
        println(s"Failed to parse JSON: $error")
    }
    
    producer.close()
  }
}
