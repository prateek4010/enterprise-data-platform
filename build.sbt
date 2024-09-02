// Define common settings and dependencies
lazy val commonSettings = Seq(
  scalaVersion := "2.12.15",
  libraryDependencies ++= Seq(
    "com.typesafe" % "config" % "1.4.1"
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
    libraryDependencies ++= Seq(
      "org.apache.kafka" %% "kafka" % "3.0.0",
      "io.circe" %% "circe-core" % "0.14.5",
      "io.circe" %% "circe-generic" % "0.14.5",
      "io.circe" %% "circe-parser" % "0.14.5"
    )
  )

// Ingestion module
lazy val ingestion = (project in file("ingestion"))
  .dependsOn(common)
  .settings(
    name := "ingestion",
    libraryDependencies ++= Seq(
      "org.apache.flink" %% "flink-streaming-scala" % "1.16.0",
      "org.apache.flink" %% "flink-connector-kafka" % "1.16.0",
      "org.apache.flink" %% "flink-json" % "1.16.0",
      "io.circe" %% "circe-core" % "0.14.6",
      "io.circe" %% "circe-generic" % "0.14.6",
      "io.circe" %% "circe-parser" % "0.14.6"
    )
  )
  .settings(commonSettings: _*)

// Processing module
lazy val processing = (project in file("processing"))
  .dependsOn(common)
  .settings(
    name := "processing",
    libraryDependencies ++= Seq(
      "org.apache.flink" %% "flink-streaming-scala" % "1.15.0",
      "org.apache.flink" %% "flink-connector-kafka" % "1.15.0"
    )
  )
  .settings(commonSettings: _*)

// Storage module
lazy val storage = (project in file("storage"))
  .dependsOn(common)
  .settings(
    name := "storage",
    libraryDependencies ++= Seq(
      "org.apache.flink" %% "flink-streaming-scala" % "1.15.0",
      "org.apache.flink" %% "flink-connector-jdbc" % "1.15.0"
    )
  )
  .settings(commonSettings: _*)

// Serving module
lazy val serving = (project in file("serving"))
  .dependsOn(common)
  .settings(
    name := "serving",
    libraryDependencies ++= Seq(
      "org.apache.flink" %% "flink-streaming-scala" % "1.15.0",
      "org.apache.flink" %% "flink-connector-kafka" % "1.15.0",
      "org.apache.httpcomponents" % "httpclient" % "4.5.13"
    )
  )
  .settings(commonSettings: _*)

// Aggregating all projects
lazy val root = (project in file("."))
  .aggregate(kafkaProducer, ingestion, processing, storage, serving)
