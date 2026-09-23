# Daddy Soda House Backend

Backend foundation and identity service for the **Daddy Soda House** franchise management portal. Built with Java 21 and Spring Boot 3.x, this service manages core backend capabilities including authentication, user roles, security, franchise operations, inventory, orders, billing, and IoT integrations.

---

## 1. Required Software

Ensure the following tools are installed on your system:
- **Java**: OpenJDK / Temurin 21 LTS
- **Build Tool**: Apache Maven 3.9+ (or use the included `./mvnw` wrapper)
- **Containerization**: Docker & Docker Compose (or Colima / Docker Desktop)
- **Database (Optional for local non-Docker runs)**: PostgreSQL 16+

---

## 2. Environment Variables

Database credentials, JWT settings, and initial admin bootstrap configuration are externalized using environment variables. A template is provided in [.env.example](file:///.env.example). Never hardcode secrets in version control.

| Variable Name | Description | Example (Dev) |
|---|---|---|
| `DB_HOST` | Hostname of the PostgreSQL server | `localhost` |
| `DB_PORT` | Port number of the PostgreSQL server | `5432` |
| `DB_NAME` | PostgreSQL database name | `daddy_soda_house` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | `postgres` |
| `JWT_SECRET` | Secret key for HMAC-SHA256 JWT signing (min 256 bits) | `dev_only_jwt_secret_key_minimum_256_bits_for_testing_purposes_only` |
| `JWT_EXPIRATION_MS` | JWT expiration duration in milliseconds (Default: `86400000` / 24h) | `86400000` |
| `APP_IOT_API_KEY` | Secret API key for IoT telemetry authentication | `dev_iot_api_key_placeholder` |
| `INITIAL_ADMIN_NAME` | *(Optional)* Full name for first SUPER_ADMIN account | `Super Administrator` |
| `INITIAL_ADMIN_MOBILE` | *(Optional)* 10-digit mobile number for first SUPER_ADMIN | `9999999999` |
| `INITIAL_ADMIN_EMAIL` | *(Optional)* Email for first SUPER_ADMIN | `admin@daddysoda.com` |
| `INITIAL_ADMIN_PASSWORD` | *(Optional)* Plaintext password (will be BCrypt hashed) | `Admin@12345` |
| `PORT` | *(Optional)* HTTP server port (Default: `8080`) | `8080` |

---

## 3. How to Start PostgreSQL Using Docker Compose

A dedicated `docker-compose.yml` file is provided to run PostgreSQL 16 Alpine with persistent storage in a Docker volume named `daddy_soda_pgdata`.

Start PostgreSQL container:
```bash
docker compose up -d
```
*(Or if using legacy Docker Compose syntax: `docker-compose up -d`)*

Verify database container is running and healthy:
```bash
docker compose ps
```

Stop database container:
```bash
docker compose down
```

---

## 4. How to Start with the Initial Super Admin

On application startup, `SuperAdminBootstrapService` checks if a user with role `SUPER_ADMIN` exists in the database.
- If **no SUPER_ADMIN exists** and the environment variables (`INITIAL_ADMIN_NAME`, `INITIAL_ADMIN_MOBILE`, `INITIAL_ADMIN_PASSWORD`) are present, it creates and hashes the initial administrator account.
- Passwords, secrets, and tokens are **never logged**.
- If any required bootstrap variable is missing or a `SUPER_ADMIN` already exists, the bootstrap step is safely skipped.

### Running locally:
```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=daddy_soda_house
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export JWT_SECRET=dev_only_jwt_secret_key_minimum_256_bits_for_testing_purposes_only
export JWT_EXPIRATION_MS=86400000
export APP_IOT_API_KEY=dev_iot_api_key_placeholder
export INITIAL_ADMIN_NAME="Super Administrator"
export INITIAL_ADMIN_MOBILE="9999999999"
export INITIAL_ADMIN_EMAIL="admin@daddysoda.com"
export INITIAL_ADMIN_PASSWORD="Admin@12345"

./mvnw spring-boot:run
```

---

## 5. API Endpoints & Documentation

| Endpoint | Method | Security | Description |
|---|---|---|---|
| `/actuator/health` | `GET` | Public | Actuator health probes |
| `/api/v1/public/status` | `GET` | Public | Foundation status endpoint |
| `/api/v1/auth/login` | `POST` | Public | Authenticate via 10-digit mobile and password to obtain JWT |
| `/api/v1/auth/me` | `GET` | Authenticated (Bearer) | Retrieve logged-in user profile excluding password |
| `/api/v1/auth/logout` | `POST` | Authenticated (Bearer) | Stateless logout acknowledgment |
| `/api/v1/franchises` | `POST` | SUPER_ADMIN, MANAGER | Create new franchise |
| `/api/v1/franchises` | `GET` | SUPER_ADMIN, MANAGER | List franchises (search, active, pagination) |
| `/api/v1/franchises/{id}` | `GET` | SUPER_ADMIN, MANAGER | Get franchise by ID |
| `/api/v1/franchises/{id}` | `PUT` | SUPER_ADMIN, MANAGER | Update franchise details |
| `/api/v1/franchises/{id}/status` | `PATCH` | SUPER_ADMIN, MANAGER | Toggle franchise status (protected if active outlets exist) |
| `/api/v1/outlets` | `POST` | SUPER_ADMIN, MANAGER | Create outlet under active franchise |
| `/api/v1/outlets` | `GET` | SUPER_ADMIN, MANAGER | List outlets (franchiseId, search, active, pagination) |
| `/api/v1/outlets/{id}` | `GET` | SUPER_ADMIN, MANAGER | Get outlet by ID |
| `/api/v1/outlets/{id}` | `PUT` | SUPER_ADMIN, MANAGER | Update outlet details |
| `/api/v1/outlets/{id}/status` | `PATCH` | SUPER_ADMIN, MANAGER | Toggle outlet status (protected if active machines exist) |
| `/api/v1/machines` | `POST` | SUPER_ADMIN, MANAGER | Create machine under active outlet |
| `/api/v1/machines` | `GET` | SUPER_ADMIN, MANAGER | List machines (outletId, franchiseId, search, active, pagination) |
| `/api/v1/machines/{id}` | `GET` | SUPER_ADMIN, MANAGER | Get machine by ID |
| `/api/v1/machines/{id}` | `PUT` | SUPER_ADMIN, MANAGER | Update machine details |
| `/api/v1/machines/{id}/status` | `PATCH` | SUPER_ADMIN, MANAGER | Toggle machine status |
| `/api/v1/products` | `POST` | SUPER_ADMIN, MANAGER | Create syrup/flavour product |
| `/api/v1/products` | `GET` | SUPER_ADMIN, MANAGER | List products (search, active, pagination) |
| `/api/v1/products/{id}` | `GET` | SUPER_ADMIN, MANAGER | Get product by ID |
| `/api/v1/products/{id}` | `PUT` | SUPER_ADMIN, MANAGER | Update product details |
| `/api/v1/products/{id}/status` | `PATCH` | SUPER_ADMIN, MANAGER | Toggle product status |
| `/api/v1/suppliers` | `POST` | SUPER_ADMIN, MANAGER | Create raw material / syrup supplier |
| `/api/v1/suppliers` | `GET` | SUPER_ADMIN, MANAGER | List suppliers (search, active, pagination) |
| `/api/v1/suppliers/{id}` | `GET` | SUPER_ADMIN, MANAGER | Get supplier by ID |
| `/api/v1/suppliers/{id}` | `PUT` | SUPER_ADMIN, MANAGER | Update supplier details |
| `/api/v1/suppliers/{id}/status` | `PATCH` | SUPER_ADMIN, MANAGER | Toggle supplier status |
| `/api/v1/users` | `POST` | SUPER_ADMIN | Create MANAGER or FRANCHISE_OWNER |
| `/api/v1/users` | `GET` | SUPER_ADMIN | List users (role, franchiseId, active, pagination) |
| `/api/v1/users/{id}` | `PUT` | SUPER_ADMIN | Update user name, email, role, and franchise |
| `/api/v1/users/{id}/status` | `PATCH` | SUPER_ADMIN | Toggle user status (protected for last active SUPER_ADMIN) |
| `/api/v1/my/franchise` | `GET` | FRANCHISE_OWNER | View logged-in owner's active franchise |
| `/api/v1/my/outlets` | `GET` | FRANCHISE_OWNER | View logged-in owner's active outlets |
| `/api/v1/my/machines` | `GET` | FRANCHISE_OWNER | View logged-in owner's active machines |
| `/api/v1/purchases` | `POST` | SUPER_ADMIN, MANAGER | Create confirmed purchase bill & increment warehouse stock |
| `/api/v1/purchases` | `GET` | SUPER_ADMIN, MANAGER | List purchase bills (supplier, status, dates, search, pagination) |
| `/api/v1/purchases/{id}` | `GET` | SUPER_ADMIN, MANAGER | Get purchase bill details with item stock balance |
| `/api/v1/purchases/{id}/cancel` | `POST` | SUPER_ADMIN, MANAGER | Cancel purchase bill & reverse warehouse stock |
| `/api/v1/inventory/warehouse` | `GET` | SUPER_ADMIN, MANAGER | List warehouse stock (search, IN_STOCK, OUT_OF_STOCK, pagination) |
| `/api/v1/inventory/warehouse/{productId}` | `GET` | SUPER_ADMIN, MANAGER | View product stock balance & top 20 recent stock movements |
| `/api/v1/inventory/movements` | `GET` | SUPER_ADMIN, MANAGER | List stock movement audit trail (filters, pagination) |
| `/api/v1/inventory/adjustments` | `POST` | SUPER_ADMIN, MANAGER | Perform manual stock adjustment (ADJUSTMENT_IN or ADJUSTMENT_OUT) |
| `/swagger-ui.html` | `GET` | Dev Profile Only | Swagger UI interactive documentation |
| `/v3/api-docs` | `GET` | Dev Profile Only | OpenAPI specification JSON |

---

## 6. Master Data Module Overview

The Master Data module establishes core business entities required for subsequent purchase, inventory, sales, and IoT operations.

### A. Hierarchy and Dependency Order
All operational physical entities adhere to a strict 3-tier hierarchy:
```
Franchise (Business Entity)
  └── Outlet (Physical Shop / Location)
        └── Machine (Dispensing Soda Unit with Optional IoT Device ID)
```
- **Creation Guardrail**: An Outlet can only be created under an active Franchise. A Machine can only be created under an active Outlet.
- **Deactivation Guardrail**: A Franchise cannot be deactivated while it has active Outlets. An Outlet cannot be deactivated while it has active Machines.

### B. User Role and Franchise Assignment Rules
- **SUPER_ADMIN**: Full access to all master records and user administration. Cannot be created via API. System prevents deactivating or demoting the last active `SUPER_ADMIN`. Must **not** have a `franchiseId`.
- **MANAGER**: Can create, view, and update master records (Franchises, Outlets, Machines, Products, Suppliers). Must **not** have a `franchiseId`. Cannot access user administration (`/api/v1/users` returns HTTP 403).
- **FRANCHISE_OWNER**: Must be assigned to an active, valid `franchiseId`. Cannot access full master lists or creation/update APIs (`/api/v1/franchises`, `/outlets`, `/machines`, `/products`, `/suppliers` return HTTP 403). Can only view their own assigned franchise, outlets, and machines via the `/api/v1/my/**` self-service endpoints.

### C. Example Product: "Orange Syrup, 5 litre bottle"
```json
{
  "itemCode": "SYR-ORG-05L",
  "flavourName": "Orange Syrup",
  "bottleSizeLitre": 5.00,
  "defaultSellingPrice": 650.00,
  "description": "Standard 5 Litre Orange Syrup Can for Soda Dispensing"
}
```

---

## 7. Purchase Bill, Warehouse Inventory, and Stock Movement Module Overview (BE-04)

This module handles physical stock-inward from suppliers, centralized warehouse inventory balances, and an auditable ledger of all stock changes.

### A. Workflow and Stock Inward Rules
1. **Manual Bill Entry**: A manager physically receives a supplier bill and inputs the details.
2. **Direct Confirmation**: Purchase bills are created directly in `CONFIRMED` status. Drafts are intentionally excluded in this version.
3. **Automatic Stock Inward**: Upon confirmation, each product's available quantity in `inventory_stock` is immediately increased, and a `PURCHASE_IN` movement record is written.
4. **Pessimistic Locking**: `SELECT ... FOR UPDATE` is acquired on the product's inventory stock row during stock modifications to guarantee accurate concurrency and prevent race conditions.
5. **Atomic Transactions**: If any single item in a multi-item purchase bill is invalid, the entire purchase and all associated inventory increments roll back completely.

### B. Why Purchase Price is Intentionally Not Stored
In Daddy Soda House architecture:
- Purchase bills in this module are **pure stock-inward records**.
- Procurement costs, supplier payment terms, tax ledgers, and accounting entries are segregated from operational warehouse inventory.
- Selling prices are managed at the product master level and can be customized during franchise order creation in subsequent modules.

### C. Cancellation vs. Editing Confirmed Bills
To preserve immutable audit history:
- Confirmed purchase bills **cannot be edited**.
- If a mistake is made, the bill must be cancelled (`POST /api/v1/purchases/{id}/cancel`) with a mandatory reason (up to 500 characters).
- **Reversal Guardrail**: Cancellation reverses the stock inward by creating `PURCHASE_CANCEL_REVERSAL` movements. If current warehouse inventory is insufficient to cover the reversal (e.g. stock was adjusted out), cancellation is **rejected** with HTTP 400 to prevent stock from ever becoming negative.

### D. Stock Movement Audit Trail
Every single quantity change writes an immutable `stock_movements` row capturing:
- `movement_type`: `PURCHASE_IN`, `PURCHASE_CANCEL_REVERSAL`, `ADJUSTMENT_IN`, `ADJUSTMENT_OUT`, `DELIVERY_OUT` (reserved for sales module)
- `quantity_in` / `quantity_out`
- `balance_after`
- `reference_type` (`PURCHASE_ENTRY`, `MANUAL_ADJUSTMENT`, etc.)
- `reference_number` and `reference_id`
- Timestamp and user ID of the actor

### E. Example Payloads

#### 1. Create Purchase Bill (`POST /api/v1/purchases`)
```json
{
  "supplierId": "e2c34bf6-5a4e-4f76-88a2-a9b0c201d411",
  "supplierBillNumber": "BILL-458",
  "billDate": "2026-09-23",
  "description": "Monthly syrup stock received",
  "items": [
    {
      "productId": "748805a4-23db-4fc7-bf98-04f5e718b5ef",
      "quantity": 5.000
    },
    {
      "productId": "91ba2d31-cf19-45d2-965a-52643a6d71b3",
      "quantity": 3.000
    }
  ]
}
```

#### 2. Cancel Purchase Bill (`POST /api/v1/purchases/{id}/cancel`)
```json
{
  "reason": "Incorrect quantity entered on supplier bill; re-entering corrected invoice"
}
```

#### 3. Manual Stock Adjustment (`POST /api/v1/inventory/adjustments`)
```json
{
  "productId": "748805a4-23db-4fc7-bf98-04f5e718b5ef",
  "adjustmentType": "ADJUSTMENT_IN",
  "quantity": 2.000,
  "note": "Physical warehouse audit reconciliation surplus"
}
```

---

## 8. Authentication & Testing Examples

### A. Login via curl
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "mobile": "9999999999",
    "password": "Admin@12345"
  }'
```

### B. Testing via Swagger UI
1. Start the application with the `dev` profile active:
   ```bash
   ./mvnw spring-boot:run
   ```
2. Open Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).
3. Log in with `POST /api/v1/auth/login` to obtain a JWT.
4. Click the green **Authorize** button in Swagger UI and input: `Bearer <your_token>`.
5. Execute operations:
   - Create Master entities (Suppliers, Products).
   - Enter supplier purchase bills (`POST /api/v1/purchases`).
   - Query warehouse stock balance (`GET /api/v1/inventory/warehouse`).
   - Check stock audit movements (`GET /api/v1/inventory/movements`).
   - Perform manual stock adjustments (`POST /api/v1/inventory/adjustments`).

---

## 9. Flyway Database Migrations

Database versioning is managed with **Flyway**:
- `V1__baseline.sql`: Initializes the `application_metadata` tracking table.
- `V2__create_users_and_roles.sql`: Creates `users` table with UUID primary key, 10-digit mobile uniqueness check, role check (`SUPER_ADMIN`, `MANAGER`, `FRANCHISE_OWNER`), and indexes on mobile, role, and active status.
- `V3__create_master_data_tables.sql`: Creates `franchises`, `outlets`, `machines`, `products`, and `suppliers` tables with foreign keys, uniqueness constraints, check constraints (`bottle_size_litre > 0`, `default_selling_price >= 0`), and indexes.
- `V4__add_franchise_reference_to_users.sql`: Adds nullable `franchise_id UUID` foreign key to `users` referencing `franchises(id)` with index.
- `V5__create_purchase_inventory_tables.sql`: Creates `purchase_entries`, `purchase_items`, `inventory_stock`, and `stock_movements` tables with foreign keys, uniqueness constraints, positive quantity checks, non-negative balance checks, and indexes.

---

## 10. Testing

To run the full unit and integration test suite:
```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw clean test
```

Tests run in an isolated in-memory H2 database (PostgreSQL compatibility mode) with Flyway migrations enabled, verifying:
- Application context initialization
- Public status and Actuator health endpoints
- Authentication, JWT issuance, and expiration validation
- Master data CRUD and search/pagination filters
- Hierarchy constraints (Franchise → Outlet → Machine)
- Deactivation guards preventing orphaned active dependents
- Role-based authorization rules and self-service `/api/v1/my/**` isolation
- Protection for the last active `SUPER_ADMIN` account
- Purchase bill creation, stock-inward increment, and stock movement logging
- Multi-item atomic transactions with rollback on invalid items
- Duplicate product in single purchase rejection and duplicate supplier bill number validation
- Purchase cancellation with stock decrement and negative stock prevention guardrails
- Manual stock adjustments (`ADJUSTMENT_IN` / `ADJUSTMENT_OUT`) and validation against stock going below zero
- Role protection: `FRANCHISE_OWNER` receiving HTTP 403 on all purchase and warehouse inventory endpoints


