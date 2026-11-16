# Order Platform: Spring Boot Microservices with Kafka & Redis

An event-driven e-commerce backend made of two Spring Boot microservices. **order-service** manages products, stock and orders; every order change is published to **Apache Kafka**. **notification-service** consumes those events and creates customer notifications. Product reads are cached in **Redis**, each service owns its own **PostgreSQL** database, and the whole system starts with one `docker compose up`.

## Architecture

```
 REST :8081                                                REST :8082
     │                                                         │
     ▼                                                         ▼
┌───────────────────────────┐   order-events    ┌──────────────────────────────┐
│       order-service       │ ───── Kafka ────▶ │     notification-service     │
│ products · stock · orders │                   │ consumer · idempotency · DLT │
└──────┬─────────────┬──────┘                   └──────────────┬───────────────┘
       │             │                                         │
  Redis cache    PostgreSQL                               PostgreSQL
  (products)     (orders_db)                          (notifications_db)
```

**Order flow**

1. `POST /api/orders` reserves stock, calculates the discount and saves the order in one transaction.
2. After the transaction commits, an `ORDER_CREATED` event is published to Kafka (key = order id).
3. notification-service consumes it, skips duplicates by event id and stores the notification.
4. Status changes (`PAID`, `SHIPPED`, `DELIVERED`) and cancellations publish further events the same way. Cancelling returns the stock.

## Tech stack

| Area | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3 (Web, Data JPA, Validation, Cache, Actuator), Spring Kafka |
| Messaging | Apache Kafka 3.8 (KRaft mode, no ZooKeeper) |
| Database | PostgreSQL 16, Flyway migrations (database per service) |
| Cache | Redis 7 |
| Build | Gradle multi-module (`common`, `order-service`, `notification-service`) |
| API docs | springdoc-openapi / Swagger UI |
| DevOps | Docker multi-stage builds, Docker Compose, GitHub Actions |
| Testing | JUnit 5, Mockito, AssertJ |

## Key features

- **Event-driven communication**: services share only an event contract (`common` module), not a database
- **Events published after commit**: `@TransactionalEventListener(AFTER_COMMIT)`, so a rolled-back order is never announced
- **Ordered per order**: the order id is the Kafka key, so all events of one order go to the same partition
- **Idempotent consumer**: each event has a UUID; duplicates are detected and skipped
- **Retries and dead-letter topic**: failed messages are retried 3 times, then moved to `order-events.DLT`
- **Redis caching**: product reads cached with a 10-minute TTL; stock changes evict the cache after commit
- **Optimistic locking** (`@Version`) on products, so two orders cannot oversell the last item
- **Rich domain model (OOP)**:
  - *Encapsulation*: stock and totals change only through methods such as `addItem()` and `changeStatus()`
  - *Inheritance*: entities extend `BaseEntity` (id and audit timestamps)
  - *Polymorphism*: discount rules implement `DiscountPolicy` (`NoDiscountPolicy`, `PercentageDiscountPolicy`)
  - *State rules*: `OrderStatus` decides which transitions are allowed
- **Request/response DTOs** as Java records with Bean Validation; entities never leave the service layer
- **Consistent error responses** (`400`, `404`, `409`) with field-level validation messages

## Getting started

Requirements: Docker Desktop.

```bash
docker compose up --build
```

| Service | URL |
|---|---|
| order-service Swagger UI | http://localhost:8081/swagger-ui.html |
| notification-service Swagger UI | http://localhost:8082/swagger-ui.html |
| Health checks | http://localhost:8081/actuator/health, http://localhost:8082/actuator/health |

Infrastructure host ports: PostgreSQL `5434`, Redis `6380`, Kafka `29092`.

## Try it

```bash
# 1. Create products
curl -X POST http://localhost:8081/api/products -H "Content-Type: application/json" \
  -d '{"name":"Mechanical Keyboard","price":45.00,"stockQuantity":10}'
curl -X POST http://localhost:8081/api/products -H "Content-Type: application/json" \
  -d '{"name":"Wireless Mouse","price":20.00,"stockQuantity":5}'

# 2. Place an order (110.00 >= 100, so 10% discount applies)
curl -X POST http://localhost:8081/api/orders -H "Content-Type: application/json" \
  -d '{"customerEmail":"customer@example.com","items":[{"productId":1,"quantity":2},{"productId":2,"quantity":1}]}'

# 3. Move it through the lifecycle
curl -X PATCH http://localhost:8081/api/orders/1/status -H "Content-Type: application/json" -d '{"status":"PAID"}'

# 4. See the notifications created from Kafka events
curl http://localhost:8082/api/notifications/orders/1
```

Example order response:

```json
{
  "id": 1,
  "customerEmail": "customer@example.com",
  "status": "CREATED",
  "items": [
    { "productId": 1, "productName": "Mechanical Keyboard", "unitPrice": 45.00, "quantity": 2, "lineTotal": 90.00 },
    { "productId": 2, "productName": "Wireless Mouse", "unitPrice": 20.00, "quantity": 1, "lineTotal": 20.00 }
  ],
  "subtotal": 110.00,
  "discountAmount": 11.00,
  "totalAmount": 99.00
}
```

More ready-made requests: [`requests.http`](requests.http).

### Look inside Kafka and Redis

```bash
# Events on the topic
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic order-events --from-beginning

# Cached products
docker compose exec redis redis-cli KEYS '*'
```

## API

**order-service** (`:8081`)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/products` | Create a product |
| GET | `/api/products` | List products (paged) |
| GET | `/api/products/{id}` | Get a product (Redis cache) |
| PUT | `/api/products/{id}` | Update a product |
| DELETE | `/api/products/{id}` | Delete a product not used in orders |
| POST | `/api/orders` | Place an order |
| GET | `/api/orders?customerEmail=` | List orders (paged, optional customer filter) |
| GET | `/api/orders/{id}` | Get an order |
| PATCH | `/api/orders/{id}/status` | Change status: `PAID`, `SHIPPED`, `DELIVERED`, `CANCELLED` |

**notification-service** (`:8082`)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/notifications?recipient=` | Notifications for a customer (paged) |
| GET | `/api/notifications/orders/{orderId}` | Notifications for an order |

## Project structure

```
order-platform
├── common/                    shared event contract (OrderEvent, OrderEventType)
├── order-service/
│   └── src/main/java/.../orderservice
│       ├── product/           Product entity, service (Redis cache), controller, DTOs
│       ├── order/             Order aggregate, OrderStatus, service, controller, DTOs
│       │   └── discount/      DiscountPolicy strategy and implementations
│       ├── messaging/         OrderEventPublisher (Kafka producer, after commit)
│       ├── config/            Kafka topic, Redis cache, discount, OpenAPI
│       └── common/            BaseEntity, PageResponse, exceptions and error handler
├── notification-service/
│   └── src/main/java/.../notificationservice
│       ├── messaging/         OrderEventListener (Kafka consumer)
│       ├── notification/      entity, idempotent service, message factory, controller
│       └── config/            retry + dead-letter topic, OpenAPI
├── docker/postgres/init.sql   creates the second database
└── docker-compose.yml
```

## Tests

```bash
gradle test
# or without a local Gradle installation:
docker run --rm -v "$PWD":/workspace -w /workspace gradle:8.10.2-jdk21 gradle test --no-daemon
```

Covered: order domain rules (stock reservation, pricing, discounts, status lifecycle, cancellation), order service (merging duplicate items, events published per change), discount policy, and the idempotent notification consumer.

## Author

**Mansur Bagirzade**: Java Backend & Flutter Developer · [GitHub](https://github.com/mbaghirov)
