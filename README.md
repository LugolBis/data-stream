# data-stream
This project demonstrates the implementation of a real-time data processing pipeline built with **Scala** and **Kafka**. The application consumes data from a **streaming API**, transforms the incoming events, publishes them to an Apache Kafka cluster, and exposes the processed data through a live dashboard.

The entire platform is containerized using **Docker**, enabling easy deployment, reproducibility, and local development.

## Architecture

```mermaid
flowchart TB
  subgraph API [Stream API]
    APIServer@{ shape: h-cyl, label: "API Server" }
  end

  subgraph KafkaLayer [Kafka CLuster]
    KafkaBroker@{ shape: h-cyl, label: "Kafka broker" }
  end

  subgraph ScalaLayer [Scala programs]
    Producer
    Consumer
  end

  subgraph MonitoringLayer [Monitoring & BI]
    KafkaUI@{ shape: curv-trap, label: "Kafka UI" }
    Dashboard@{ shape: curv-trap, label: "Live Dashboard" }
  end

  subgraph Docker [Docker containers]
    ScalaLayer
    KafkaLayer
    MonitoringLayer
  end

  Producer -->|Consume| APIServer
  APIServer -->|Serve via HTTP| Producer
  Producer -->|Produce| KafkaBroker
  Consumer -->|Consume| KafkaBroker
  KafkaBroker -->|Serve| Consumer
  KafkaUI -->|Query| KafkaBroker
  Consumer -->|Serve| Dashboard
  Dashboard -->|Consume| Consumer

  style Docker fill: #099cec
  style KafkaBroker fill:#ffffff,stroke:#000000,color:#000000,stroke-width:1px
  style APIServer fill:#ffffff,stroke:#000000,color:#000000,stroke-width:1px
  style Producer fill:#df311e,stroke:#000000,color:#000000,stroke-width:1px
  style Consumer fill:#df311e,stroke:#000000,color:#000000,stroke-width:1px
  style KafkaUI fill:#4f4fff,stroke:#ffffff,color:#ffffff,stroke-width:1px
  style Dashboard fill:#feca1f,stroke:#ffffff,color:#ffffff,stroke-width:1px
```

## Features

- **Data Ingestion**
  - Consumes data from an external streaming API.
  - Processes continuous event streams using **FS2** and **Cats Effect**.

- **Data Transformation**
  - Applies functional and composable transformations to incoming events.
  - Ensures type-safe and asynchronous stream processing.

- **Kafka Integration**
  - Publishes transformed events to **Apache Kafka** using **FS2-Kafka**.
  - Consumes Kafka topics in real time with backpressure support.

- **Live Dashboard**
  - Streams processed data to a dynamic dashboard.
  - Provides real-time visualization of incoming events.

- **Containerized Deployment**
  - All services are packaged and deployed with **Docker**.
  - Enables reproducible local and production environments.

## Tech Stack

- **Scala** → Core language.
- **Cats Effect** → Functional effect system and concurrency.
- **FS2** → Purely functional streaming library.
- **FS2-Kafka** → Functional Kafka Producer and Consumer.
- **Apache Kafka** → Distributed event streaming platform.
- **Docker** → Containerization and deployment.

## Pipeline Overview

1. **Extract**: Consume data from a streaming API.
2. **Transform**: Apply business logic and data transformations using FS2 streams.
3. **Publish**: Send transformed events to Kafka topics.
4. **Consume**: Read Kafka events through an FS2-Kafka consumer.
5. **Visualize**: Feed processed data to a live dashboard in real time.
