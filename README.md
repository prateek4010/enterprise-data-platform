# enterprise-data-platform

This project is a real-time data platform built using **Apache Flink**, **Kafka**, and **AWS**. It handles ingestion, processing, storage, and serving of streaming data, with a focus on fault tolerance and quality checks.

## Table of Contents
- [Project Structure](#project-structure)
- [Modules Overview](#modules-overview)
- [Makefile](#Makefile)
- [Conclusion](#Conclusion)

---

## Project Structure

├── common/src

├── kafka-producer

├── ingestion/src

├── processing/src

├── storage/src

├── serving/src

├── scripts

├── docker

├── docker-compose.yml

├── build.sbt

├── Makefile



## Modules Overview

### Common
Contains shared utilities and constants used by other modules.

### Kafka Producer
Handles the production of events into Kafka from a JSON source.

### Ingestion
Consumes raw events from Kafka, applies transformations, and forwards them for processing.

### Processing
Processes events (keyed by customer ID) from Kafka and sends them to the storage layer.

### Storage
Reads processed events and persists them in **AWS DynamoDB**.

### Serving
Requests an external Decision API with the event data and stored records from DynamoDB.

---

## How to run

You can run commands from Makefile to build and run your code. Few examples -
```
make build
make start-all
make build-ingestion
make clean
make run-transformation-sbt
```

## Conclusion
This README provides a basic structure for getting started with your real-time data platform project. It includes Docker setup, makefile commands, and an overview of the different modules and their roles in the pipeline. Let me know if you'd like to customize it further.
