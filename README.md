# Card Management Service

[![CI](https://github.com/Dedmoo/card-management-service/actions/workflows/ci.yml/badge.svg)](https://github.com/Dedmoo/card-management-service/actions/workflows/ci.yml)

Card issuance and lifecycle microservice: issue **demo** cards with a Luhn-valid PAN (test IIN `400000`), mask the number, manage daily limits, and block / unblock.

Built with **Java 17** and **Spring Boot 3**. Cards are persisted to an **H2 file database** (via Spring Data JPA) so issued cards survive a restart, with zero external infrastructure to run.

> Educational demo only. Not PCI DSS compliant. Generated PANs use a documented test IIN and must never be treated as real card numbers.

## Scope (honest)

This is a learning / portfolio service, not a production card platform.

| Capability | Status |
|------------|--------|
| Issue a Luhn-valid demo card, mask PAN, manage limits, block/unblock | Implemented |
| H2 file persistence of cards (survives restart) | Implemented |
| Luhn PAN validation endpoint | Implemented |
| Real card network / issuer processor integration | Not included |
| PCI DSS controls (tokenization, HSM, encrypted storage) | Not included |
| AuthN/AuthZ on the API | Not included |

## Architecture

```mermaid
flowchart TD
    Client["Ops / Channel"] -->|/api/cards| API["CardController"]
    API --> Service["CardService"]
    Service --> Luhn["LuhnValidator<br/>check digit + validation"]
    Service --> Store[("In-memory card store")]
    Service --> View["CardView<br/>masked PAN only"]
    API --> Validate["POST /validate<br/>returns valid + panLast4 only"]
```

## Card lifecycle

```mermaid
stateDiagram-v2
    [*] --> ACTIVE: issue
    ACTIVE --> BLOCKED: block
    BLOCKED --> ACTIVE: unblock
    ACTIVE --> ACTIVE: update limit
```

## Features

- Issue cards with a generated **Luhn-valid** demo PAN (test IIN `400000`) and expiry
- Full PAN never leaves the API; card views expose a masked PAN only
- Validate endpoint returns `{ valid, panLast4 }` — never echoes the full PAN
- Daily limit updates
- Block / unblock lifecycle
- Cards persisted to an H2 file database — issued cards survive a restart

## Domain model

Class-level view of the main types and how they relate (fields, operations and dependencies).

```mermaid
classDiagram
    direction TB
    class CardController {
        <<controller>>
        -cardService: CardService
        +issue(request) CardView
        +list() List~CardView~
        +get(cardId) CardView
        +block(cardId) CardView
        +unblock(cardId) CardView
        +updateLimit(cardId, request) CardView
        +validate(request) Map
    }
    class CardService {
        <<service>>
        -cardRepository: CardRepository
        +issue(name, dailyLimit) CardView
        +get(cardId) CardView
        +list() List~CardView~
        +block(cardId) CardView
        +unblock(cardId) CardView
        +updateLimit(cardId, dailyLimit) CardView
    }
    class CardRepository {
        <<repository>>
        +save(card) Card
        +findById(cardId) Optional~Card~
        +findAll() List~Card~
    }
    class LuhnValidator {
        <<utility>>
        +isValid(pan) boolean
        +checkDigit(body) int
    }
    class Card {
        <<entity>>
        -cardId: String
        -cardholderName: String
        -pan: String
        -expiry: String
        -status: CardStatus
        -dailyLimit: BigDecimal
        +maskedPan() String
    }
    class CardStatus {
        <<enumeration>>
        ACTIVE
        BLOCKED
    }
    class IssueCardRequest {
        <<record>>
        +cardholderName: String
        +dailyLimit: BigDecimal
    }
    class CardView {
        <<record>>
        +cardId: String
        +cardholderName: String
        +maskedPan: String
        +expiry: String
        +status: CardStatus
        +dailyLimit: BigDecimal
    }
    class UpdateLimitRequest {
        <<record>>
        +dailyLimit: BigDecimal
    }
    class ValidatePanRequest {
        <<record>>
        +pan: String
    }
    CardController --> CardService
    CardController ..> LuhnValidator
    CardService --> CardRepository
    CardRepository ..> Card
    CardService ..> LuhnValidator
    Card --> CardStatus
    CardView --> CardStatus
    CardController ..> IssueCardRequest
    CardController ..> UpdateLimitRequest
    CardController ..> ValidatePanRequest
```

## Quick start

```bash
./mvnw spring-boot:run      # Linux / macOS
mvnw.cmd spring-boot:run    # Windows
```

Run tests:

```bash
./mvnw test
```

## Example flow

```bash
# Issue a card

[![CI](https://github.com/Dedmoo/card-management-service/actions/workflows/ci.yml/badge.svg)](https://github.com/Dedmoo/card-management-service/actions/workflows/ci.yml)
curl -s -X POST http://localhost:8082/api/cards \
  -H "Content-Type: application/json" \
  -d '{ "cardholderName": "Ada Lovelace", "dailyLimit": 8000 }'

# Validate a PAN with the Luhn algorithm

[![CI](https://github.com/Dedmoo/card-management-service/actions/workflows/ci.yml/badge.svg)](https://github.com/Dedmoo/card-management-service/actions/workflows/ci.yml)
curl -s -X POST http://localhost:8082/api/cards/validate \
  -H "Content-Type: application/json" \
  -d '{ "pan": "4111111111111111" }'

# Block / unblock (use the cardId from issue)

[![CI](https://github.com/Dedmoo/card-management-service/actions/workflows/ci.yml/badge.svg)](https://github.com/Dedmoo/card-management-service/actions/workflows/ci.yml)
curl -s -X POST http://localhost:8082/api/cards/CARD-XXXXXXXX/block
curl -s -X POST http://localhost:8082/api/cards/CARD-XXXXXXXX/unblock
```

## API

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/cards` | Issue a card |
| `GET` | `/api/cards` | List cards |
| `GET` | `/api/cards/{id}` | Get a card |
| `POST` | `/api/cards/{id}/block` | Block a card |
| `POST` | `/api/cards/{id}/unblock` | Unblock a card |
| `POST` | `/api/cards/{id}/limit` | Update daily limit |
| `POST` | `/api/cards/validate` | Luhn-validate a PAN |
| `GET` | `/api/cards/health` | Health check |

## Design notes

- PANs are generated on a demo BIN and completed with a computed Luhn check digit
- Only masked PANs (`**** **** **** 1234`) leave the service
- Storage is an H2 file database (`./data/card-management.mv.db`, gitignored) via Spring Data
  JPA; `spring.jpa.hibernate.ddl-auto=update` lets the schema self-create on first run

## License

MIT — see [LICENSE](LICENSE).
