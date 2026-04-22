[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=pk-f38670e9-3e0c-4963-8da0-1d668a894f26&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=pk-f38670e9-3e0c-4963-8da0-1d668a894f26) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=pk-f38670e9-3e0c-4963-8da0-1d668a894f26&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=pk-f38670e9-3e0c-4963-8da0-1d668a894f26) [![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=pk-f38670e9-3e0c-4963-8da0-1d668a894f26&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=pk-f38670e9-3e0c-4963-8da0-1d668a894f26) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=pk-f38670e9-3e0c-4963-8da0-1d668a894f26&metric=coverage)](https://sonarcloud.io/summary/new_code?id=pk-f38670e9-3e0c-4963-8da0-1d668a894f26) [![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=pk-f38670e9-3e0c-4963-8da0-1d668a894f26&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=pk-f38670e9-3e0c-4963-8da0-1d668a894f26) [![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=pk-f38670e9-3e0c-4963-8da0-1d668a894f26&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=pk-f38670e9-3e0c-4963-8da0-1d668a894f26)
# Store Auth API ([axgiri.tech](https://axgiri.tech))

store_auth is the identity and trust service of the axgiri.tech platform. It is responsible for authentication, session lifecycle, OTP verification, and token trust distribution across internal services.

## Status

Core security service. Business services depend on it for token trust, account access flows, and JWKS key discovery.

### Role in the architecture

store_auth is the single source of truth for user login state. It receives credential based and OTP based authentication requests, issues JWT access and refresh tokens, rotates and revokes refresh sessions, and publishes a JWKS endpoint that other services use for signature verification.

Without this service, each microservice would need its own token logic, key handling, and account flow. store_auth removes that duplication and keeps security behavior consistent.

### Functional scope

- validates user credentials and returns auth tokens
- supports refresh token renewal and targeted or full revoke flows
- supports OTP activation, OTP login, and OTP password recovery paths
- exposes JWKS for public key discovery
- provides internal endpoints used by Grafana auth integration

### API contract highlights

- user auth endpoints are grouped under `/api/v1/users`
- OTP endpoints are grouped under `/api/v1/otp`
- JWKS is exposed via `/api/v1/.well-known/jwks.json`
- internal integration endpoints are grouped under `/api/v1/internal`

### Token trust model

- service signs JWT tokens and manages refresh lifecycle
- downstream services validate tokens against published JWKS key set
- JWT issuer consistency is part of platform wide trust boundary

### Data and integrations

- PostgreSQL stores user and security related persistent entities
- Redis stores short lived authentication state
- Kafka carries registration and compensation events
- Flyway controls schema migrations
- Actuator, Prometheus, and OpenTelemetry provide metrics and traces

### Tech Stack

- Java 21
- Spring Boot 4.0.1
- Spring Security and OAuth2 Resource Server
- Spring Data JPA
- PostgreSQL
- Redis
- Kafka
- Flyway
- Micrometer with Prometheus and OpenTelemetry

### Platform impact

store_auth defines the platform security boundary. Every service that consumes JWT trust chain behavior depends on the stability and correctness of this module.

If store_auth is degraded, login and session refresh degrade first, then all protected domain operations start failing due to trust validation errors.

## All microservices

- https://github.com/axgiri/store-jwt-spring-boot-starter
- https://github.com/axgiri/store_gateway
- https://github.com/axgiri/store_infrastructure
- https://github.com/axgiri/store_auth
- https://github.com/axgiri/store_core
- https://github.com/axgiri/store_chat
- https://github.com/Scheldie/Notification_Reports

## k6 Load Testing Guide

This service has a ready-to-run k6 setup in `k6/`.

### Prerequisites

- Docker and Docker Compose
- GNU make
- k6 CLI installed locally

### Quick Run

Run the full flow (up, readiness checks, seed, test, cleanup):

```bash
make -f k6/main.mk run
```

### Step-by-Step Run

Use this flow when debugging or tuning:

```bash
make -f k6/main.mk up
make -f k6/main.mk wait-schema
make -f k6/main.mk seed
make -f k6/main.mk wait-app
make -f k6/main.mk run-test
make -f k6/main.mk down
```

Notes:

- Seed data is loaded from `store_auth/k6/helpers/seed.sql`.
- Short `curl: (56) Recv failure: Connection reset by peer` lines can appear during warm-up.
- `.envK6` for this module is in `store_auth/k6/helpers/.envK6`.

### k6 Tuning Knobs

Common variables in `store_auth/k6/helpers/.envK6`:

- `BASE_URL`
- `AUTH_USERS_COUNT`
- `AUTH_PASSWORD`
- `AUTH_USER_EMAIL_TEMPLATE`

### Capacity Template (VM: 4 vCPU, 4GB DDR4)

| Metric             | Value             |
|--------------------|-------------------|
| VM profile         | 4 vCPU / 4GB DDR4 |
| Test duration      | 30s               |
| Virtual users      | 3700              |
| Total requests     | 169156            |
| Throughput (req/s) | 5542.3            |
| p95 latency (ms)   | 398.2             |
| p90 latency (ms)   | 152.6             |
| Error rate (%)     | 0                 |