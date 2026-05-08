# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Project Is

**phoss-ap** is a production Peppol Access Point — a Spring Boot application that sends and receives electronic business documents (invoices, orders, etc.) over the Peppol network using the AS4 messaging protocol.

## Build & Test Commands

```bash
# Full build and test
mvn clean verify

# Skip integration tests
mvn clean package

# Run a single unit test class
mvn test -Dtest=ClassName

# Run a single integration test class
mvn verify -Dit.test=ClassNameIT

# Start local PostgreSQL + MySQL for unit tests
docker compose -f unittest-db-docker-compose.yml up -d
docker compose -f unittest-db-docker-compose.yml down
```

Integration tests (`*IT.java`) are wired to the Maven Failsafe plugin in `phoss-ap-basic` and run during `verify`. S3 integration tests use TestContainers LocalStack.

## Module Architecture

This is an 11-module Maven project. The dependency flow is:

```
phoss-ap-api          (interfaces & domain model — no implementations)
    ↓
phoss-ap-basic        (storage: filesystem & S3, utilities, common implementations)
    ↓
phoss-ap-db           (JDBC persistence, Flyway migrations)
    ↓
phoss-ap-core         (orchestration: inbound/outbound, phase4 AS4, SMP lookup, retry, circuit breaker)
    ↓
phoss-ap-forwarding   (HTTP sync/async, S3, SFTP forwarding backends)
phoss-ap-dirsender    (directory-based SBD batch sender)
phoss-ap-sentry       (Sentry error tracking integration)
phoss-ap-validation   (external phorm Validation Service integration)
    ↓
phoss-ap-webapp       (Spring Boot entry point — assembles everything)
```

Standalone tools:
- `phoss-ap-testbackend`: Receives forwarded documents for testing
- `phoss-ap-testsender`: Exercises the outbound API

## Key Flows

**Inbound:** `Phase4InboundMessageProcessorSPI` → `InboundOrchestrator` → duplicate check → optional validation → storage (filesystem/S3) → forwarding (HTTP/S3/SFTP) with exponential-backoff retry.

**Outbound:** REST API call → `OutboundOrchestrator` → SMP lookup for receiver endpoint → SBDH creation → phase4 AS4 envelope → send with retry + circuit breaker.

**Supporting services:** Flyway schema migration (auto on startup), Peppol Reporting (scheduled), MLS tracking, archival scheduler.

## Configuration

Primary config: `phoss-ap-webapp/src/main/resources/application.properties`

Before running the app you must set these (all marked `CHANGEME` in the file):

| Property | Purpose |
|---|---|
| `global.datapath` | Base directory for document storage |
| `peppol.owner.seatid` | Your Peppol Seat ID (e.g. `POP000306`) |
| `peppol.owner.countrycode` | 2-letter country code |
| `phase4.api.requiredtoken` | API auth bearer token |
| `phossap.jdbc.url/user/password` | Database credentials |
| `org.apache.wss4j.crypto.merlin.keystore.*` | Peppol PKCS12 certificate |
| `peppol.reporting.schedule.enabled` | Set `false` to suppress monthly TSR/EUSR submission (test lab) |

Database: PostgreSQL is primary (schemas: `ap`, `reporting`, `report`); MySQL is supported. H2 is used for tests. Flyway handles all migrations automatically.

## Docker

```bash
# Production stack (app + PostgreSQL 18)
docker compose up -d
docker compose down
```

The Dockerfile is a single-stage build using `eclipse-temurin:21-alpine`. It expects a pre-built JAR at `phoss-ap-webapp/target/phoss-ap-webapp-*-SNAPSHOT.jar`, so run `mvn clean package` first.

## Test Sender

`phoss-ap-testsender` is a standalone Spring Boot tool for exercising the outbound API. Build it with:

```bash
mvn clean package -DskipTests -pl phoss-ap-testsender -am
```

Scripts in `phoss-ap-testsender/` each send one document type (xml / sbd / pdf) and find the JAR dynamically — no version pinning needed. Pass AP URL and Peppol IDs as CLI args:

```bash
cd phoss-ap-testsender
bash run-single-xml.sh \
  --testsender.target.base-url=http://localhost:8780 \
  --testsender.target.token=phoss-ap-development-token \
  --testsender.peppol.sender-id=iso6523-actorid-upis::0088:1111111111111 \
  --testsender.peppol.receiver-id=iso6523-actorid-upis::0088:2222222222222
```

A successful send returns `"status":"sent"` and `"reportingStatus":"reported"`. The `run-bulk.sh` and `run-bulk-rampup.sh` scripts exercise concurrency (default: 100 docs, 10 threads).

## Technology Notes

- **Java 21** required; compiler uses `-parameters` flag (needed for Spring MVC reflection)
- **Spring Boot 4.0.x** — note this is the 4.x line (Jakarta EE, not javax)
- **phase4** is the AS4 implementation library (phax ecosystem)
- **phax libraries** (ph-commons, ph-web, ph-db, peppol-commons, etc.) are the core Helger/Peppol utility stack — most business logic delegates into these
- **Failsafe** (not Spring Retry) provides circuit breaker and retry with exponential backoff
- Property overrides via env vars use Spring's relaxed binding: `phossap.jdbc.url` → `PHOSSAP_JDBC_URL`
