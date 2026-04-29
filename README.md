# Payment Processing System

A robust, scalable, and resilient microservices-based system for handling payments.

## Architecture

The system consists of several microservices:

- **API Gateway**: Entry point for all requests, handles routing and global concerns.
- **Payment Service**: Manages payment initiation, validation, and persistence.
- **Processing Service**: Asynchronously processes payments received via Kafka.
- **Bank Adapter Service**: Integrates with external bank APIs using specific adapters.
- **Shared Library**: Common DTOs, security utilities, and shared logic.

## Infrastructure

The local development environment uses:
- **PostgreSQL**: For persistent data storage.
- **Kafka**: For event-driven communication.

## Getting Started

### Prerequisites

- Java 21
- Maven 3.8+
- Docker & Docker Compose

## Current Version: 0.0.9

### Features Implemented
- Global Rate Limiting in API Gateway (Bucket4j)
- Persistence logic for payment requests (save to PostgreSQL with `PENDING` status)
- PostgreSQL database setup & schema management (Flyway)
- CI/CD Pipelines (GitHub Actions & CircleCI)
- Configured Mockito as Java agent for tests
- JWT Authentication Middleware (Payment Service)
- Payment Initiation Endpoint (`POST /api/v1/payments`)
- Payment Data Validation (amount, currency, recipient)
- Shared Security Library
- Docker Compose setup (Postgres, Kafka)
- Microservices skeleton implementation

### CI/CD
- **GitHub Actions**: Automated build and test on push and pull requests.
- **CircleCI**: Continuous integration with service containers for integration testing.

### Local Infrastructure

Start the infrastructure using Docker Compose:

```bash
docker-compose up -d
```

### Build

Build the entire project:

```bash
mvn clean install
```

## Versioning

Current Version: 0.0.9 (Semantic versioning is used)

## Phase 1: Setup & Infrastructure (Completed)
- [x] Initialized project structure for microservices.
- [x] Set up Docker Compose for local infrastructure.
- [x] Defined shared Event Schema.
- [x] Implemented Shared Security Library.
- [x] Configured GitHub Actions & CircleCI pipelines.
