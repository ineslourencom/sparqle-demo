# Order Link Service

Order Link orchestrates merchant shipments through the Late Logistics carrier, maintains internal order state, and streams live updates to client UIs.

---

## Outcomes

- Accept new orders via REST, persist inventory snapshots, and transition state automatically.
- Integrate with an unreliable carrier API while surfacing shipment labels and failure states for remediation.
- Expose real-time status changes over Server-Sent Events (SSE) for the Vue dashboard.
- Provide hooks for adding additional carriers with minimal refactoring.

---

## Architecture Overview

```
POST /api/orders
    ↓
OrderServiceFacade → ApplicationEventPublisher
    ↓
OnPendingStateOrder → InventoryService (reserve/update)
    ↓
OnInventoryReservedStateOrder → LateLogisticsClient (shipment + label)
    ↓
WebhookService ⇐ Late Logistics webhook
    ↓
OnLogisticsConfirmedStateOrder → InventoryService
    ↓
EventService → SSE stream (/api/events/subscribe)
```

| Concern             | Responsibility                              | Key Components                                                                                                                                                                                                                                                                                                                                                                                                               |
| ------------------- | ------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Command handling    | Accept REST input, publish domain event     | [`src/main/java/com/orderlink/order/controller/OrderController.java`](src/main/java/com/orderlink/order/controller/OrderController.java), [`src/main/java/com/orderlink/order/service/OrderServiceFacade.java`](src/main/java/com/orderlink/order/service/OrderServiceFacade.java)                                                                                                                                           |
| Inventory state     | Persist and expose order snapshots          | [`src/main/java/com/orderlink/order/service/InventoryService.java`](src/main/java/com/orderlink/order/service/InventoryService.java), [`src/main/java/com/orderlink/order/entity`](src/main/java/com/orderlink/order/entity)                                                                                                                                                                                                 |
| Async orchestration | React to state transitions                  | [`src/main/java/com/orderlink/event/OnPendingStateOrder.java`](src/main/java/com/orderlink/event/OnPendingStateOrder.java), [`src/main/java/com/orderlink/event/OnInventoryReservedStateOrder.java`](src/main/java/com/orderlink/event/OnInventoryReservedStateOrder.java), [`src/main/java/com/orderlink/event/OnLogisticsConfirmedStateOrder.java`](src/main/java/com/orderlink/event/OnLogisticsConfirmedStateOrder.java) |
| Carrier integration | Call Late Logistics, retry, retrieve labels | [`src/main/java/com/orderlink/logistic/client/LateLogisticsClient.java`](src/main/java/com/orderlink/logistic/client/LateLogisticsClient.java), [`src/main/java/com/orderlink/config/LateLogisticsFeignConfig.java`](src/main/java/com/orderlink/config/LateLogisticsFeignConfig.java)                                                                                                                                       |
| Real-time UX        | Push order updates to browsers              | [`src/main/java/com/orderlink/notification/controller/EventController.java`](src/main/java/com/orderlink/notification/controller/EventController.java), [`src/main/java/com/orderlink/notification/service/EventService.java`](src/main/java/com/orderlink/notification/service/EventService.java)                                                                                                                           |
| Webhook ingestion   | Map carrier events to internal state        | [`src/main/java/com/orderlink/logistic/webhook/WebhookController.java`](src/main/java/com/orderlink/logistic/webhook/WebhookController.java), [`src/main/java/com/orderlink/logistic/webhook/WebhookService.java`](src/main/java/com/orderlink/logistic/webhook/WebhookService.java)                                                                                                                                         |

---

## Domain Model & State Machine

Orders flow through `OrderState` values:

| State                 | Trigger               | Details                 |
| --------------------- | --------------------- | ----------------------- |
| `PENDING`             | Order accepted        | Awaiting reservation    |
| `INVENTORY_RESERVED`  | Inventory locked      | Shipment creation ready |
| `LOGISTICS_CONFIRMED` | Carrier accepted      | Tracking code assigned  |
| `COMPLETED`           | Webhook delivered     | Final state             |
| `FAILED_INVENTORY`    | Reservation failure   | Auto-retry scope        |
| `FAILED_LOGISTICS`    | Carrier failure       | Eligible for retry      |
| `FAILED`              | Unknown fatal issue   | Manual intervention     |
| `CANCELLED`           | Merchant cancellation | Stops pipeline          |

`OnPendingStateOrder` upgrades orders to `INVENTORY_RESERVED` once a record exists. `OnInventoryReservedStateOrder` handles both `INVENTORY_RESERVED` and `FAILED_LOGISTICS` states so failed shipments can retry. Webhooks may push orders directly to `LOGISTICS_CONFIRMED`, `COMPLETED`, or `FAILED_LOGISTICS`.

---

## Resilience Strategy

- **Feign retrying** – `Retryer.Default` with bounded attempts in [`LateLogisticsFeignConfig`](src/main/java/com/orderlink/config/LateLogisticsFeignConfig.java).
- **Stateful fallbacks** – failures emit new `OrderEvent` instances with `FAILED_INVENTORY` or `FAILED_LOGISTICS` to persist & surface issues.
- **Idempotent updates** – inventory entries are keyed by merchant reference (`uq_inventory_order_request`), enabling safe replays.
- **Async isolation** – each stage uses `@Async` event listeners to decouple API latency from user requests.
- **SSE back-pressure** – `EventService` tracks emitters so slow consumers do not block server threads.

Potential hardening (backlog):

- Circuit breaker around Late Logistics.
- Persistent outbox for emitting events after DB commit.
- Dead-letter queue for poisoned events.

---

## API Surface

| Method   | Path                           | Description                                              |
| -------- | ------------------------------ | -------------------------------------------------------- |
| `POST`   | `/api/orders`                  | Create order (returns `202 Accepted` with pending state) |
| `GET`    | `/api/orders/{merchantRef}`    | Retrieve current state, tracking ref, details            |
| `GET`    | `/api/orders`                  | Paginated list of orders (`page`, `size`)                |
| `DELETE` | `/api/orders/{merchantRef}`    | Cancel order (fails if already shipped)                  |
| `GET`    | `/api/events/subscribe`        | SSE stream of `order-update` events for UI               |
| `POST`   | `/api/webhooks/late-logistics` | Carrier webhook ingress                                  |

> Label retrieval is currently proxied through `LateLogisticsClient#getLabel`. Exposing a REST facade (e.g. `GET /api/orders/{merchantRef}/label`) is tracked under [Future Enhancements](#future-enhancements).

### Example Requests

Create order:

```sh
curl -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{
    "merchantId": 42,
    "merchantRef": "MERCHANT-12345",
    "barcode": "TRACK123456",
    "recipient": {
      "name": "Jane Doe",
      "address1": "123 Any Street",
      "address2": "Unit 4",
      "postalCode": "90210",
      "city": "Springfield",
      "country": "US"
    },
    "parcel": {
      "weightGrams": 750,
      "lengthCm": 12.5,
      "widthCm": 8.4,
      "heightCm": 3.2
    }
  }'
```

Subscribe to real-time updates:

```sh
curl -N http://localhost:8080/api/events/subscribe
```

Webhook payload shape is documented in [`WebhookPayload`](src/main/java/com/orderlink/logistic/webhook/WebhookPayload.java).

---

## Local Development

### Prerequisites

- JDK 21+
- Maven 3.9+ (wrapper included)
- PostgreSQL with database `order-link-db` and credentials `admin` / `admin`

### Backend

```sh
./mvnw spring-boot:run
```

Properties live in [`src/main/resources/application.yml`](src/main/resources/application.yml). Sample seed data is under [`src/main/resources/data.sql`](src/main/resources/data.sql).

## Testing

Unit and integration tests cover the event pipeline, webhook ingestion, and API contract:

```sh
./mvnw test
```

Key suites:

- [`OnPendingStateOrderTest`](src/test/java/com/orderlink/event/OnPendingStateOrderTest.java)
- [`OnInventoryReservedStateOrderTest`](src/test/java/com/orderlink/event/OnInventoryReservedStateOrderTest.java)
- [`OrderControllerIntegrationTest`](src/test/java/com/orderlink/integration/OrderControllerIntegrationTest.java)
- [`EventControllerIntegrationTest`](src/test/java/com/orderlink/integration/EventControllerIntegrationTest.java)

Awaitility manages async assertions to ensure background processors complete.

---

## Future Enhancements

- REST endpoint for label retrieval using `LateLogisticsClient#getLabel`.
- Persistent event outbox to guarantee delivery after DB commits.
- Observability (metrics, tracing) for async processors.
- Role-based access control on order endpoints.

