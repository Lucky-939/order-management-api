# Order Management REST API

## Overview
A Spring Boot REST API for managing order lifecycles and inventory reservations. The application supports placing orders with stock decrements and price snapshotting, fetching order details, paginated order listing, status transitions, and order cancellation with stock restoration. Optimistic locking (@Version) detects conflicting concurrent updates. In the tested scenarios it prevented overselling and lost updates.

---

## Stack
- **Java**: 17
- **Framework**: Spring Boot 3.3.4 (Spring Web, Spring Data JPA, Validation)
- **Database**: MySQL 8.0 with InnoDB
- **Driver**: `com.mysql:mysql-connector-j:jar:8.3.0:runtime`
- **Testing**: JUnit 5 (Jupiter), Mockito, AssertJ (via `spring-boot-starter-test`)
- **Build Tool**: Apache Maven

---

## Prerequisites
- JDK 17 or higher
- Apache Maven 3.8+
- MySQL Server 8.0 running on `localhost:3306`

---

## Setup and Run

Follow these setup steps in order:

### 1. Create Database
Connect to your local MySQL instance and create the database:
```sql
CREATE DATABASE order_db;
```

### 2. Configure Environment Variables
Set database credentials as environment variables (`DB_USERNAME` and `DB_PASSWORD`). Never commit credentials to source control.

**PowerShell (Windows):**
```powershell
$env:DB_USERNAME="<your-db-username>"
$env:DB_PASSWORD="<your-db-password>"
```

**Bash / macOS / Linux:**
```bash
export DB_USERNAME="<your-db-username>"
export DB_PASSWORD="<your-db-password>"
```

### 3. Start the App Once (Creates Schema)
Start the application once so Hibernate inspects the entity models and creates the tables in `order_db` (`spring.jpa.hibernate.ddl-auto=update`):

**PowerShell (Windows):**
```powershell
mvn spring-boot:run
```
*(Wait until `Started OrdersApplication` appears in the logs, then stop the process with `Ctrl+C`)*

**Bash / macOS / Linux:**
```bash
mvn spring-boot:run
```
*(Wait until `Started OrdersApplication` appears in the logs, then stop the process with `Ctrl+C`)*

### 4. Run seed.sql
Now that tables exist, seed the initial inventory:

**PowerShell (Windows):**
```powershell
$env:MYSQL_PWD=$env:DB_PASSWORD; mysql -u $env:DB_USERNAME order_db -e "source seed.sql;"
```

**Bash / macOS / Linux:**
```bash
mysql -u "$DB_USERNAME" -p order_db < seed.sql
```

### 5. Start and Use the API
Launch the application:

**PowerShell / Bash:**
```bash
mvn spring-boot:run
# Alternatively:
mvn clean package -DskipTests
java -jar target/orders-0.0.1-SNAPSHOT.jar
```

The REST API will be available on `http://localhost:8080`.

---

## Endpoints

| Method | Path | Success Status | Error Statuses |
|---|---|---|---|
| `POST` | `/orders` | `201 Created` | `400 Bad Request`, `404 Not Found`, `409 Conflict`, `500 Internal Server Error` |
| `GET` | `/orders/{id}` | `200 OK` | `400 Bad Request`, `404 Not Found`, `500 Internal Server Error` |
| `GET` | `/orders` | `200 OK` | `400 Bad Request`, `500 Internal Server Error` |
| `PATCH` | `/orders/{id}/status` | `200 OK` | `400 Bad Request`, `404 Not Found`, `409 Conflict`, `500 Internal Server Error` |
| `DELETE` | `/orders/{id}` | `204 No Content` | `400 Bad Request`, `404 Not Found`, `409 Conflict`, `500 Internal Server Error` |

### Sample: POST /orders

**Request:**
`POST /orders`
```json
{
  "customerName": "Alice Smith",
  "items": [
    {
      "productId": 1,
      "quantity": 2
    },
    {
      "productId": 2,
      "quantity": 1
    }
  ]
}
```

**Response (`201 Created`):**
```json
{
  "id": 1,
  "customerName": "Alice Smith",
  "status": "PLACED",
  "createdAt": "2026-10-13T02:47:26.4092808",
  "totalAmount": 209.48,
  "items": [
    {
      "productId": 1,
      "productName": "Mechanical Keyboard",
      "quantity": 2,
      "unitPrice": 89.99
    },
    {
      "productId": 2,
      "productName": "Wireless Mouse",
      "quantity": 1,
      "unitPrice": 29.50
    }
  ]
}
```

### Sample: Error Response (`400 Bad Request`)
```json
{
  "timestamp": "2026-10-13T02:47:53.9117915",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/orders",
  "fieldErrors": [
    {
      "field": "customerName",
      "message": "customerName must not be blank"
    },
    {
      "field": "items[0].quantity",
      "message": "quantity must be at least 1"
    }
  ]
}
```

---

## Error Format (`ApiError`)
All non-2xx responses are mapped into a standardized `ApiError` envelope by `GlobalExceptionHandler`:
- `timestamp`: ISO-8601 timestamp of when the error occurred
- `status`: HTTP status code integer (e.g. `400`, `404`, `409`, `500`)
- `error`: HTTP status reason phrase (e.g. `"Bad Request"`, `"Conflict"`)
- `message`: Sanitized error description (never exposes entity class names or database internals)
- `path`: Request URI that triggered the failure
- `fieldErrors`: Optional list of field-level validation errors (omitted if null via `@JsonInclude(NON_NULL)`), deterministically sorted by field name then message.

---

## Business Rules
1. **Duplicate Items**: A single order request cannot specify duplicate `productId`s; violates semantic order validation and returns `400 Bad Request`.
2. **Inventory Decrement & Snapshots**: When placing an order, inventory is decremented sequentially. Each `OrderItem` stores the product's `price` at purchase time to isolate existing orders from future product price changes.
3. **Atomic Rollback**: If stock for any item in an order is insufficient, `InsufficientStockException` is thrown, returning `409 Conflict`, and database transaction rollback reverses any prior stock decrements within the request.
4. **Order Status Transitions**: Strict linear lifecycle enforced:
   - `PLACED` → `SHIPPED`
   - `SHIPPED` → `DELIVERED`
   - `DELIVERED` is terminal (no further transitions).
   - Skipping stages or requesting same-status transitions returns `409 Conflict`.
5. **Order Cancellation**: Only orders in `PLACED` status can be cancelled (`DELETE /orders/{id}`). Cancellation restores inventory stock for each line item and hard-deletes the order record (cascading to order items).

---

## Concurrency
### Optimistic Locking
Optimistic locking is implemented via JPA `@Version private long version;` on both `Product` and `Order` entities:
- **On `Product`**: Prevented overselling in the tested scenario. When multiple concurrent orders attempt to purchase the same inventory item, Hibernate includes `WHERE id = ? AND version = ?` on update. If another transaction commits an update first, zero rows match and Spring throws `ObjectOptimisticLockingFailureException`.
- **On `Order`**: Protects against concurrent PATCH-vs-PATCH status updates and DELETE-vs-DELETE cancellation races, both of which were tested.

### Concurrency Verification (race.ps1)
- **Baseline (without locking)**: 3 rounds of 10 simultaneous orders against Product 3 (stock 1) resulted in **10 successful orders per round** (30 orders placed across 3 units of stock), illustrating lost updates and overselling.
- **Place Race (with optimistic locking)**: 5 rounds of 10 simultaneous orders against Product 3 (stock 1) resulted in **exactly 1 success (`201 Created`) and 9 conflicts (`409 Conflict`) per round**. Stock ended at `0` every round, and total items sold equaled exactly 5 across 5 rounds.
- **Cancel Race**: 10 simultaneous `DELETE /orders/{id}` requests on a placed order resulted in **exactly 1 success (`204 No Content`) and 9 conflicts (`409 Conflict`)**. Stock was restored by exactly the ordered quantity once, and the order was removed.
- **Patch Race**: 10 simultaneous `PATCH /orders/{id}/status` requests resulted in **exactly 1 success (`200 OK`) and 9 conflicts (`409 Conflict`)**. Order status updated to `SHIPPED` with incremented entity version.

### Limitations
- **No Automatic Retry**: If an optimistic lock conflict occurs, the request fails immediately with `409 Conflict` (`"The request conflicted with another update to the same data. Please retry."`). Retrying is the client's responsibility.
- **Deadlock and Lock Contention**: While optimistic locking detects concurrent modifications at commit/flush time, underlying MySQL InnoDB row-level locks can still experience deadlocks or lock wait timeouts under intense contention. These are captured by Spring's `ConcurrencyFailureException` hierarchy and similarly return `409 Conflict` (mapped by exception hierarchy, not tested).

---

## Testing
Unit tests run purely in-memory with JUnit 5 and Mockito. No Spring context or database connection is initialized.

Run tests:
```bash
mvn test
```

### Test Coverage
- `OrderStatusTest`: Parameterized tests across all 9 `(from, to)` status transition pairs verifying allowable state transitions, plus null target validation.
- `OrderServiceTest`: 10 isolated Mockito unit tests validating:
  1. Valid order placement decrements stock, snapshots price, and calculates total amount.
  2. Insufficient stock on secondary items aborts before saving.
  3. Duplicate product IDs reject without calling repositories.
  4. Unknown product lookups throw `ResourceNotFoundException`.
  5. Illegal order status transitions reject and leave status unchanged.
  6. Legal transitions update status and rely on transaction dirty checking.
  7. Unknown order lookups on status update throw `ResourceNotFoundException`.
  8. Cancellation of placed orders restores inventory stock and deletes the order.
  9. Cancellation attempts on shipped orders reject and leave stock untouched.
  10. Unknown order lookups on cancellation throw `ResourceNotFoundException`.

### What Mockito Tests Do NOT Prove
- **Database Rollbacks**: Mockito unit tests mock repositories and verify that `orderRepository.save()` was never invoked; they cannot verify that MySQL actually rolled back earlier flushed `UPDATE products` statements upon transaction abort. Real transaction rollback was verified against live MySQL in the manual rollback check.
- **Optimistic Locking**: Mockito stubs do not execute Hibernate version increment checks or simulate database race conditions; optimistic locking and multi-threaded concurrency protections were verified using real concurrent HTTP requests via the concurrent-request script (scripts/race.ps1).

---

## Design Decisions and Known Limitations
- **Hard Deletes**: Order cancellation performs a physical SQL delete on the order and cascade-deletes items. No soft-delete column or `CANCELLED` status is maintained.
- **List Endpoint Summaries**: `GET /orders` returns lightweight order summaries (`OrderSummaryResponse`) excluding line items to avoid large payload transfer on paginated listings.
- **Hibernate DDL Auto**: `spring.jpa.hibernate.ddl-auto=update` is configured for development schema generation. Production deployments should disable ddl-auto and utilize migration tools such as Flyway or Liquibase.
- **Authentication**: No authentication or authorization is configured.
- **No Product Endpoints**: Products are pre-seeded directly into MySQL via `seed.sql`.
- **Idempotency on Status PATCH**: Attempting to transition an order to its current status (e.g. `SHIPPED` → `SHIPPED`) is rejected with `409 Conflict` per transition state rules.
