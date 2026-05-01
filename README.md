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

## Current Version: 0.0.12

### Features Implemented
- Resilience patterns (Retry, Circuit Breaker, Fallback) for Bank Integrations
- Unit tests for OutboxPublisher
- Intelligent Routing Logic to select Bank Adapter
- Bank Adapter Interface and Mock Implementations
- Feign Client communication between Processing and Bank services
- Failure handling for bank routing
- Event-Driven Architecture (Kafka)
- Transactional Outbox Pattern for reliable event publishing
- Kafka Consumer in Processing Service
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

Current Version: 0.0.12 (Semantic versioning is used)

## Phase 1: Setup & Infrastructure (Completed)
- [x] Initialized project structure for microservices.
- [x] Set up Docker Compose for local infrastructure.
- [x] Defined shared Event Schema.
- [x] Implemented Shared Security Library.
- [x] Configured GitHub Actions & CircleCI pipelines.

## Phase 2: Core Payment Features (Completed)
- [x] Implement JWT Authentication Middleware.
- [x] Create Payment Initiation Endpoint.
- [x] Implement Payment Data Validation.
- [x] Set up PostgreSQL database and schema for payments.
- [x] Implement persistence logic to save payments.
- [x] Implement Rate Limiting logic at API Gateway.

## Phase 3: Messaging & Event-Driven Architecture (Completed)
- [x] Implement Kafka Producer in Payment Service.
- [x] Implement Outbox Pattern for reliable Kafka publishing.
- [x] Implement Kafka Consumer in Processing Service.

## Phase 4: Intelligent Routing & Bank Integration (Completed)
- [x] Implement Routing Logic to select the appropriate Bank Adapter.
- [x] Implement Bank Adapter Interface and mock implementations.
- [x] Implement communication between Processing Service and Bank Adapters.
- [x] Handle routing failures by marking payment as FAILED.

## Phase 5: Resilience & Advanced Integration (Completed)
- [x] Implement Retry Strategy with exponential backoff for Bank API calls.
- [x] Implement Circuit Breaker pattern for Bank Integrations.
- [x] Implement Fallback Strategy for multi-bank support.
- [x] Implement Bulkhead and Timeouts for resource protection.
