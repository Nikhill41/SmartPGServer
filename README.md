# Smart PG Management System

> A production-oriented Java/Spring Boot REST platform for managing paying-guest (PG) properties, tenant operations, automated billing, and IoT-enabled electricity monitoring.

[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)](https://redis.io/)
[![AWS](https://img.shields.io/badge/AWS-IoT%20%7C%20DynamoDB%20%7C%20S3-232F3E?logo=amazonaws&logoColor=white)](https://aws.amazon.com/)
[![ESP32](https://img.shields.io/badge/ESP32-C%2B%2B-E7352C?logo=espressif&logoColor=white)](https://www.espressif.com/en/products/socs/esp32)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)

## Overview

Smart PG is a connected property-management system for paying-guest accommodations. It combines a Java backend, an ESP32-based smart electricity meter, and AWS services to support property operations from room booking through electricity billing.

- **For property owners:** manage properties, rooms, amenities, bookings, tenants, meters, billing rules, bills, and payment verification.
- **For tenants:** discover available rooms, submit booking requests, view bills and meter data, initiate checkout, and upload payment proof.
- **For electricity management:** monitor room-level consumption, detect meter connectivity and disconnection, calculate electricity charges, and provide remote relay control.
- **For operations:** automate recurring billing and overdue processing, maintain bill history, and expose health and metrics endpoints.

## Main features

- Built a **Java 17 / Spring Boot 3 REST API** with layered controller-service-repository architecture for PG property, room, tenant, booking, billing, and payment workflows.
- Implemented **stateless JWT authentication**, BCrypt password hashing, OTP-based verification, CORS configuration, and method/route-level owner-versus-tenant authorization.
- Developed an **ESP32 smart-meter device in C++** to measure electricity consumption, track device connectivity, identify connection/disconnection conditions, and communicate meter state and readings to the backend through AWS IoT.
- Integrated **AWS IoT Device Shadows and MQTT relay commands** for live meter status and remote control, with **DynamoDB** queries for historical time-series readings.
- Designed automated billing workflows with configurable charges, electricity-cost allocation, bill versioning, overdue processing, payment-proof review, UPI links, and checkout/security-deposit handling.
- Added **Redis-backed caching**, PostgreSQL persistence through Spring Data JPA, S3 uploads with presigned URLs, Springdoc/OpenAPI documentation, and Spring Boot Actuator health/metrics endpoints.
- Containerized the application with **Docker Compose** and persistent PostgreSQL/Redis services to make development and integration setup repeatable.

## Core capabilities

### Property and occupancy management

- Create and update PG properties, rooms, amenities, pricing, capacity, and room status.
- Upload property and room images to Amazon S3 and serve them through presigned URLs.
- Search properties and available rooms using filters and cached advanced-search results.
- Process tenant booking requests with availability, ownership, duplicate-request, and gender-preference checks.
- Accept, reject, and complete bookings while maintaining occupancy and tenant-stay records.

### Billing and payments

- Configure per-property billing modes, standard charges, UPI/bank payment details, late fees, and security deposits.
- Generate individual or bulk tenant bills with rent, standard charges, electricity allocation, deductions, and pro-rated calculations.
- Run scheduled automatic bill generation and overdue-bill processing.
- Maintain editable bill versions and audit-friendly payment-proof history.
- Support checkout previews, payment initiation, payment-proof upload, owner verification, late-fee waiver, and settlement completion.

### ESP32 smart meter and IoT monitoring

The smart-meter component is built around an ESP32 and programmed in C++. It provides the device-side data collection and control layer for room-level electricity monitoring.

#### Device-side responsibilities

- Reads electrical usage data from the connected metering hardware and tracks consumption over time.
- Publishes current readings and device state to the cloud using the ESP32 connectivity stack.
- Maintains a connection state so the platform can distinguish an active meter from a disconnected or unavailable device.
- Detects connection and disconnection events and reports the latest state to the backend.
- Receives relay-control commands and changes the connected electrical relay state.
- Publishes updated state after a relay operation so the backend can confirm whether the command was applied.
- Continues to expose the latest known meter state through the AWS IoT Device Shadow when the backend requests live information.

#### Backend and cloud responsibilities

- Pair and unpair a room meter with owner authorization.
- Read live meter/device-shadow state and determine whether a meter is online from a configurable 60-second freshness threshold.
- Query historical readings from DynamoDB for a selected time range.
- Publish relay commands through AWS IoT and retry confirmation within a bounded two-second timeout.
- Use the meter readings during billing to calculate each tenant's electricity share.
- Restrict owners to their meters and tenants to the meter associated with their own room.

#### Smart-meter data flow

```text
Electrical load
      |
      v
ESP32 smart meter (C++)
  - measure consumption
  - track connection state
  - control relay
      |
      | AWS IoT / Device Shadow
      v
Spring Boot meter services
  - live readings and online status
  - owner/tenant authorization
  - relay command confirmation
      |
      +--> DynamoDB: historical readings
      +--> PostgreSQL: meter-room-owner relationships
      +--> Billing services: electricity charge calculation
```

The ESP32 firmware and the Spring Boot backend are separate layers: C++ handles embedded device behavior and communication, while Java handles authentication, authorization, persistence, billing rules, and API access.

### Security and reliability

- Stateless JWT request authentication with a custom Spring Security filter.
- BCrypt password encoding and OTP verification with expiry and attempt limits.
- Explicit role boundaries for `OWNER` and `USER` APIs.
- Bean validation DTOs, centralized exception handling, consistent API response models, and domain-specific error messages.
- Actuator health, info, mappings, and metrics endpoints for operational visibility.

## Architecture

```text
Web / Mobile Client
        |
        | JSON over HTTP
        v
Spring Boot REST API
  Controllers
      |
  Services + business rules
      |
  Repositories / integrations
      |             |              |              |
 PostgreSQL       Redis           AWS S3       AWS IoT + DynamoDB
 domain data      search/cache    media         live + historical meters
                                                   ^
                                                   |
                                      ESP32 smart meter (C++)
```

The codebase follows a separation of concerns:

| Layer | Responsibility |
| --- | --- |
| `controller` | HTTP routes, request mapping, authorization boundaries, response contracts |
| `dto` | Validated request objects and stable API response models |
| `service` | Booking, billing, meter, payment, authentication, and search business rules |
| `repository` | PostgreSQL persistence through Spring Data JPA |
| `security` | JWT creation/validation and authenticated user loading |
| `config` | Security, AWS clients, Redis serialization, application integration |
| `exception` | Domain exceptions and consistent error responses |

## Representative API surface

The full contract is available through Swagger UI after startup.

| Area | Example endpoints | Access |
| --- | --- | --- |
| Authentication | `/api/auth/**` | Public entry points |
| Discovery | `/api/public/pg/search`, `/api/public/rooms/available` | Public |
| Owner operations | `/api/owner/**` | `OWNER` |
| Tenant operations | `/api/user/**`, `/api/tenant/**` | `USER` |
| Meter reads | `/api/meters/{meterId}/live`, `/api/meters/{meterId}/readings` | Authenticated |
| Meter control | `/api/owner/meters/{meterId}/relay` | `OWNER` |
| Billing | `/api/owner/billing/**`, `/api/tenant/billing/**` | Role-scoped |
| Documentation and health | `/swagger-ui.html`, `/v3/api-docs`, `/actuator/health` | Configured public endpoints |

## Technology stack

- **Language/runtime:** Java 17
- **Embedded development:** C++ on ESP32
- **Backend:** Spring Boot 3.2, Spring MVC, Spring Data JPA, Spring Security, Bean Validation
- **Persistence:** PostgreSQL 15 with Hibernate
- **Caching:** Redis 7 with JSON serialization and a five-minute cache policy
- **Cloud integrations:** AWS SDK v2, S3, IoT Data Plane, Device Shadows, DynamoDB
- **Authentication:** JWT, BCrypt, OTP/email verification
- **API quality:** DTO mapping with ModelMapper, OpenAPI/Swagger, Actuator
- **Delivery:** Maven Wrapper, Docker, Docker Compose, GitHub Actions deployment workflow
- **Testing:** Spring Boot test context and Spring Security test dependencies

## Run locally with Docker

### Prerequisites

- Docker Desktop with Docker Compose
- An AWS account/configuration if testing S3, IoT, or DynamoDB features
- An email provider configuration if testing email/OTP flows

### Start the stack

1. Create a local environment file from the template:

   ```powershell
   Copy-Item .env.example .env
   ```

2. Replace placeholder values in `.env`. Keep credentials local and never commit them.

3. Build and start the application, PostgreSQL, and Redis:

   ```powershell
   docker compose up --build
   ```

4. Open:

   - Application: <http://localhost:8080>
   - Swagger UI: <http://localhost:8080/swagger-ui.html>
   - Actuator health: <http://localhost:8080/actuator/health>

5. Stop the stack:

   ```powershell
   docker compose down
   ```

The Compose file persists PostgreSQL and Redis data in named volumes. Use `docker compose down -v` only when intentionally removing local data.

### Run with Maven

For a local Java 17 environment:

```powershell
.\mvnw.cmd spring-boot:run
```

The application requires database, Redis, and external-service configuration from environment variables. Docker is the recommended path for a complete local stack.

## Verify the project

Run the available automated test suite:

```powershell
.\mvnw.cmd test
```

The current test suite includes an application-context smoke test. API behavior can be explored and validated through the generated OpenAPI contract at `/swagger-ui.html`.

## Repository guide

- [`src/main/java/com/pgmanagement/controller`](./src/main/java/com/pgmanagement/controller) - REST API boundaries
- [`src/main/java/com/pgmanagement/service`](./src/main/java/com/pgmanagement/service) - domain workflows and integrations
- [`src/main/java/com/pgmanagement/security`](./src/main/java/com/pgmanagement/security) - JWT authentication
- [`src/main/java/com/pgmanagement/entity`](./src/main/java/com/pgmanagement/entity) - relational domain model
- [`src/main/java/com/pgmanagement/repository`](./src/main/java/com/pgmanagement/repository) - persistence interfaces
- [`src/main/resources/application.yml`](./src/main/resources/application.yml) - environment-driven configuration
- [`README-DOCKER.md`](./README-DOCKER.md) - Docker troubleshooting and operational commands
- [`SmartPG.md`](./SmartPG.md) - detailed feature and workflow handoff

## Engineering notes

- The application supports environment-driven credentials for database, AWS, Redis, and email integrations; replace all development placeholders before deployment.
- Production deployments should use a managed secret store, rotate credentials, remove tracked development defaults, use `ddl-auto: validate` or migrations, and place the API behind HTTPS.
- AWS IoT and DynamoDB features require correctly provisioned IoT things, Device Shadows, IAM permissions, and the configured DynamoDB table.
- The project intentionally uses domain-specific services so billing, booking, meter access, and payment verification rules remain independently testable and maintainable.

## License

No license has been specified for this repository yet.
