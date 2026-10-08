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
| `DB_NAME` | PostgreSQL database name | `Daddy_soda_house` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | `postgres` |
| `JWT_SECRET` | Secret key for HMAC-SHA256 JWT signing (min 256 bits) | `dev_only_jwt_secret_key_minimum_256_bits_for_testing_purposes_only` |
| `JWT_EXPIRATION_MS` | JWT expiration duration in milliseconds (Default: `86400000` / 24h) | `86400000` |
| `APP_IOT_API_KEY` | Secret API key for IoT telemetry authentication | `dev_iot_api_key_placeholder` |
| `IOT_OFFLINE_THRESHOLD_MINUTES` | Inactivity window (in minutes) before machine is marked OFFLINE (Default: `30`) | `30` |
| `COMPANY_NAME` | Company name shown on invoice header | `Daddy Soda House Pvt Ltd` |
| `COMPANY_ADDRESS` | Company address printed on invoice PDF | `123 Beverage Park, Industrial Zone, Pune, Maharashtra 411001` |
| `COMPANY_MOBILE` | Company contact phone printed on invoice PDF | `+91 98220 00000` |
| `COMPANY_EMAIL` | Company support email printed on invoice PDF | `billing@Daddysodahouse.com` |
| `COMPANY_GSTIN` | Company GSTIN (printed only on GST invoices) | `27AAAAA0000A1Z5` |
| `DEFAULT_GST_RATE_PERCENT` | Default GST rate percent when omitted in GST invoices | `18.00` |
| `INVOICE_STORAGE_PATH` | Local directory for storing generated invoice PDFs | `./storage/invoices` |
| `WHATSAPP_ENQUIRY_NUMBER` | Target phone number (e.g. 919876543210) for website enquiry WhatsApp click-to-chat URL | `919876543210` |
| `INITIAL_ADMIN_NAME` | *(Optional)* Full name for first SUPER_ADMIN account | `Super Administrator` |
| `INITIAL_ADMIN_MOBILE` | *(Optional)* 10-digit mobile number for first SUPER_ADMIN | `9999999999` |
| `INITIAL_ADMIN_EMAIL` | *(Optional)* Email for first SUPER_ADMIN | `admin@Daddysoda.com` |
| `INITIAL_ADMIN_PASSWORD` | *(Optional)* Plaintext password (will be BCrypt hashed) | `Admin@12345` |
| `PORT` | *(Optional)* HTTP server port (Default: `8080`) | `8080` |

---

## 3. How to Start PostgreSQL Using Docker Compose

A dedicated `docker-compose.yml` file is provided to run PostgreSQL 16 Alpine with persistent storage in a Docker volume named `Daddy_soda_pgdata`.

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
export INITIAL_ADMIN_EMAIL="admin@Daddysoda.com"
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
| `/api/v1/orders` | `POST` | SUPER_ADMIN, MANAGER, FRANCHISE_OWNER | Create sales order (DRAFT or PLACED) |
| `/api/v1/orders` | `GET` | SUPER_ADMIN, MANAGER | List sales orders (franchise, status, date filters, pagination) |
| `/api/v1/orders/{id}` | `GET` | SUPER_ADMIN, MANAGER, FRANCHISE_OWNER | Get sales order details (franchise owner scoped to own franchise) |
| `/api/v1/orders/{id}` | `PUT` | SUPER_ADMIN, MANAGER, FRANCHISE_OWNER | Update order (DRAFT or PLACED; franchise owner DRAFT only) |
| `/api/v1/orders/{id}/place` | `POST` | SUPER_ADMIN, MANAGER, FRANCHISE_OWNER | Transition order from DRAFT to PLACED |
| `/api/v1/orders/{id}/prepare` | `POST` | SUPER_ADMIN, MANAGER | Mark order as PREPARED |
| `/api/v1/orders/{id}/deliver` | `POST` | SUPER_ADMIN, MANAGER | Deliver order and deduct warehouse stock atomically |
| `/api/v1/orders/{id}/cancel` | `POST` | SUPER_ADMIN, MANAGER, FRANCHISE_OWNER | Cancel order (reason mandatory; stock untouched) |
| `/api/v1/orders/{id}` | `DELETE` | SUPER_ADMIN, MANAGER, FRANCHISE_OWNER | Physically delete DRAFT order only |
| `/api/v1/my/orders` | `GET` | FRANCHISE_OWNER | Retrieve orders belonging to authenticated owner's franchise |
| `/api/v1/invoices` | `POST` | SUPER_ADMIN, MANAGER | Create GST or NORMAL invoice for DELIVERED order |
| `/api/v1/invoices` | `GET` | SUPER_ADMIN, MANAGER, FRANCHISE_OWNER | List invoices (filters, overdue, pagination; scoped for owner) |
| `/api/v1/invoices/{id}` | `GET` | SUPER_ADMIN, MANAGER, FRANCHISE_OWNER | Get invoice details, items, tax, payments, and PDF URL |
| `/api/v1/invoices/{id}/due-date` | `PATCH` | SUPER_ADMIN, MANAGER | Update credit due date with mandatory audit reason |
| `/api/v1/invoices/{id}/payments` | `POST` | SUPER_ADMIN, MANAGER | Record manual payment collection against pending invoice balance |
| `/api/v1/invoices/{id}/payments` | `GET` | SUPER_ADMIN, MANAGER, FRANCHISE_OWNER | List payment collection history for invoice |
| `/api/v1/invoices/{id}/download` | `GET` | SUPER_ADMIN, MANAGER, FRANCHISE_OWNER | Download generated invoice PDF (regenerates dynamically if missing) |
| `/api/v1/my/invoices` | `GET` | FRANCHISE_OWNER | Self-service list of authenticated owner's franchise invoices |
| `/api/v1/public/enquiries` | `POST` | Public | Submit franchise enquiry & obtain prefilled WhatsApp click-to-chat URL |
| `/api/v1/enquiries` | `GET` | SUPER_ADMIN, MANAGER | List enquiries (status, search, date filters, pagination) |
| `/api/v1/enquiries/{id}` | `GET` | SUPER_ADMIN, MANAGER | Get enquiry details by ID |
| `/api/v1/enquiries/{id}` | `PATCH` | SUPER_ADMIN, MANAGER | Update enquiry status (NEW -> CONTACTED -> CLOSED) & admin notes |
| `/api/v1/dashboard/summary` | `GET` | SUPER_ADMIN, MANAGER | Operational summary metrics for the current calendar month |
| `/api/v1/dashboard/dues` | `GET` | SUPER_ADMIN, MANAGER, FRANCHISE_OWNER | Outstanding invoice dues (OVERDUE, DUE_TODAY, UPCOMING) with severity indicators (RED, AMBER, NORMAL) |
| `/api/v1/dashboard/franchise-summary` | `GET` | FRANCHISE_OWNER | Personalized dashboard summary for authenticated franchise owner |
| `/api/v1/reports/monthly/purchases/download` | `GET` | SUPER_ADMIN, MANAGER | Download monthly purchase entries as CSV (strict YYYY-MM) |
| `/api/v1/reports/monthly/orders/download` | `GET` | SUPER_ADMIN, MANAGER, FRANCHISE_OWNER | Download monthly sales orders as CSV (strict YYYY-MM; scoped for owner) |
| `/api/v1/reports/monthly/invoices/download` | `GET` | SUPER_ADMIN, MANAGER, FRANCHISE_OWNER | Download monthly invoices as CSV (strict YYYY-MM; scoped for owner) |
| `/api/v1/reports/monthly/dues/download` | `GET` | SUPER_ADMIN, MANAGER, FRANCHISE_OWNER | Download monthly credit dues as CSV (strict YYYY-MM; scoped for owner) |
| `/api/v1/iot/events` | `POST` | Public (X-API-KEY header) | Ingest IoT soda machine drink dispense or heartbeat event |
| `/api/v1/iot/events` | `GET` | SUPER_ADMIN, MANAGER | Search and filter IoT telemetry event records (paginated) |
| `/api/v1/iot/events/{id}` | `GET` | SUPER_ADMIN, MANAGER | View complete IoT event details with line items and raw JSON payload |
| `/api/v1/iot/machines/status` | `GET` | SUPER_ADMIN, MANAGER | View connectivity status (ONLINE, OFFLINE, NEVER_CONNECTED) for all active machines |
| `/api/v1/manual-sales` | `POST` | SUPER_ADMIN, MANAGER, FRANCHISE_OWNER | Record manual end-of-day counter reading for flavour sales |
| `/api/v1/manual-sales` | `GET` | SUPER_ADMIN, MANAGER, FRANCHISE_OWNER | Search and filter manual daily sales entries (scoped for owner) |
| `/api/v1/manual-sales/{id}` | `PUT` | SUPER_ADMIN, MANAGER, FRANCHISE_OWNER | Correct manual daily sales entry (scoped for owner) |
| `/api/v1/manual-sales/{id}` | `DELETE` | SUPER_ADMIN, MANAGER, FRANCHISE_OWNER | Delete manual daily sales entry (scoped for owner) |
| `/api/v1/reports/consumption/summary` | `GET` | SUPER_ADMIN, MANAGER, FRANCHISE_OWNER | Retrieve aggregated IoT syrup/soda usage and manual sales numbers |
| `/api/v1/reports/consumption/download` | `GET` | SUPER_ADMIN, MANAGER, FRANCHISE_OWNER | Download consumption telemetry and manual sales data as CSV |
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

## 8. Franchise Sales Orders, Delivery & Stock Deduction (BE-05)

### A. Order Status Lifecycle
```
  [DRAFT] ───(place)───> [PLACED] ───(prepare)───> [PREPARED] ───(deliver)───> [DELIVERED]
     │                      │
     ├──(cancel)──> [CANCELLED]
     │                      │
     └──(delete) [PHYSICAL REMOVAL]
```
- **`DRAFT`**: Order is being prepared by franchise owner or manager. Can be edited, placed, cancelled, or deleted. No stock affected.
- **`PLACED`**: Order confirmed for processing. Can be edited, marked prepared, marked delivered, or cancelled. Cannot be deleted. No stock affected.
- **`PREPARED`**: Order packed and ready for delivery dispatch. Can be marked delivered. Cannot be edited, cancelled, or deleted. No stock affected.
- **`DELIVERED`**: Final confirmed state. Warehouse stock is deducted atomically using pessimistic locks. Order becomes strictly **immutable** (cannot be edited, cancelled, or deleted).
- **`CANCELLED`**: Cancelled order. Reason is mandatory. Stock is untouched.

### B. Core Business Rules
1. **Franchise Ownership Isolation**:
   - `FRANCHISE_OWNER` can create and view orders strictly for their own mapped franchise (`/api/v1/my/orders` or `/api/v1/orders/{id}`).
   - Attempting to order on behalf of another franchise or view another franchise's order is rejected with `400 Bad Request` or `403 Forbidden`.
   - `SUPER_ADMIN` and `MANAGER` can manage orders for any active franchise.
2. **Pricing Rules & Snapshot**:
   - `FRANCHISE_OWNER` cannot manually specify unit prices; unit price is auto-filled from `Product.default_selling_price`.
   - `SUPER_ADMIN` and `MANAGER` may override unit prices or allow auto-fill from Product Master.
   - Price snapshot is stored on `sales_order_items` (`unit_price`, `line_total`) to protect historical accounting from future price catalog adjustments.
3. **Delivery-Time Stock Deduction & Atomic Rollback**:
   - **No stock reservation** occurs when an order is created, placed, or prepared.
   - Warehouse stock deduction occurs **strictly at delivery time** (`POST /api/v1/orders/{id}/deliver`).
   - Evaluated under a pessimistic write lock (`@Lock(LockModeType.PESSIMISTIC_WRITE)`).
   - Generates one `DELIVERY_OUT` audit record in `stock_movements` per item.
4. **No Partial Delivery in V1**:
   - Delivery is full-order only.
   - If any ordered syrup product has insufficient warehouse stock, the entire delivery is rejected and rolls back atomically (0 stock deducted).
5. **Deletion & Cancellation Rules**:
   - Only `DRAFT` orders can be physically deleted.
   - `DELIVERED` orders cannot be edited, cancelled, or deleted.
   - Cancellation requires a mandatory reason (`reason` up to 500 characters).

### C. Example Order Payloads

#### 1. Manager Creating Order with Manual Price Override (`POST /api/v1/orders`)
```json
{
  "franchiseId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "orderDate": "2026-09-23",
  "status": "PLACED",
  "notes": "Deliver along with next route",
  "items": [
    {
      "productId": "11111111-1111-1111-1111-111111111111",
      "quantity": 5.000,
      "unitPrice": 420.00
    },
    {
      "productId": "22222222-2222-2222-2222-222222222222",
      "quantity": 3.000,
      "unitPrice": 400.00
    }
  ]
}
```

#### 2. Franchise Owner Creating Order with Auto-Filled Catalog Pricing (`POST /api/v1/orders`)
```json
{
  "orderDate": "2026-09-23",
  "status": "PLACED",
  "notes": "Please deliver this week",
  "items": [
    {
      "productId": "11111111-1111-1111-1111-111111111111",
      "quantity": 5.000
    }
  ]
}
```

#### 3. Cancelling Order with Mandatory Reason (`POST /api/v1/orders/{id}/cancel`)
```json
{
  "reason": "Franchise entered order by mistake; requested cancellation"
}
```

---

## 9. Invoice, GST/Normal Bill, Credit Due Date, PDF, and Payment Collection (BE-06)

This module handles official billing for delivered franchise sales orders, tax calculations, credit due-date control, downloadable PDF invoices via OpenPDF, and manual payment collection entries.

### A. Core Billing & Business Rules
1. **Delivered Order Prerequisite**: An invoice can be generated **only** for a sales order in `DELIVERED` status. Non-delivered orders return HTTP 400.
2. **One-to-One Invoice Relationship**: Each sales order can have exactly one invoice (`UNIQUE(sales_order_id)` constraint).
3. **Item & Price Snapshot Immutability**: Line items are permanently copied from the delivered sales order items into `invoice_items` at generation time. Any subsequent changes to master product prices or catalog descriptions do not affect the finalized invoice snapshot.
4. **GST vs. Normal Billing Rules**:
   - **NORMAL Invoice**:
     - `gstRatePercent` = 0.00
     - `gstAmount` = 0.00
     - `totalAmount` = `subtotalAmount`
     - Invoice number format: `INV-YYYYMM-XXXXX`
     - GSTIN is omitted from the PDF header
   - **GST Invoice**:
     - Unit prices from order are treated as prices **excluding GST**.
     - `gstRatePercent`: optional; defaults to `DEFAULT_GST_RATE_PERCENT` (18.00%) from config if omitted.
     - `gstAmount` = `round(subtotalAmount * gstRatePercent / 100, 2)`
     - `totalAmount` = `subtotalAmount + gstAmount`
     - Invoice number format: `GST-YYYYMM-XXXXX`
     - Company GSTIN is clearly displayed on the invoice header
5. **Payment Terms & Due Date Rules**:
   - `FULL_PAYMENT`: `dueDate` must be `null`.
   - `CREDIT` or `PARTIAL_PAYMENT`:
     - Initial due date defaults to `invoiceDate + 10 days` if omitted.
     - Manager / Super Admin may explicitly supply a valid due date (must not be before `invoiceDate`).
6. **Due Date Extension**:
   - Controlled via `PATCH /api/v1/invoices/{id}/due-date`.
   - Allowed only when `balanceAmount > 0` and payment term is `CREDIT` or `PARTIAL_PAYMENT`.
   - Reason is mandatory (up to 500 characters) for audit trail.
7. **Manual Payment Collection**:
   - Recorded by `SUPER_ADMIN` or `MANAGER` via `POST /api/v1/invoices/{id}/payments`.
   - Supports `CASH`, `UPI`, `BANK_TRANSFER`, `CHEQUE`, and `OTHER`.
   - Payment amount must be positive and cannot exceed current pending balance (overpayment rejected).
   - Recalculates `paidAmount`, `balanceAmount`, and updates `paymentStatus` atomically.
   - Triggers dynamic PDF regeneration so the printed bill always reflects latest payments and balance.
8. **Payment Status Logic**:
   - `PAID`: `balanceAmount == 0.00`
   - `UNPAID`: `paidAmount == 0.00` and (current date <= `dueDate` or `dueDate` is null)
   - `PARTIALLY_PAID`: `paidAmount > 0.00` and `balanceAmount > 0.00` and (current date <= `dueDate`)
   - `OVERDUE`: `balanceAmount > 0.00` and `dueDate != null` and `dueDate < current date`
9. **Daily Scheduled Overdue Refresh**:
   - A background cron task (`InvoiceOverdueScheduler`) runs every day at 12:05 AM server time (`0 5 0 * * ?`) to transition eligible invoices to `OVERDUE`.
   - Real-time status recalculation also runs dynamically on invoice retrieval so queries are always up to date even before the scheduler fires.
10. **Server-Side PDF Generation & Download**:
    - Built using `OpenPDF 2.0.3`.
    - Rendered to `INVOICE_STORAGE_PATH/invoice-number.pdf` with company branding, tax summary, customer franchise details, item tables, and payment breakdown.
    - Filesystem paths are kept strictly internal and never exposed to the client.
    - Protected download endpoint: `GET /api/v1/invoices/{id}/download`. If the PDF file is missing on disk, it is recreated on demand on the fly.
11. **Multi-Tenancy Isolation**:
    - `FRANCHISE_OWNER` can view and download invoices and payment records only for their own franchise.
    - Franchise owners cannot create invoices, update due dates, or record manual payments.

### B. Example Payloads

#### 1. Create GST Invoice (`POST /api/v1/invoices`)
```json
{
  "salesOrderId": "91ba2d31-cf19-45d2-965a-52643a6d71b3",
  "invoiceType": "GST",
  "invoiceDate": "2026-09-23",
  "paymentTerm": "CREDIT",
  "dueDate": "2026-10-03",
  "gstRatePercent": 18.00,
  "notes": "Net 10 payment terms"
}
```

#### 2. Create Normal Invoice (`POST /api/v1/invoices`)
```json
{
  "salesOrderId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "invoiceType": "NORMAL",
  "invoiceDate": "2026-09-23",
  "paymentTerm": "FULL_PAYMENT",
  "notes": "Direct counter purchase"
}
```

#### 3. Update Credit Due Date (`PATCH /api/v1/invoices/{id}/due-date`)
```json
{
  "dueDate": "2026-10-15",
  "reason": "Franchise owner requested an approved 12-day payment extension"
}
```

#### 4. Record Manual Payment (`POST /api/v1/invoices/{id}/payments`)
```json
{
  "paymentDate": "2026-09-24",
  "amount": 2500.00,
  "paymentMode": "UPI",
  "referenceNumber": "UPI-IND-8849201",
  "remarks": "Received first installment from franchise owner via QR"
}
```

---

---

## 10. Dashboard, Credit Due List, Monthly Reports, and Website Enquiry (BE-07)

This module implements the business intelligence layer, credit recovery management, monthly downloadable CSV reports, and the public website franchise enquiry acquisition flow.

### A. Public Website Enquiry & WhatsApp Redirect Flow
1. **Public Submission**: Prospective franchise partners submit inquiries via `POST /api/v1/public/enquiries` (no authentication required).
2. **Server-Side Validation**:
   - `fullName`: 2-150 characters, mandatory.
   - `mobile`: Valid 10-digit Indian mobile number (e.g. `9876543210`), mandatory.
   - `email`: Optional, validated format if provided (max 150 chars).
   - `city`: Optional, max 100 chars.
   - `message`: Optional, max 1000 chars.
3. **Database Record**:
   - Saved with status `NEW` and default source `WEBSITE`.
   - Auto-generated sequential enquiry number: `ENQ-YYYYMM-XXXXX` (e.g. `ENQ-202609-00001`).
4. **WhatsApp Click-to-Chat Generation**:
   - Backend constructs a standard WhatsApp click-to-chat URL:
     `https://wa.me/{WHATSAPP_ENQUIRY_NUMBER}?text=...`
   - Configurable environment variable: `WHATSAPP_ENQUIRY_NUMBER` (e.g. `919876543210`).
   - Prefilled message includes: Header ("Website Franchise Enquiry"), Enquiry Number, Name, Mobile, Email (if provided), City (if provided), and Message (if provided).
   - **Crucial Architecture Boundary**: The backend does **not** make outbound WhatsApp API calls, send emails, or issue HTTP browser redirects. The backend responds with HTTP 201 containing `{ "enquiryId": "...", "enquiryNumber": "...", "whatsAppUrl": "..." }`, and the **frontend client performs the browser redirect** to the generated `whatsAppUrl`.

### B. Enquiry Management Workflow
- Managed via `GET /api/v1/enquiries`, `GET /api/v1/enquiries/{id}`, and `PATCH /api/v1/enquiries/{id}` by `SUPER_ADMIN` and `MANAGER`.
- **Status Lifecycle**: `NEW` → `CONTACTED` → `CLOSED`.
- Setting status to `CONTACTED` automatically records the timestamp in `contacted_at`.
- Direct transition from `NEW` to `CLOSED` is rejected with HTTP 400 unless performed by a `SUPER_ADMIN`.
- Enquiries cannot be deleted in V1 to preserve lead acquisition audit history.

### C. Operational Dashboard Summary
- `GET /api/v1/dashboard/summary`: Fast, query-based real-time operational metrics for `SUPER_ADMIN` and `MANAGER`:
  - Active counts: `activeFranchises`, `activeOutlets`, `activeMachines`, `activeProducts`.
  - Warehouse stock counts: `warehouseProductsInStock`, `warehouseProductsOutOfStock`.
  - Current calendar month metrics:
    - Purchases: `purchasesThisMonthCount`, `purchaseQuantityThisMonth`.
    - Orders: `ordersPlacedCount`, `ordersPreparedCount`, `ordersDeliveredThisMonthCount`.
    - Invoices & Dues: `invoicesThisMonthCount`, `invoiceTotalThisMonth`, `pendingInvoiceCount`, `pendingAmount`, `overdueInvoiceCount`, `overdueAmount`.
  - Leads: `newEnquiryCount`.

### D. Credit Due List & Severity Color Indicators
- `GET /api/v1/dashboard/dues`: Lists all invoices with `balanceAmount > 0` and non-null `dueDate`.
  - Filterable by `dueType` (`OVERDUE`, `DUE_TODAY`, `UPCOMING`), date range (`fromDate`, `toDate`), with pagination and sorting.
  - **Multi-Tenancy Isolation**: `SUPER_ADMIN` and `MANAGER` see all franchise dues; `FRANCHISE_OWNER` is strictly scoped to their own franchise dues.
- **Classification & Severity Mapping**:
  | Due Type | Condition | Display Severity | Frontend Display |
  |---|---|---|---|
  | `OVERDUE` | `dueDate < today` | `RED` | Highlight row in bold red |
  | `DUE_TODAY` | `dueDate == today` | `AMBER` | Highlight row in amber / warning |
  | `UPCOMING` | `dueDate > today` | `NORMAL` | Standard row display |
- Each item returns `invoiceId`, `invoiceNumber`, `franchiseId`, `franchiseName`, `invoiceDate`, `dueDate`, `totalAmount`, `paidAmount`, `balanceAmount`, `paymentStatus`, `dueType`, `daysOverdue`, and `displaySeverity`.

### E. Franchise Owner Personalized Dashboard
- `GET /api/v1/dashboard/franchise-summary`: Dedicated, lightweight overview for `FRANCHISE_OWNER`:
  - `franchiseName`, active `outletCount`, active `machineCount`.
  - Order breakdown by status (`ordersByStatus`: `DRAFT`, `PLACED`, `PREPARED`, `DELIVERED`, `CANCELLED`).
  - Monthly financial overview: `invoiceTotalThisMonth`, `pendingAmount`, `overdueAmount`, `overdueInvoiceCount`.
  - `nextDueInvoice`: Details of the earliest upcoming due invoice (if any).
  - `latestOrders`: Top 5 recent sales orders.
  - `latestInvoices`: Top 5 recent invoices.

### F. Monthly Downloadable CSV Reports
All reports stream `text/csv; charset=UTF-8` with `Content-Disposition: attachment; filename="..."`:
1. **Purchases Report** (`GET /api/v1/reports/monthly/purchases/download`):
   - Access: `SUPER_ADMIN`, `MANAGER`
   - Parameter: `month=YYYY-MM` (mandatory), `supplierId` (optional)
   - Columns: `Purchase Number`, `Bill Date`, `Supplier`, `Supplier Bill Number`, `Product Code`, `Flavour Name`, `Bottle Size Litre`, `Quantity`, `Purchase Status`, `Description`, `Created By`, `Created At`
2. **Orders Report** (`GET /api/v1/reports/monthly/orders/download`):
   - Access: `SUPER_ADMIN`, `MANAGER` (all), `FRANCHISE_OWNER` (own franchise only)
   - Parameter: `month=YYYY-MM` (mandatory), `franchiseId` (optional for admin/manager), `status` (optional)
   - Columns: `Order Number`, `Order Date`, `Franchise Name`, `Status`, `Product Code`, `Flavour Name`, `Quantity`, `Unit Price`, `Line Total`, `Notes`, `Created By`, `Delivered At`
3. **Invoices Report** (`GET /api/v1/reports/monthly/invoices/download`):
   - Access: `SUPER_ADMIN`, `MANAGER` (all), `FRANCHISE_OWNER` (own franchise only)
   - Parameter: `month=YYYY-MM` (mandatory), `franchiseId` (optional for admin/manager), `invoiceType`, `paymentStatus`
   - Columns: `Invoice Number`, `Invoice Date`, `Franchise Name`, `Invoice Type`, `Payment Term`, `Due Date`, `Subtotal`, `GST Rate`, `GST Amount`, `Total Amount`, `Paid Amount`, `Balance Amount`, `Payment Status`, `Product Code`, `Flavour Name`, `Quantity`, `Unit Price`, `Line Total`
4. **Credit Dues Report** (`GET /api/v1/reports/monthly/dues/download`):
   - Access: `SUPER_ADMIN`, `MANAGER` (all), `FRANCHISE_OWNER` (own franchise only)
   - Parameter: `month=YYYY-MM` (mandatory)
   - Columns: `Invoice Number`, `Franchise Name`, `Invoice Date`, `Due Date`, `Total Amount`, `Paid Amount`, `Balance Amount`, `Payment Status`, `Due Type`, `Days Overdue`

### G. Security & Sanitization Rules
- **Month Validation**: Strictly validated using `^[0-9]{4}-(0[1-9]|1[0-2])$`. Invalid formats return HTTP 400.
- **Spreadsheet Formula Injection Protection**: All cells starting with `=+-@\t\r` are prefixed with `'` to neutralize formula execution in Microsoft Excel, LibreOffice Calc, and Google Sheets.
- **RFC 4180 Escaping**: Commas, double quotes, and line breaks are safely enclosed in quotes with escaped double-quotes.
- **Multi-Tenant Protection**: A `FRANCHISE_OWNER` passing another franchise's ID or omitting `franchiseId` receives **only** data for their own authenticated franchise.
- **No Retail Soda Sale Calculations**: System intentionally tracks syrup orders, stock inward, invoices, and credit dues. Retail dispensing amounts belong to future consumption tracking modules.

---

## 11. IoT Soda Machine Integration & Manual Daily Sales Fallback (BE-08)

This module implements soda machine telemetry ingestion, connectivity monitoring, manual end-of-day counter sales fallback, and consolidated consumption reporting.

### A. Technical IoT Vendor Integration Guide
Soda machines and vendor gateways communicate with the backend exclusively via machine-to-server HTTPS API calls.

> [!IMPORTANT]
> - **Production Protocol**: Dispense and heartbeat telemetry **must always communicate over HTTPS** in production.
> - **Vendor Credential Boundary**: IoT vendors receive **only** the API endpoint URL, secret API key, and payload specification. Database credentials, server/VPS credentials, SSH keys, or cloud hosting logins must **never** be shared with vendors or third parties.
> - **Header Authentication**: Telemetry endpoints require the `X-API-KEY` header. No JWT authentication is needed.

#### 1. Ingestion Endpoint
- **URL**: `POST https://your-domain.com/api/v1/iot/events`
- **Headers**:
  ```http
  Content-Type: application/json
  X-API-KEY: <configured APP_IOT_API_KEY>
  ```

#### 2. Sample Payloads

##### A. Drink Dispensed Event (`DRINK_DISPENSED`)
Dispatched by the machine whenever a customer drink is prepared and dispensed:
```json
{
  "eventId": "EVT-20260923-000101",
  "machineId": "MACH-001",
  "deviceId": "DEV-001",
  "franchiseId": "FRAN-001",
  "eventTime": "2026-09-23T15:10:00+05:30",
  "eventType": "DRINK_DISPENSED",
  "drinkName": "Orange Soda",
  "items": [
    {
      "itemType": "SYRUP",
      "itemCode": "SYR-ORG",
      "itemName": "Orange Syrup",
      "lineNo": 1,
      "quantityMl": 30.000
    },
    {
      "itemType": "SODA",
      "itemCode": "RAW_SODA",
      "itemName": "Raw Soda",
      "lineNo": 9,
      "quantityMl": 220.000
    }
  ],
  "status": "SUCCESS"
}
```

##### B. Heartbeat Event (`HEARTBEAT`)
Dispatched periodically (recommended interval: **every 10 to 15 minutes**) to report machine liveness and telemetry health:
```json
{
  "eventId": "EVT-HB-20260923-150000",
  "machineId": "MACH-001",
  "deviceId": "DEV-001",
  "franchiseId": "FRAN-001",
  "eventTime": "2026-09-23T15:00:00+05:30",
  "eventType": "HEARTBEAT",
  "status": "SUCCESS"
}
```

#### 3. Expected Responses & Status Codes

##### Success Response (HTTP 200)
```json
{
  "success": true,
  "message": "IoT event processed successfully",
  "data": {
    "eventId": "EVT-20260923-000101",
    "duplicate": false,
    "machineCode": "MACH-001",
    "receivedAt": "2026-09-23T15:10:01.120Z"
  },
  "timestamp": "2026-09-23T15:10:01"
}
```

##### Idempotent Duplicate Response (HTTP 200)
If the device retries a transmission with an already-processed `eventId`, the server immediately acknowledges it without creating duplicate records or altering totals:
```json
{
  "success": true,
  "message": "Duplicate event already processed",
  "data": {
    "eventId": "EVT-20260923-000101",
    "duplicate": true
  },
  "timestamp": "2026-09-23T15:10:05"
}
```

##### Validation / Mapping Failure (HTTP 400)
Returned if `machineId`, `deviceId`, or `franchiseId` do not match active master records, or if required items are missing:
```json
{
  "success": false,
  "message": "Device ID 'DEV-999' does not match machine 'MACH-001'",
  "data": null,
  "timestamp": "2026-09-23T15:10:02"
}
```

##### Unauthorized (HTTP 401)
Returned if `X-API-KEY` header is missing or does not match the configured secret:
```json
{
  "success": false,
  "message": "Invalid or missing API key",
  "data": null,
  "timestamp": "2026-09-23T15:10:02"
}
```

### B. Machine Connectivity Monitoring & Limitations
- Tracked via `GET /api/v1/iot/machines/status` for `SUPER_ADMIN` and `MANAGER`.
- **Status Classification**:
  - `ONLINE`: `last_seen_at` is within `IOT_OFFLINE_THRESHOLD_MINUTES` (default: 30 minutes).
  - `OFFLINE`: `last_seen_at` is older than the configured threshold.
  - `NEVER_CONNECTED`: Machine has never transmitted a heartbeat or dispense event.
- **Important Limitation**: Connectivity status indicates only whether data was recently received. It does not conclusively prove whether the physical dispensing mechanism has mechanical failure or whether counter sales were simply low during quiet periods.

### C. Manual Daily Flavour-Wise Sales Fallback
When a machine operates offline or IoT gateway transmission is temporarily disrupted, the franchise owner reads the small hardware counter display at day-end (e.g., 10 PM) and records sales numbers manually.
- **Endpoints**: `POST`, `GET`, `PUT`, `DELETE` on `/api/v1/manual-sales`.
- **Multi-Tenancy Guardrail**: A `FRANCHISE_OWNER` can enter and view data **only** for machines belonging to their own franchise. Cross-franchise machine access is rejected with HTTP 400.
- **Uniqueness**: Only one entry is allowed per `(machine, product, salesDate)`. Duplicate entries are rejected.
- **Future Dates**: Entries cannot have a `salesDate` in the future.

### D. Unit Separation & Zero Retail Pricing Rules
- **Volume Separation**: Machine telemetry quantities are tracked strictly in **Millilitres (`ML`)**. Manual counter readings are tracked in **`GLASSES`** (or `ML`). The system keeps these metrics completely distinct in consumption summaries and CSV exports.
- **Zero Inventory Side Effects**: IoT dispenses and manual sales entries are for **reporting and visibility only**. They do **not** decrement warehouse inventory or franchise stock balances in V1.
- **No Retail Revenue Calculations**: The backend tracks raw syrup consumption, billing invoices, and stock movements. Retail customer pricing and dispensing cash receipts are intentionally not computed in this module.

---

## 12. Authentication & Testing Examples

### A. Login via curl
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "mobile": "9999999999",
    "password": "Admin@12345"
  }'
```

### B. Submit IoT Machine Event via curl
```bash
curl -X POST http://localhost:8080/api/v1/iot/events \
  -H "Content-Type: application/json" \
  -H "X-API-KEY: dev_iot_api_key_placeholder" \
  -d '{
    "eventId": "EVT-TEST-001",
    "machineId": "MACH-001",
    "deviceId": "DEV-001",
    "franchiseId": "FRAN-001",
    "eventTime": "2026-09-23T16:00:00Z",
    "eventType": "DRINK_DISPENSED",
    "drinkName": "Orange Soda",
    "status": "SUCCESS",
    "items": [
      { "itemType": "SYRUP", "itemCode": "SYR-ORG", "quantityMl": 30 },
      { "itemType": "SODA", "itemCode": "RAW_SODA", "quantityMl": 220 }
    ]
  }'
```

### C. Testing via Swagger UI
1. Start the application with the `dev` profile active:
   ```bash
   ./mvnw spring-boot:run
   ```
2. Open Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).
3. Log in with `POST /api/v1/auth/login` to obtain a JWT.
4. Click the green **Authorize** button in Swagger UI and input: `Bearer <your_token>`.
5. Execute operations across all modules:
   - Create Master entities (Suppliers, Products, Franchises, Outlets, Machines).
   - Enter supplier purchase bills (`POST /api/v1/purchases`).
   - Query warehouse stock balance (`GET /api/v1/inventory/warehouse`).
   - Place sales orders (`POST /api/v1/orders`).
   - Deliver orders and inspect stock deduction (`POST /api/v1/orders/{id}/deliver`).
   - Generate GST or Normal invoice (`POST /api/v1/invoices`).
   - Download invoice PDF (`GET /api/v1/invoices/{id}/download`).
   - View operational dashboard summary (`GET /api/v1/dashboard/summary`).
   - Query outstanding dues with severity indicators (`GET /api/v1/dashboard/dues`).
   - Ingest IoT telemetry events (`POST /api/v1/iot/events` using `X-API-KEY`).
   - Query machine connection status (`GET /api/v1/iot/machines/status`).
   - Record manual daily flavour sales (`POST /api/v1/manual-sales`).
   - Query and download consumption report (`GET /api/v1/reports/consumption/**`).

---

## 13. Flyway Database Migrations

Database versioning is managed with **Flyway**:
- `V1__baseline.sql`: Initializes the `application_metadata` tracking table.
- `V2__create_users_and_roles.sql`: Creates `users` table with UUID primary key, 10-digit mobile uniqueness check, role check (`SUPER_ADMIN`, `MANAGER`, `FRANCHISE_OWNER`), and indexes on mobile, role, and active status.
- `V3__create_master_data_tables.sql`: Creates `franchises`, `outlets`, `machines`, `products`, and `suppliers` tables with foreign keys, uniqueness constraints, check constraints (`bottle_size_litre > 0`, `default_selling_price >= 0`), and indexes.
- `V4__add_franchise_reference_to_users.sql`: Adds nullable `franchise_id UUID` foreign key to `users` referencing `franchises(id)` with index.
- `V5__create_purchase_inventory_tables.sql`: Creates `purchase_entries`, `purchase_items`, `inventory_stock`, and `stock_movements` tables with foreign keys, uniqueness constraints, positive quantity checks, non-negative balance checks, and indexes.
- `V6__create_sales_orders.sql`: Creates `sales_orders` and `sales_order_items` tables with UUID keys, unique order number format, cascade delete for items, quantity check > 0, unit price check >= 0, unique (order, product) composite constraint, and foreign key indexes.
- `V7__create_invoices_and_payments.sql`: Creates `invoices`, `invoice_items`, and `payment_collections` tables with UUID primary keys, financial precision `DECIMAL(14,2)`, unique `sales_order_id`, unique `invoice_number`, check constraints on types and terms, cascade delete for items, and indexes.
- `V8__create_enquiries.sql`: Creates `enquiries` table with UUID primary key, unique sequential enquiry number (`ENQ-YYYYMM-XXXXX`), status check (`NEW`, `CONTACTED`, `CLOSED`), contact timestamp, audit columns, and indexes on `status`, `mobile`, `created_at`, and `enquiry_number`.
- `V9__create_iot_events.sql`: Creates `iot_events` and `iot_event_items` tables with UUID primary keys, unique `external_event_id`, PostgreSQL `JSONB` raw payload, check constraints on types and positive quantities, and indexes on machine, franchise, event time, and item codes.
- `V10__create_manual_daily_flavour_sales.sql`: Creates `manual_daily_flavour_sales` table with UUID primary key, composite unique constraint `(machine_id, product_id, sales_date)`, check constraints on positive quantity and units (`GLASSES`, `ML`), and foreign key indexes.

---

## 14. Testing

To run the full unit and integration test suite:
```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw clean test
```

Tests run in an isolated in-memory H2 database (PostgreSQL compatibility mode) with Flyway migrations enabled, verifying **116 automated test cases**:
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
- Sales order creation: Manager price override vs Franchise Owner default price auto-fill
- Security enforcement: Franchise Owner cannot set unit price or create orders for another franchise
- Multi-tenancy isolation: Franchise Owner cannot read another franchise's order by ID
- Franchise self-service: `GET /api/v1/my/orders` returns only orders for the authenticated owner's franchise
- Order lifecycle progression: `DRAFT` → `PLACED` → `PREPARED` → `DELIVERED`
- Delivery stock deduction: Atomic deduction on delivery with `DELIVERY_OUT` movements and pessimistic locks
- Pre-flight stock verification: Insufficient stock halts delivery with full shortage details and zero stock deducted
- Atomic rollback: Multi-item delivery failure rolls back all stock deductions completely
- Order immutability: Delivered orders cannot be edited, cancelled, or deleted
- Deletion rules: Only `DRAFT` orders can be deleted; non-draft orders reject deletion
- Cancellation: Mandatory cancellation reason, no stock alterations, and rejection of re-cancellation
- Authorization guards: Only `SUPER_ADMIN` and `MANAGER` can prepare or deliver orders
- Invoice creation: Allowed strictly on `DELIVERED` orders; rejects non-delivered orders
- Invoice uniqueness: Enforces exactly one invoice per sales order
- Normal invoice: 0% GST, subtotal = total, no tax lines
- GST invoice: Correct GST calculation based on configurable/supplied percentage
- Credit due date: Automatically assigns `invoiceDate + 10 days` when omitted for `CREDIT`
- Due date update: Manager can extend due date with mandatory audit reason
- Partial payment: Decrements balance, increments paid amount, updates status to `PARTIALLY_PAID`
- Full payment: Settles invoice balance to 0 and transitions status to `PAID`
- Overpayment prevention: Rejects payment exceeding outstanding balance
- Overdue evaluation: Invoices with past due dates and pending balance correctly evaluate to `OVERDUE`
- Multi-tenant security: Franchise Owner can view and download only own franchise invoices
- Role restriction: Franchise Owner cannot create invoices or record payments
- PDF generation & download: Generates professional PDF file and streams download safely
- Item snapshot immutability: Subsequent changes to master product pricing do not alter existing invoice items
- Public enquiry creation: Saves record with `ENQ-YYYYMM-XXXXX` and returns prefilled WhatsApp click-to-chat URL
- Phone number validation: Rejects invalid mobile formats on public enquiry submission
- Enquiry lifecycle rules: Enforces `NEW` → `CONTACTED` → `CLOSED` progression and records `contacted_at`
- Operational dashboard summary: Correctly computes active master counts, monthly purchases, orders, invoices, and dues
- Credit due classification: Evaluates `OVERDUE`, `DUE_TODAY`, and `UPCOMING` with corresponding display severity (`RED`, `AMBER`, `NORMAL`)
- Credit dues multi-tenancy: Franchise owner views only own franchise dues; Manager/Super Admin views all dues
- Franchise owner dashboard summary: Returns personalized breakdown of machine/outlet counts, order statuses, next due invoice, and recent orders/invoices
- Monthly purchase CSV download: Returns valid CSV file with correct headers, UTF-8 charset, and filename
- Monthly reports multi-tenancy: Franchise owner downloads only own franchise order, invoice, and due CSV reports
- Report month validation: Rejects invalid month strings (must be `YYYY-MM`) with HTTP 400
- CSV injection protection: Neutralizes spreadsheet formula triggers (`=+-@\t\r`) by prefixing with `'`
- Role restrictions: Franchise Owner receives HTTP 403 on enquiry management and operational dashboard summary
- Valid DRINK_DISPENSED event saved with item rows and raw JSON payload
- Valid HEARTBEAT updates machine last_seen_at
- Invalid API key returns HTTP 401 Unauthorized
- Missing API key returns HTTP 401 Unauthorized
- Invalid machine/device/franchise mapping is rejected with HTTP 400 Bad Request
- Duplicate eventId is idempotent and does not create duplicate events (returns HTTP 200)
- Unknown or inactive syrup item code is rejected with HTTP 400 Bad Request
- Missing SYRUP or SODA item for DRINK_DISPENSED is rejected with HTTP 400 Bad Request
- IoT event does not alter warehouse inventory stock
- Machine connection status evaluates ONLINE, OFFLINE, and NEVER_CONNECTED correctly
- Franchise Owner can enter manual sales only for their own machine
- Duplicate manual machine/product/date entry is rejected with HTTP 400
- Franchise Owner is blocked from viewing raw IoT event list (HTTP 403 Forbidden)
- Consumption summary calculates flavour-wise usage and usage percentages accurately
- Manual GLASSES quantity is kept separate from IoT ML usage in summary and CSV exports
- Franchise Owner cannot download another franchise's consumption data (multi-tenancy isolation)


