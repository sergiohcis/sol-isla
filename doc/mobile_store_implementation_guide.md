# Mobile-Friendly Product Inventory & WhatsApp Order Web App
## Implementation Guide — Spring Boot + PostgreSQL + Angular 21 + PrimeNG

## 1. Project goal

Build a responsive web application where:

- Administrators authenticate securely and manage products.
- Customers browse and search the catalog without necessarily creating an account.
- Customers view product details, select quantities, and add products to a cart.
- Customers choose a payment method:
  - **Pay with Card**
  - **Cash on Delivery**
- The application creates an order and sends its cart/order information to a configured WhatsApp business/account.
- The business/operator confirms:
  - product availability,
  - final prices/discounts,
  - delivery availability and cost,
  - and any other required order details.
- Actual payment is completed only after the business confirmation.
- The system keeps an auditable order lifecycle rather than treating the WhatsApp message as the order itself.

> **Important design principle:** WhatsApp should be a communication/confirmation channel, not the system of record. PostgreSQL should remain authoritative for products, inventory, carts/orders, prices, discounts, payment state, and order status.

---

# 2. Recommended architecture

Use a modular monolith initially.

```text
                         ┌──────────────────────────┐
                         │       Angular 21         │
                         │     PrimeNG + PWA        │
                         └────────────┬─────────────┘
                                      │ HTTPS / REST
                                      ▼
                         ┌──────────────────────────┐
                         │     Spring Boot API      │
                         │                          │
                         │ Auth / Admin             │
                         │ Catalog                  │
                         │ Search                   │
                         │ Cart                     │
                         │ Orders                   │
                         │ Pricing / Discounts     │
                         │ Inventory                │
                         │ Payments                │
                         │ WhatsApp Integration    │
                         └────────────┬─────────────┘
                                      │
                    ┌─────────────────┴─────────────────┐
                    ▼                                   ▼
          ┌──────────────────┐                 ┌──────────────────┐
          │   PostgreSQL     │                 │ WhatsApp API /   │
          │                  │                 │ provider         │
          └──────────────────┘                 └──────────────────┘
```

Recommended deployment:

```text
Internet
   │
   ▼
Reverse Proxy / Load Balancer
   │
   ├── Angular static application
   │
   └── Spring Boot API
           │
           ├── PostgreSQL
           ├── Object Storage / Image CDN
           └── WhatsApp provider
```

Do not store product images as database BLOBs unless there is a specific requirement. Store images in object storage or an image service and persist only their URLs/metadata in PostgreSQL.

---

# 3. Main business modules

The backend should be divided into business modules rather than one large `ProductController`, `OrderController`, etc.

Recommended modules:

1. **Identity & Security**
2. **Admin / User Management**
3. **Catalog**
4. **Product Media**
5. **Categories**
6. **Pricing & Discounts**
7. **Inventory**
8. **Search**
9. **Cart**
10. **Checkout**
11. **Orders**
12. **Payments**
13. **Delivery**
14. **WhatsApp Integration**
15. **Notifications**
16. **Audit**
17. **Configuration**
18. **Reporting / Administration**

The initial release can combine some modules physically while preserving these logical boundaries.

---

# 4. Identity & Security module

## Responsibilities

This module protects administrative functionality.

Customer catalog browsing should be public, while administration must require authentication.

### Roles

Start with:

```text
ADMIN
```

Optionally add:

```text
SUPER_ADMIN
CATALOG_MANAGER
ORDER_MANAGER
```

Example permissions:

```text
PRODUCT_READ
PRODUCT_CREATE
PRODUCT_UPDATE
PRODUCT_DELETE

CATEGORY_CREATE
CATEGORY_UPDATE
CATEGORY_DELETE

ORDER_READ
ORDER_UPDATE
ORDER_CANCEL

INVENTORY_READ
INVENTORY_UPDATE

USER_READ
USER_UPDATE

SETTINGS_READ
SETTINGS_UPDATE
```

Prefer permissions/authorities over scattering checks based only on role names.

## Authentication

Recommended:

- Spring Security
- BCrypt/Argon2 password hashing
- Short-lived access tokens
- Refresh-token rotation if using JWT
- HTTPS everywhere
- Rate limiting on authentication endpoints
- Account lockout or progressive delay after repeated failures
- Audit login/logout/security events

For a browser-only first-party application, an HTTP-only, Secure, SameSite cookie-based session can be simpler and safer than putting JWTs in `localStorage`.

## Security rules

Public:

```text
GET /api/products
GET /api/products/{id}
GET /api/categories
GET /api/products/search
```

Authenticated admin:

```text
POST   /api/admin/products
PUT    /api/admin/products/{id}
DELETE /api/admin/products/{id}

POST   /api/admin/categories
PUT    /api/admin/categories/{id}
DELETE /api/admin/categories/{id}

GET    /api/admin/orders
PUT    /api/admin/orders/{id}/status
```

Never trust the frontend for authorization.

The backend must enforce every administrative permission.

---

# 5. Catalog module

The catalog represents what customers can buy.

## Product

Suggested fields:

```text
Product
-------
id
sku
name
slug
description
category_id
base_price
currency
status
featured
created_at
updated_at
version
```

Possible statuses:

```text
DRAFT
ACTIVE
INACTIVE
ARCHIVED
```

Use `ARCHIVED` rather than hard-deleting products that have participated in historical orders.

## SKU

Every product should have a unique SKU.

Example:

```text
SKU-000123
```

Business rule:

```text
SKU must be unique.
```

## Product name

Rules:

- required
- reasonable maximum length
- searchable
- HTML should not be blindly rendered
- normalize whitespace

## Slug

Generate a URL-friendly slug:

```text
Blue Running Shoes
        ↓
blue-running-shoes
```

Make it unique.

---

# 6. Categories module

Suggested structure:

```text
Category
--------
id
name
slug
description
parent_id
active
sort_order
```

Initially, categories can be flat.

If hierarchical categories are expected later, `parent_id` gives you a migration path.

Example:

```text
Clothing
 ├── Men's
 ├── Women's
 └── Kids

Electronics
 ├── Phones
 └── Accessories
```

---

# 7. Product images / media

A product may have multiple images.

Use a separate table:

```text
ProductImage
------------
id
product_id
url
storage_key
alt_text
sort_order
is_primary
created_at
```

Business rules:

- A product may have 0..N images.
- At most one image is primary.
- Validate file type.
- Validate file size.
- Generate safe server-side filenames.
- Do not trust the original filename.
- Consider resizing/compressing images before serving them.
- Use responsive image sizes.

For mobile performance, provide thumbnails/list images separately from full-resolution product images.

---

# 8. Pricing module

Do not put all pricing logic directly inside the Angular application.

The backend is authoritative.

## Price model

At minimum:

```text
base_price
discount_type
discount_value
effective_from
effective_to
```

Discount types:

```text
NONE
PERCENTAGE
FIXED_AMOUNT
```

Example:

```text
Base price:       $100
Discount:         20%
Effective price:  $80
```

## Important rule

The frontend may display:

```text
$100
20% OFF
$80
```

but the customer request must never be trusted to tell the server that the price is `$80`.

The backend recalculates the price.

---

# 9. Price snapshots

This is one of the most important parts of the design.

When an order is created, save the price that was actually used.

Do not calculate old orders from the current product price.

Example:

```text
OrderItem
---------
product_id
sku_snapshot
product_name_snapshot
unit_price
discount_amount
final_unit_price
quantity
line_total
```

If the product later changes from `$80` to `$100`, an existing order still retains its original price.

---

# 10. Inventory module

Even if the first version has simple inventory, model it explicitly.

Product:

```text
stock_quantity
```

or preferably a separate inventory model:

```text
Inventory
---------
product_id
available_quantity
reserved_quantity
updated_at
version
```

Useful concepts:

```text
available = physical stock - reserved stock
```

## Prevent overselling

At checkout/order creation, inventory updates must be transactional.

Use PostgreSQL transactions and appropriate locking/optimistic versioning.

Conceptually:

```text
BEGIN

read product/inventory

verify quantity available

reserve/decrement quantity

create order

COMMIT
```

Two simultaneous customers must not both successfully purchase the last item.

---

# 11. Search module

Start with PostgreSQL search rather than introducing Elasticsearch/OpenSearch immediately.

Search should support:

- product name
- SKU
- description
- category
- active status
- price range
- discount/featured filters

API example:

```text
GET /api/products?
    q=shoes
    &category=running
    &minPrice=20
    &maxPrice=100
    &page=0
    &size=20
```

Use pagination.

For larger catalogs, investigate PostgreSQL:

- full-text search
- trigram indexes
- appropriate GIN indexes

Do not return the entire catalog to Angular.

---

# 12. Cart module

The cart is a customer-facing temporary shopping state.

For a guest-first store, a cart can be identified by a secure random cart token.

Example:

```text
Cart
----
id
session_token
status
currency
created_at
updated_at
expires_at
```

```text
CartItem
--------
id
cart_id
product_id
quantity
```

Do not persist the product price as the authoritative price in the cart.

At checkout:

1. Load current product data.
2. Validate product status.
3. Validate inventory.
4. Recalculate discounts.
5. Calculate totals.
6. Create order snapshot.

## Quantity rules

For example:

```text
quantity >= 1
quantity <= configured maximum
```

The maximum should be enforced server-side.

---

# 13. Checkout module

Checkout is where the shopping cart becomes an order.

Collect only the information needed.

Example customer data:

```text
Customer information
--------------------
name
phone
email (optional)
```

Delivery information:

```text
Delivery
--------
address
city
postal_code
delivery_notes
```

Do not assume that WhatsApp alone contains all authoritative customer information.

---

# 14. Payment method selection

The user chooses:

```text
CARD
CASH_ON_DELIVERY
```

The payment method is stored on the order.

Example:

```text
Order
-----
payment_method
payment_status
```

Payment statuses:

```text
NOT_REQUIRED
PENDING
AUTHORIZED
PAID
FAILED
CANCELLED
REFUNDED
```

For COD:

```text
payment_method = CASH_ON_DELIVERY
payment_status = PENDING
```

For card:

```text
payment_method = CARD
payment_status = PENDING
```

The exact transition depends on the card provider.

---

# 15. Card payments

Do not implement card processing by sending card details to your Spring Boot application.

Use a PCI-compliant payment provider and hosted/tokenized payment components.

Recommended flow:

```text
Angular
   │
   │ checkout
   ▼
Spring Boot
   │
   │ create payment session
   ▼
Payment Provider
   │
   ▼
Angular / hosted checkout
   │
   ▼
Payment Provider
   │
   │ webhook
   ▼
Spring Boot
   │
   ▼
Order.payment_status = PAID
```

The webhook is the authoritative payment confirmation.

Never mark a card payment as paid merely because the browser says it succeeded.

---

# 16. Cash on Delivery

COD can have a simpler flow.

Example:

```text
Customer selects COD
        ↓
Create order
        ↓
payment_status = PENDING
        ↓
Send WhatsApp order
        ↓
Business confirms availability/delivery
        ↓
Order confirmed
        ↓
Delivery
        ↓
Cash collected
        ↓
payment_status = PAID
```

---

# 17. Order module

The order is the central business object.

Suggested model:

```text
Order
-----
id
order_number
customer_name
customer_phone
customer_email
delivery_address
delivery_city
delivery_postal_code
delivery_notes

subtotal
discount_total
delivery_fee
grand_total
currency

payment_method
payment_status

order_status

whatsapp_status

created_at
updated_at
confirmed_at
cancelled_at
```

## Order statuses

A useful initial state machine:

```text
PENDING_CONFIRMATION
        │
        ├── CANCELLED
        │
        ▼
CONFIRMED
        │
        ▼
PREPARING
        │
        ▼
READY_FOR_DELIVERY
        │
        ▼
OUT_FOR_DELIVERY
        │
        ▼
DELIVERED
```

Potential additional states:

```text
REJECTED
FAILED_DELIVERY
RETURNED
```

Do not allow arbitrary status changes.

Define legal transitions.

For example:

```text
PENDING_CONFIRMATION -> CONFIRMED
PENDING_CONFIRMATION -> CANCELLED

CONFIRMED -> PREPARING
CONFIRMED -> CANCELLED

PREPARING -> READY_FOR_DELIVERY
READY_FOR_DELIVERY -> OUT_FOR_DELIVERY
OUT_FOR_DELIVERY -> DELIVERED
```

---

# 18. Order numbers

Use a human-friendly order number in addition to the database ID.

Example:

```text
ORD-20260910-00421
```

The internal database UUID/ID and customer-facing order number serve different purposes.

---

# 19. Order creation transaction

The order creation process should be transactional.

Recommended flow:

```text
POST /api/checkout

1. Validate customer input.
2. Load cart.
3. Verify cart isn't empty.
4. Load all products.
5. Verify every product is ACTIVE.
6. Verify stock.
7. Calculate current prices.
8. Calculate discounts.
9. Calculate subtotal.
10. Calculate delivery fee if applicable.
11. Calculate total.
12. Reserve/decrement inventory.
13. Create Order.
14. Create OrderItems with snapshots.
15. Create payment record if required.
16. Create WhatsApp notification/outbox record.
17. Commit transaction.
18. Trigger WhatsApp delivery asynchronously.
19. Return order confirmation.
```

Do not make the WhatsApp network call inside the database transaction.

---

# 20. WhatsApp integration

Create a dedicated integration module.

Do not scatter WhatsApp HTTP calls throughout order code.

Interface:

```java
public interface WhatsAppService {

    WhatsAppSendResult sendOrderCreatedMessage(Order order);

}
```

Implementation:

```text
WhatsAppService
      │
      ▼
WhatsApp Provider API
```

Use the official WhatsApp Business Platform or another compliant provider appropriate to your region/business.

---

# 21. WhatsApp message content

A message could contain:

```text
New order: ORD-20260910-00421

Customer:
John Doe
+1 555 123 4567

Delivery:
123 Main Street
Orlando, FL

Items:
1 x Product A      $25.00
2 x Product B      $15.00

Subtotal:          $55.00
Discount:          -$5.00
Delivery:          $8.00
Total:             $58.00

Payment:
Cash on Delivery

Order status:
Pending confirmation
```

The exact message should be generated by a backend template.

Do not trust a client-generated WhatsApp message.

---

# 22. WhatsApp reliability: Outbox pattern

A very important implementation detail:

Do not do this:

```text
create order
    ↓
call WhatsApp API
    ↓
if WhatsApp fails, rollback order
```

External services can be unavailable.

Instead:

```text
DATABASE TRANSACTION
--------------------
Create Order
Create OrderItems
Create WhatsAppOutboxMessage
Commit
        │
        ▼
Background Worker
        │
        ▼
WhatsApp API
```

Example table:

```text
MessageOutbox
-------------
id
aggregate_type
aggregate_id
message_type
payload
status
attempt_count
next_attempt_at
last_error
created_at
sent_at
```

Statuses:

```text
PENDING
PROCESSING
SENT
FAILED
```

Retry transient failures with exponential backoff.

This prevents an order from disappearing just because WhatsApp is temporarily unavailable.

---

# 23. Idempotency

Checkout and external integrations must handle duplicate requests.

Example:

A customer taps:

```text
Place Order
```

then the network is slow and taps again.

Without protection:

```text
Order A
Order B
```

could be created accidentally.

Support an idempotency key:

```text
POST /api/checkout
Idempotency-Key: <random-client-generated-value>
```

Store the result associated with the key.

Repeated requests return the original order rather than creating another one.

Also make WhatsApp sends idempotent where possible.

---

# 24. Delivery module

Keep delivery separate from order pricing.

Start with simple configuration:

```text
DeliveryZone
------------
id
name
fee
active
```

For example:

```text
Zone A -> $5
Zone B -> $8
Zone C -> unavailable
```

Later you can add:

- minimum order
- free delivery threshold
- postal-code rules
- distance calculation
- delivery schedules
- delivery partners

The checkout response should show delivery cost before order submission.

---

# 25. Configuration module

Do not hard-code business settings.

Examples:

```text
STORE_NAME
STORE_CURRENCY
WHATSAPP_PHONE_NUMBER
ORDER_MAX_QUANTITY
DEFAULT_DELIVERY_FEE
CARD_PAYMENT_ENABLED
COD_ENABLED
```

Sensitive credentials belong in environment variables/secrets management, not in PostgreSQL or source code.

Example:

```text
WHATSAPP_API_TOKEN
PAYMENT_PROVIDER_SECRET
DATABASE_PASSWORD
```

---

# 26. Audit module

Administrative operations should be auditable.

Record:

```text
AuditLog
--------
id
actor_user_id
action
entity_type
entity_id
old_value
new_value
ip_address
created_at
```

Examples:

```text
ADMIN_CREATED_PRODUCT
ADMIN_UPDATED_PRODUCT
ADMIN_ARCHIVED_PRODUCT
ADMIN_CHANGED_PRICE
ADMIN_CHANGED_ORDER_STATUS
ADMIN_CHANGED_INVENTORY
ADMIN_LOGIN
```

For sensitive values, do not store secrets in audit logs.

---

# 27. Angular application structure

Suggested Angular structure:

```text
src/app/

  core/
    auth/
    guards/
    interceptors/
    services/
    models/

  shared/
    components/
    pipes/
    directives/
    validators/

  features/
    catalog/
    product-detail/
    cart/
    checkout/
    order-confirmation/

    admin/
      dashboard/
      products/
      categories/
      inventory/
      orders/
      users/
      settings/

  layout/
    public-layout/
    admin-layout/
```

Use Angular standalone components.

Use lazy-loaded routes for feature areas.

---

# 28. Angular public routes

Example:

```text
/
 /products
 /products/:slug
 /category/:slug
 /cart
 /checkout
 /order/:orderNumber
```

Admin:

```text
/admin/login
/admin
/admin/products
/admin/products/new
/admin/products/:id/edit
/admin/categories
/admin/inventory
/admin/orders
/admin/orders/:id
/admin/settings
```

Protect `/admin/**` using Angular route guards.

Remember: guards improve UX but **are not security**. Spring Security must independently enforce authorization.

---

# 29. PrimeNG components

PrimeNG is suitable for the admin application.

Useful components:

```text
Table
Dialog
Drawer
Form
InputText
InputNumber
Select
MultiSelect
Textarea
FileUpload
Tag
Toast
ConfirmDialog
Paginator
Menu
Toolbar
Card
Button
Image
Skeleton
```

Customer storefront should remain visually simpler than the administration UI.

---

# 30. Mobile-first storefront

The customer experience should prioritize:

```text
Mobile
  ↓
Tablet
  ↓
Desktop
```

Recommended mobile layout:

```text
┌─────────────────────────┐
│ Logo      🔍      🛒 2 │
├─────────────────────────┤
│ Search products...      │
├─────────────────────────┤
│ Categories              │
├─────────────────────────┤
│ ┌────────┐ ┌────────┐  │
│ │ image  │ │ image  │  │
│ │        │ │        │  │
│ │ Name   │ │ Name   │  │
│ │ $19.99 │ │ $25.00 │  │
│ │  [Add] │ │  [Add] │  │
│ └────────┘ └────────┘  │
└─────────────────────────┘
```

Keep important actions thumb-friendly.

Avoid large desktop-only tables on the customer side.

---

# 31. Admin product management

Admin product form:

```text
Product
--------------------------------

Name *
SKU *
Category *
Description

Price *
Discount type
Discount value

Status
Featured

Images
[Upload]

[Save]
```

Backend validation must mirror but not depend on frontend validation.

Admin should be able to:

- create
- edit
- archive
- activate/deactivate
- change price
- change discount
- manage images
- change category
- update inventory

Prefer archive/deactivate over physical deletion.

---

# 32. Product deletion strategy

Do not allow:

```text
DELETE product permanently
```

if it has historical order references.

Instead:

```text
ACTIVE
   ↓
INACTIVE
   ↓
ARCHIVED
```

If an admin requests deletion, the backend should determine whether physical deletion is safe.

A product referenced by an order should remain historically identifiable.

---

# 33. REST API design

Example API organization:

```text
/api/auth

/api/products
/api/categories

/api/cart
/api/checkout

/api/orders

/api/admin/products
/api/admin/categories
/api/admin/inventory
/api/admin/orders
/api/admin/users
/api/admin/settings
```

Public catalog example:

```http
GET /api/products?page=0&size=20&q=shoes
```

Product:

```http
GET /api/products/{slug}
```

Cart:

```http
GET    /api/cart
POST   /api/cart/items
PUT    /api/cart/items/{id}
DELETE /api/cart/items/{id}
```

Checkout:

```http
POST /api/checkout
```

Order:

```http
GET /api/orders/{orderNumber}
```

Admin:

```http
POST   /api/admin/products
PUT    /api/admin/products/{id}
DELETE /api/admin/products/{id}

GET    /api/admin/orders
GET    /api/admin/orders/{id}
PATCH  /api/admin/orders/{id}/status
```

---

# 34. DTO strategy

Do not expose JPA entities directly through REST.

Use DTOs.

Example:

```text
ProductResponse
ProductCreateRequest
ProductUpdateRequest
CartResponse
CartItemResponse
CheckoutRequest
CheckoutResponse
OrderResponse
OrderItemResponse
```

Benefits:

- API stability
- security
- validation
- prevention of accidental field exposure
- separation of persistence and API models

---

# 35. Database model

A reasonable first schema:

```text
users
roles
permissions
user_roles
role_permissions

categories

products
product_images

inventory
inventory_movements

carts
cart_items

orders
order_items

payments
payment_events

delivery_zones

message_outbox

audit_logs

app_settings
```

Potential relationships:

```text
Category 1 ─── N Product

Product 1 ─── N ProductImage

Product 1 ─── 1 Inventory
Product 1 ─── N InventoryMovement

Cart 1 ─── N CartItem
CartItem N ─── 1 Product

Order 1 ─── N OrderItem
Order 1 ─── N PaymentEvent
Order 1 ─── 1 Payment

Order 1 ─── N MessageOutbox
```

---

# 36. Inventory movements

Do not only overwrite the stock quantity.

Keep an inventory ledger:

```text
InventoryMovement
-----------------
id
product_id
type
quantity
reference_type
reference_id
reason
created_at
created_by
```

Types:

```text
PURCHASE
SALE
RESERVATION
RELEASE
ADJUSTMENT
RETURN
```

This makes inventory problems much easier to diagnose.

---

# 37. Order and inventory interaction

A recommended initial approach:

```text
Order created
     ↓
Inventory reserved
     ↓
Business confirms
     ↓
Reservation becomes sale
```

If the business rejects the order:

```text
Order rejected
     ↓
Inventory reservation released
```

This is preferable to immediately treating an unconfirmed WhatsApp order as a completed sale.

---

# 38. Recommended order state machine

Separate order status from payment status.

For example:

```text
ORDER STATUS

PENDING_CONFIRMATION
        │
        ├───────────────► CANCELLED
        │
        ▼
CONFIRMED
        │
        ▼
PREPARING
        │
        ▼
READY_FOR_DELIVERY
        │
        ▼
OUT_FOR_DELIVERY
        │
        ▼
DELIVERED
```

Payment:

```text
PAYMENT STATUS

PENDING
  │
  ├──► PAID
  │
  ├──► FAILED
  │
  └──► CANCELLED
```

These are intentionally independent.

For example:

```text
Order = CONFIRMED
Payment = PENDING
```

is valid for COD.

---

# 39. WhatsApp confirmation workflow

There are two possible architectures.

## Option A — Business receives messages manually

```text
Customer
   ↓
Website
   ↓
Order saved
   ↓
WhatsApp message
   ↓
Business employee
   ↓
Manual confirmation
```

This is the recommended first version.

The admin updates the order in the admin UI after confirming through WhatsApp.

## Option B — Automated WhatsApp conversations

Later:

```text
Customer
   ↓
WhatsApp
   ↓
Webhook
   ↓
Spring Boot
   ↓
Order workflow
```

This requires significantly more WhatsApp integration logic and should be added only if automation is genuinely needed.

---

# 40. Admin order screen

The order management screen should show:

```text
Order #ORD-20260910-00421

Customer
Name
Phone

Delivery
Address
Zone
Delivery fee

Items
--------------------------------
Product     Qty    Price    Total

Totals
Subtotal
Discount
Delivery
TOTAL

Payment
Method
Status

Order status
[Pending Confirmation ▼]

WhatsApp
Sent
Timestamp
Message status

Audit/history
```

Actions should depend on the current state.

For example, a delivered order should not expose a normal "Confirm" button.

---

# 41. Error handling

Create consistent API errors.

Example:

```json
{
  "code": "PRODUCT_OUT_OF_STOCK",
  "message": "One or more products are no longer available.",
  "details": [
    {
      "productId": "123",
      "available": 1,
      "requested": 3
    }
  ],
  "timestamp": "..."
}
```

Useful business error codes:

```text
PRODUCT_NOT_FOUND
PRODUCT_NOT_AVAILABLE
PRODUCT_OUT_OF_STOCK
INVALID_QUANTITY
CART_EMPTY
CART_EXPIRED
PRICE_CHANGED
CHECKOUT_FAILED
ORDER_NOT_FOUND
INVALID_ORDER_TRANSITION
PAYMENT_FAILED
DELIVERY_NOT_AVAILABLE
WHATSAPP_SEND_FAILED
```

---

# 42. Concurrency and consistency

The hardest business problems are not CRUD.

Pay particular attention to:

### Stock

Two customers buying the last product simultaneously.

### Price

Admin changes price while customer is checking out.

### Discount

Discount expires during checkout.

### Duplicate checkout

Customer submits the same checkout twice.

### WhatsApp failure

Order succeeds but WhatsApp API is unavailable.

### Payment webhook duplication

Payment provider sends the same webhook multiple times.

### Admin updates

Two admins edit the same product simultaneously.

Use:

- PostgreSQL transactions
- optimistic locking/version fields
- unique constraints
- idempotency keys
- database-level constraints
- outbox pattern
- payment webhook idempotency

---

# 43. Optimistic locking

Add:

```text
version
```

to entities where concurrent administration is possible.

Example:

```text
Product
id
...
version
```

If Admin A edits product version 5 and Admin B already changed it to version 6, reject Admin A's stale update instead of silently overwriting Admin B.

---

# 44. Database constraints

Do not rely exclusively on Java validation.

Useful PostgreSQL constraints:

```text
products.sku UNIQUE
products.slug UNIQUE

categories.slug UNIQUE

orders.order_number UNIQUE

product_images.product_id FK
cart_items.cart_id FK
order_items.order_id FK
```

Also add sensible `NOT NULL`, check constraints, and indexes.

---

# 45. Indexes

Initial indexes should include:

```text
products.sku
products.slug
products.category_id
products.status

categories.slug

cart_items.cart_id

orders.order_number
orders.customer_phone
orders.order_status
orders.created_at

message_outbox.status
message_outbox.next_attempt_at
```

Tune indexes after observing real queries.

---

# 46. Spring Boot package structure

A clean modular package structure:

```text
com.example.store

  common/
    exception/
    api/
    security/
    validation/

  auth/
    controller/
    service/
    repository/
    domain/
    dto/

  catalog/
    controller/
    service/
    repository/
    domain/
    dto/

  category/
    ...

  inventory/
    ...

  cart/
    ...

  checkout/
    ...

  order/
    ...

  payment/
    ...

  delivery/
    ...

  whatsapp/
    ...

  notification/
    ...

  audit/
    ...

  configuration/
    ...
```

Keep business rules inside services/domain logic rather than controllers.

---

# 47. Spring Boot persistence

Recommended stack:

```text
Spring Boot
Spring Web
Spring Security
Spring Data JPA
Hibernate
PostgreSQL Driver
Bean Validation
Flyway
```

Use Flyway for database migrations.

Never rely on:

```text
hibernate.ddl-auto=create
```

for production schema management.

---

# 48. Transactions

Use `@Transactional` around business operations such as:

```text
createOrder()
confirmOrder()
cancelOrder()
reserveInventory()
releaseInventory()
```

Avoid excessively large transactions.

External HTTP calls should generally happen outside the database transaction, using an outbox/event mechanism.

---

# 49. Background processing

The application needs asynchronous processing for:

- WhatsApp messages
- retrying failed messages
- cleanup of expired carts
- releasing expired inventory reservations
- possibly image processing
- payment reconciliation

Initially, Spring scheduling plus an outbox table may be sufficient.

As volume increases, move to a proper queue such as:

```text
RabbitMQ
Kafka
AWS SQS
Google Pub/Sub
Azure Service Bus
```

Do not introduce a message broker on day one unless scale or operational requirements justify it.

---

# 50. Security hardening

At minimum:

### Authentication

- Strong password policy
- Secure password hashing
- Rate limiting
- Secure cookies/tokens
- Session expiration
- Refresh-token rotation if applicable

### Authorization

- Server-side role/permission checks
- Admin endpoint protection
- Principle of least privilege

### Input security

- DTO validation
- Output encoding
- Avoid raw SQL
- Parameterized queries
- File upload validation
- Content Security Policy

### Infrastructure

- HTTPS
- Secure headers
- Database not publicly exposed
- Secrets outside Git
- Regular dependency updates
- Backups

### Logging

Never log:

```text
passwords
payment secrets
access tokens
full card details
```

---

# 51. Image upload security

Admin image uploads are an attack surface.

Validate:

```text
MIME type
file signature
file size
image dimensions
```

Do not rely solely on:

```text
filename.endsWith(".jpg")
```

Generate your own storage key.

Consider stripping metadata such as EXIF when appropriate.

---

# 52. Customer authentication

For the first release, I recommend **guest checkout**.

Customers should not need an account just to buy.

This reduces friction considerably.

Later, add optional customer accounts for:

- order history
- saved addresses
- favorites
- faster checkout

Do not make customer registration part of the MVP unless there is a real business need.

---

# 53. Customer order tracking

After checkout, return:

```text
orderNumber
status
paymentStatus
```

Example:

```text
Order ORD-20260910-00421

✓ Order received
● Waiting for confirmation
○ Preparing
○ Out for delivery
○ Delivered
```

A customer can retrieve the order using a secure mechanism rather than exposing predictable database IDs.

If no customer account exists, consider a signed order-status token or one-time lookup mechanism.

Do not expose another customer's order merely because someone knows a sequential order number.

---

# 54. API versioning

Start with:

```text
/api/v1/...
```

This makes future API evolution easier.

Example:

```text
/api/v1/products
/api/v1/checkout
/api/v1/admin/products
```

---

# 55. Testing strategy

Testing should focus heavily on business rules.

## Unit tests

Test:

```text
discount calculation
price calculation
delivery calculation
order transitions
inventory reservation
inventory release
quantity validation
```

Example:

```text
$100 product
20% discount
quantity 2

subtotal = $200
discount = $40
total before delivery = $160
```

## Integration tests

Use Testcontainers for PostgreSQL.

Test:

```text
checkout transaction
concurrent inventory operations
database constraints
order persistence
outbox persistence
```

## API tests

Test:

```text
unauthenticated admin access -> 401/403
authenticated admin -> allowed
invalid product -> 400
missing product -> 404
out-of-stock checkout -> rejected
```

## End-to-end tests

Test:

```text
Browse
→ Search
→ Product
→ Add to cart
→ Checkout
→ Choose payment
→ Create order
→ Confirmation
```

---

# 56. Angular testing

Test:

- cart calculations displayed by UI
- checkout validation
- admin product forms
- route protection
- error handling
- responsive behavior

But remember: Angular calculations are presentation only.

The backend remains authoritative.

---

# 57. Observability

Include:

```text
structured application logs
request IDs
order IDs
metrics
health endpoints
error monitoring
```

Useful metrics:

```text
orders_created_total
orders_confirmed_total
orders_cancelled_total

whatsapp_messages_sent_total
whatsapp_messages_failed_total

payment_success_total
payment_failure_total

checkout_failure_total

inventory_reservation_failure_total
```

Always make it possible to trace:

```text
HTTP request
   ↓
checkout
   ↓
order
   ↓
payment
   ↓
WhatsApp message
```

using correlation/request IDs.

---

# 58. Suggested MVP

Do not build everything at once.

## Phase 1 — Foundation

Backend:

- Spring Boot
- PostgreSQL
- Flyway
- Spring Security
- Admin authentication
- basic roles

Frontend:

- Angular 21
- PrimeNG
- responsive layouts
- admin login

## Phase 2 — Catalog

Implement:

- categories
- products
- product images
- prices
- discounts
- active/inactive products
- public product listing
- product details
- search
- pagination

## Phase 3 — Cart

Implement:

- guest cart
- add item
- change quantity
- remove item
- cart totals

## Phase 4 — Checkout

Implement:

- customer information
- delivery information
- payment method
- server-side price calculation
- inventory validation
- order creation
- order snapshots

## Phase 5 — WhatsApp

Implement:

- outbox
- WhatsApp integration
- retries
- message status
- order message template

## Phase 6 — Admin orders

Implement:

- order list
- order details
- confirmation
- cancellation
- preparation
- delivery status
- payment status

## Phase 7 — Card payment

Integrate a payment provider using hosted/tokenized payment collection.

## Phase 8 — Hardening

Add:

- audit logging
- rate limiting
- monitoring
- backups
- security review
- concurrency testing
- E2E tests

---

# 59. Recommended implementation sequence

The following sequence minimizes rework:

```text
1. Database migrations
       ↓
2. Domain models
       ↓
3. Catalog API
       ↓
4. Admin security
       ↓
5. Admin product UI
       ↓
6. Public catalog UI
       ↓
7. Cart
       ↓
8. Pricing service
       ↓
9. Inventory
       ↓
10. Checkout
       ↓
11. Orders
       ↓
12. Outbox
       ↓
13. WhatsApp
       ↓
14. Admin order workflow
       ↓
15. Card payment
       ↓
16. Monitoring + hardening
```

---

# 60. Core business services

A useful Spring service boundary would be:

```text
ProductService
CategoryService
ProductImageService

PricingService
DiscountService

InventoryService
InventoryReservationService

CartService

CheckoutService
OrderService
OrderStateMachine

DeliveryService

PaymentService
PaymentWebhookService

WhatsAppService
MessageOutboxService

AuditService
```

The most important one is:

```text
CheckoutService
```

because it orchestrates several business rules.

---

# 61. Checkout pseudocode

```java
@Transactional
public CheckoutResult checkout(CheckoutRequest request,
                               String idempotencyKey) {

    // 1. Idempotency
    ExistingCheckout existing =
        idempotencyService.find(idempotencyKey);

    if (existing != null) {
        return existing.result();
    }

    // 2. Load cart
    Cart cart = cartService.getActiveCart(request.cartToken());

    if (cart.isEmpty()) {
        throw new CartEmptyException();
    }

    // 3. Recalculate everything on the server
    PricingResult pricing =
        pricingService.calculate(cart);

    // 4. Validate/reserve inventory
    inventoryService.reserve(pricing.items());

    // 5. Create order
    Order order =
        orderService.createFromCheckout(
            request,
            pricing
        );

    // 6. Create payment
    paymentService.initialize(order);

    // 7. Create outbox event
    outboxService.enqueue(
        OrderCreatedEvent.from(order)
    );

    // 8. Store idempotency result
    idempotencyService.store(
        idempotencyKey,
        order
    );

    return CheckoutResult.from(order);
}
```

The exact implementation will vary, but this illustrates the desired separation.

---

# 62. Pricing pseudocode

```java
public LinePrice calculate(Product product, int quantity) {

    Money basePrice = product.getBasePrice();

    Discount discount =
        discountService.findApplicableDiscount(product);

    Money discountAmount =
        discount.calculate(basePrice, quantity);

    Money finalUnitPrice =
        basePrice.subtract(
            discount.calculateUnitDiscount(basePrice)
        );

    Money lineTotal =
        finalUnitPrice.multiply(quantity);

    return new LinePrice(
        basePrice,
        discountAmount,
        finalUnitPrice,
        lineTotal
    );
}
```

Use a proper money representation.

For Java, prefer `BigDecimal`/a dedicated Money value object rather than `double`.

---

# 63. Currency and rounding

Define currency behavior explicitly.

For example:

```text
USD
2 decimal places
HALF_UP
```

Do not allow different parts of the application to implement their own rounding rules.

Create one money/pricing policy.

---

# 64. Important business decision: confirmation vs payment

Your described process has an unusual but reasonable sequence:

```text
Customer selects payment method
        ↓
Website creates order
        ↓
WhatsApp receives order
        ↓
Business verifies stock + price + delivery
        ↓
Business confirms
        ↓
Payment happens
```

Model this explicitly.

Do not call the initial cart submission a "paid order."

Recommended:

```text
Order:
PENDING_CONFIRMATION

Payment:
PENDING
```

After confirmation:

```text
Order:
CONFIRMED
```

Then payment can proceed.

For card payments, the safest implementation is to create the payment session only when the business has confirmed the order, unless the payment provider supports an authorization/hold workflow that matches your business requirements.

---

# 65. Recommended API flow

## Customer

```text
GET /products
        ↓
GET /products/{slug}
        ↓
POST /cart/items
        ↓
GET /cart
        ↓
POST /checkout
        ↓
Order created
        ↓
WhatsApp notification queued
        ↓
Customer sees order number
```

## Business

```text
Admin logs in
        ↓
GET /admin/orders
        ↓
Open order
        ↓
Contact/communicate through WhatsApp
        ↓
Verify stock
        ↓
Verify final delivery
        ↓
Confirm order
        ↓
Customer completes payment
        ↓
Payment webhook
        ↓
payment_status = PAID
```

---

# 66. What should NOT be in the MVP

Avoid initially building:

```text
microservices
Elasticsearch
Kafka
complex loyalty programs
multi-vendor marketplace
customer accounts
recommendation engine
real-time inventory across warehouses
automated WhatsApp chatbot
complex promotions engine
```

These can all be added later.

A well-designed modular monolith can support a large amount of traffic before microservices become necessary.

---

# 67. Production deployment

Recommended minimum:

```text
                 Internet
                    │
                    ▼
             HTTPS / Proxy
               /        \
              /          \
       Angular SPA     Spring Boot
                          │
                          ▼
                      PostgreSQL
                          │
                     Backups
```

For production:

- PostgreSQL should be private.
- API should be HTTPS-only.
- Use managed PostgreSQL if possible.
- Store images outside the application container.
- Store secrets in a secrets manager/environment configuration.
- Configure automated database backups.
- Test restoration, not just backup creation.
- Configure monitoring and alerts.

---

# 68. Environment configuration

Have at least:

```text
application-local.yml
application-test.yml
application-prod.yml
```

Use environment variables/secrets for:

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD

WHATSAPP_API_URL
WHATSAPP_API_TOKEN
WHATSAPP_PHONE_NUMBER_ID

PAYMENT_API_KEY
PAYMENT_WEBHOOK_SECRET
```

Never commit production secrets.

---

# 69. Suggested repository layout

A monorepo is reasonable initially:

```text
store-app/
│
├── backend/
│   ├── src/
│   ├── pom.xml
│   └── ...
│
├── frontend/
│   ├── src/
│   ├── angular.json
│   └── ...
│
├── infrastructure/
│   ├── docker/
│   ├── nginx/
│   └── ...
│
├── docs/
│   ├── architecture.md
│   ├── api.md
│   └── business-rules.md
│
└── README.md
```

---

# 70. Definition of Done for MVP

The MVP should be considered complete when:

### Catalog

- [ ] Admin can create products.
- [ ] Admin can edit products.
- [ ] Admin can archive products.
- [ ] Admin can manage categories.
- [ ] Admin can upload product images.
- [ ] Customers can browse products.
- [ ] Customers can search/filter products.
- [ ] Product prices and discounts display correctly.

### Cart

- [ ] Customer can add products.
- [ ] Customer can change quantities.
- [ ] Customer can remove products.
- [ ] Cart survives normal page navigation.
- [ ] Invalid/out-of-stock products are detected.

### Checkout

- [ ] Customer can enter delivery information.
- [ ] Customer can select card or COD.
- [ ] Server recalculates all prices.
- [ ] Inventory is validated transactionally.
- [ ] Order is created with immutable price snapshots.
- [ ] Duplicate checkout is prevented.

### WhatsApp

- [ ] Order creates an outbox message.
- [ ] Message is sent asynchronously.
- [ ] Failures are retried.
- [ ] Message status is visible to admins.

### Admin

- [ ] Admin authentication works.
- [ ] Unauthorized users cannot access admin APIs.
- [ ] Admin can see orders.
- [ ] Admin can confirm/reject orders.
- [ ] Order transitions are validated.
- [ ] Administrative changes are audited.

### Payment

- [ ] COD workflow is implemented.
- [ ] Card provider is integrated through a secure hosted/tokenized flow.
- [ ] Payment status is updated from provider webhooks.
- [ ] Duplicate webhooks are handled safely.

### Production

- [ ] HTTPS
- [ ] Database backups
- [ ] Error monitoring
- [ ] Structured logging
- [ ] Security headers
- [ ] Rate limiting
- [ ] Automated tests
- [ ] Database migrations
- [ ] Deployment documentation

---

# 71. Final architecture recommendation

The key architecture should be:

```text
                       CUSTOMER
                           │
                           ▼
                  Angular 21 / PrimeNG
                           │
                         HTTPS
                           │
                           ▼
                  Spring Boot API
                           │
          ┌────────────────┼────────────────┐
          │                │                │
          ▼                ▼                ▼
       Catalog          Checkout          Admin
          │                │                │
          │                ├── Pricing      │
          │                ├── Inventory    │
          │                ├── Orders       │
          │                └── Payments     │
          │                                │
          └──────────────┬─────────────────┘
                         ▼
                    PostgreSQL
                         │
                 Transactional Outbox
                         │
                         ▼
                  WhatsApp Service
                         │
                         ▼
                    WhatsApp API


Card Payment:

Angular
   │
   ▼
Payment Provider
   │
   ▼
Webhook → Spring Boot → PaymentService → Order
```

The most important design decisions are:

1. **PostgreSQL is the source of truth.**
2. **Prices are always recalculated on the server.**
3. **Orders contain immutable product/price snapshots.**
4. **Inventory operations are transactional.**
5. **Order status and payment status are separate state machines.**
6. **WhatsApp is an integration/communication channel, not the database.**
7. **Use an outbox so WhatsApp failures cannot invalidate a successfully created order.**
8. **Use idempotency to prevent duplicate orders and payment processing.**
9. **Admin security is enforced by Spring Security, not Angular guards.**
10. **Products should normally be archived rather than physically deleted.**
11. **Guest checkout should be the default MVP experience.**
12. **Card data should never pass through or be stored by your application unless you deliberately take on the relevant compliance scope.**
13. **Start as a modular monolith; extract services only when scale or organizational needs justify it.**

This structure gives you a clean MVP while leaving room for customer accounts, automated WhatsApp conversations, advanced promotions, multiple warehouses, delivery integrations, and additional payment providers later.
