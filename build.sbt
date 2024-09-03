// Define common settings and dependencies
lazy val commonSettings = Seq(
  scalaVersion := "2.12.17",
  libraryDependencies ++= Seq(
    "com.typesafe" % "config" % "1.4.1",
    "org.apache.kafka" %% "kafka" % "3.0.0",
    "org.apache.kafka" % "kafka-clients" % "3.3.1",
    "org.apache.flink" %% "flink-streaming-scala" % "1.14.0",
    "org.apache.flink" %% "flink-connector-kafka" % "1.14.0",
    "io.circe" %% "circe-core" % "0.14.5",
    "io.circe" %% "circe-generic" % "0.14.5",
    "io.circe" %% "circe-parser" % "0.14.5"
  )
)

// Common module
lazy val common = (project in file("common"))
  .settings(
    name := "common"
  )
  .settings(commonSettings: _*)

// Kafka Producer module
lazy val kafkaProducer = (project in file("kafka-producer"))
  .settings(
    name := "kafka-producer",
    mainClass in Compile := Some("KafkaProducerApp")
  )
  .settings(commonSettings: _*)

// Ingestion module
lazy val ingestion = (project in file("ingestion"))
  .settings(
    name := "ingestion",
    mainClass in Compile := Some("FlinkIngestionApp")
  )
  .settings(commonSettings: _*)

// Processing module
lazy val transformation = (project in file("transformation"))
  .dependsOn(common)
  .settings(
    name := "transformation",
    mainClass in Compile := Some("FlinkProcessingApp")
  )
  .settings(commonSettings: _*)

// Storage module
lazy val storage = (project in file("storage"))
  .dependsOn(common)
  .settings(
    name := "storage",
    mainClass in Compile := Some("FlinkStorageApp")
  )
  .settings(commonSettings: _*)

// Serving module
lazy val serving = (project in file("serving"))
  .dependsOn(common)
  .settings(
    name := "serving",
    mainClass in Compile := Some("FlinkServingApp")
  )
  .settings(commonSettings: _*)
