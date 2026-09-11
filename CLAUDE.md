# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository state

Phase 1 (Foundation), Phase 2 (Catalog), Phase 3 (Cart), Phase 4 (Checkout), and Phase 5
(WhatsApp) are implemented. Phase 1: Spring Boot backend,
Angular frontend, and the IIS deployment infra, with tooling/conventions that
deliberately mirror the sibling `SweetHome` project (same shared Windows Server, same
credentials, same non-Docker/session-cookie/Flyway/embedded-postgres-for-tests choices)
— see `infrastructure/iis/README.md` for the deployment shape. Phase 2: categories
(flat, `parent_id` ready for hierarchy later), products (SKU/slug uniqueness, DRAFT ->
ACTIVE/ARCHIVED -> INACTIVE/ARCHIVED -> ARCHIVED status machine), product images (local
filesystem storage under `sol-isla.storage.local-path`, served at `/media/**`), server-
side discount pricing (`catalog/pricing/ProductPricingService` — the only place
effective price is computed, reused by checkout later), and inventory as an explicit
ledger (`inventory_movements`, not just a counter) with an admin stock-adjust endpoint.
Public search/listing lives at `/api/products` (ACTIVE only); admin CRUD + image upload
+ inventory adjust live under `/api/admin/**`, gated by `@PreAuthorize` permissions
(`PRODUCT_*`, `CATEGORY_*`, `INVENTORY_*` in `security/authorization/Permission.java`).
Phase 3: guest cart identified by an httpOnly cookie (`sol_isla_cart`, no accounts —
CLAUDE.md rule 11), `/api/cart` + `/api/cart/items`. The cart never persists price —
`CartServiceImpl.buildResponse` always reads the current product/discount live through
`ProductPricingService`, same as the catalog (design doc §12). Add/update enforce both a
configured per-item max (`sol-isla.cart.max-quantity-per-item`) and live available stock;
a cart item's ownership is checked against the requesting cookie's cart on every
update/remove so one guest can't touch another's cart by guessing an item id. The public
product-detail page's "Add to Cart" and the header cart badge are wired to this for real.
Phase 4: `CheckoutServiceImpl` — the most important service in the codebase — does
idempotency check -> load cart -> re-verify every product ACTIVE and in stock ->
recompute prices live -> decrement inventory (`InventoryService.sell`, a SALE movement)
-> create Order + immutable OrderItem snapshots -> mark the cart CONVERTED -> enqueue a
`MessageOutbox` row, all inside one `@Transactional` boundary; no external call happens
in it (CLAUDE.md rule 6 — the actual WhatsApp send is Phase 5, reading from that outbox
row). `POST /api/checkout` accepts an optional `Idempotency-Key` header — a repeat with
the same key returns the original order (looked up before touching the cart at all) —
and a concurrent sale of the last unit surfaces as a 409 via `Inventory`'s optimistic
lock rather than overselling (verified with a real integration test, not just reasoning
about it — see `CheckoutControllerIT`). Order numbers come from a Postgres sequence
(`order_number_seq`), not a "count today's orders" query, to stay race-free under
concurrent checkouts. `DeliveryZone` (flat fee per zone, `/api/delivery/zones` public,
`/api/admin/delivery/zones` gated by `DELIVERY_MANAGE`) supplies the delivery fee — no
admin UI for zones yet, only the API; seed one via the admin API or build the UI
whenever it's actually needed. `GET /api/orders/track/{orderNumber}` is the public
order-lookup the order-confirmation page uses, with no additional secret beyond the
order number itself (design doc §53). `PaymentStatus` starts `PENDING` for both `CARD`
and `CASH_ON_DELIVERY` — real card-provider integration (CLAUDE.md rule 8: only a
webhook may set `PAID`) is Phase 7.

Phase 5: `OutboxWorker` (`@Scheduled`, every 30s) is the only thing that calls the
WhatsApp Business Cloud API — `WhatsAppServiceImpl` posts to the Meta Graph API and
always renders the message fresh from the current `Order`/`OrderItem` rows at send
time, never from the outbox row's stored payload (design doc §21: never trust a
client-generated or stale message). Processing one row is its own
`@Transactional` method on a *separate* bean (`OutboxProcessingService`) from the
`@Scheduled` caller (`OutboxWorker`) — calling a `@Transactional` method on `this`
bypasses the Spring AOP proxy entirely, so this split isn't optional. Failed sends get
exponential backoff (`MessageOutbox.recordRetryableFailure`, status stays `PENDING`
with `next_attempt_at` pushed out) up to 5 attempts, then `FAILED` terminally — nothing
auto-requeues a terminally-failed row. `WhatsAppService.isConfigured()` gates the
worker before it ever touches the database: `sol-isla.whatsapp.*` is empty by default
(same as this dev environment), so out of the box every outbox row just sits `PENDING`
indefinitely rather than burning through retries against credentials that will never
work — confirmed live against the running dev server, not just in tests. Admin order
management and the real card-payment provider (Phase 6+) are not yet implemented; their
backend packages exist only as empty `package-info.java` stubs.

Testing note: Phase 2 shipped a real bug (`/api/products` 401ing on empty filters — see
git history) that only surfaced testing against a live server, because no test actually
called the endpoint. Every module built since (see `ProductControllerIT`,
`CartControllerIT`) has a MockMvc-based integration test that exercises its real HTTP
endpoints end-to-end (not just the service layer) — keep doing that for new modules
rather than only unit-testing services.

Build/test/run:

```text
Backend   (backend/):  ./mvnw spring-boot:run        # dev, needs application-local.yml
                        ./mvnw test                    # unit + integration (embedded-postgres, no Docker)
                        ./mvnw clean package            # prod jar

Frontend  (frontend/): npm start                       # ng serve, proxies /api via proxy.conf.json
                        npm run build                   # prod build -> dist/frontend/browser
                        npm test                        # vitest
```

`backend/src/main/resources/application-local.yml` (gitignored) holds the local dev
datasource; copy it yourself before running `spring-boot:run` locally — it is not
checked in.

Deviations from the design doc worth knowing about (chosen to match SweetHome rather
than the doc's generic placeholder names): the Identity & Security module's package is
`security` (not `auth`), and each module's service layer package is `providedService`
(not `service`) — same convention as SweetHome. Package root is
`com.hosannasolutions.solisla` (Java 25 / Spring Boot 4, matching SweetHome).

When extending implementation, follow the architecture and phased plan described below
(full detail in the design doc) rather than inventing a different stack or structure.

## What is being built

A mobile-friendly product inventory / storefront web app where customers browse a
catalog, build a cart, and check out with **Card** or **Cash on Delivery**. Orders are
sent to a WhatsApp business account for the operator to manually confirm stock, price,
and delivery before payment is finalized. Target stack: **Spring Boot + PostgreSQL**
backend, **Angular 21 + PrimeNG** frontend.

## Non-negotiable design principles

These are called out repeatedly in the design doc and should govern any implementation
decisions, even ones not explicitly asked about:

1. **PostgreSQL is the sole source of truth.** WhatsApp is a communication/confirmation
   channel only — never treat a WhatsApp message as the order record.
2. **The backend recalculates prices/discounts server-side on every checkout.** Never
   trust a client-supplied price.
3. **Orders store immutable price/product snapshots** (`OrderItem.sku_snapshot`,
   `product_name_snapshot`, `unit_price`, `final_unit_price`, ...) so historical orders
   are unaffected by later product/price changes.
4. **Inventory changes are transactional** and use an explicit ledger
   (`InventoryMovement`: PURCHASE/SALE/RESERVATION/RELEASE/ADJUSTMENT/RETURN) rather
   than only overwriting a stock counter. Prevent overselling with locking/optimistic
   versioning inside the checkout transaction.
5. **Order status and payment status are separate state machines.** E.g.
   `order_status = CONFIRMED` with `payment_status = PENDING` is valid for COD. Never
   allow arbitrary status transitions — enforce a defined transition table
   (`PENDING_CONFIRMATION → CONFIRMED → PREPARING → READY_FOR_DELIVERY →
   OUT_FOR_DELIVERY → DELIVERED`, with `CANCELLED`/`REJECTED`/`FAILED_DELIVERY` as
   applicable branches).
6. **WhatsApp delivery uses the transactional outbox pattern.** Never call the
   WhatsApp API inside the same DB transaction as order creation — write a
   `MessageOutbox` row in the transaction, then send asynchronously from a background
   worker with retry/backoff. A WhatsApp outage must never roll back or hide a
   successfully created order.
7. **Idempotency is required at checkout** (client-supplied `Idempotency-Key`) and for
   WhatsApp sends / payment webhooks, to survive duplicate submissions and retries.
8. **Card payments go through a PCI-compliant, hosted/tokenized provider.** Card
   details never touch the Spring Boot app. Payment is only marked `PAID` from a
   provider webhook — never from the browser's success response alone. Prefer
   creating the payment session only after the business has confirmed the order.
9. **Admin authorization is enforced by Spring Security, not Angular route guards.**
   Guards are UX only; every admin endpoint must independently check
   permissions/authorities server-side (prefer permissions like `PRODUCT_UPDATE` over
   role-name checks).
10. **Products are archived/deactivated, never hard-deleted**, if they have historical
    order references (`ACTIVE → INACTIVE → ARCHIVED`).
11. **Guest checkout is the MVP default** — no customer accounts/registration unless
    there's a real business need.
12. Start as a **modular monolith**; do not introduce microservices, Elasticsearch,
    Kafka, or a message broker until scale/ops genuinely require it. Use PostgreSQL
    full-text/trigram search before reaching for a search engine.

## Intended module boundaries

Backend logical modules (can be physically combined at first, but keep the
boundaries): Identity & Security, Admin/User Management, Catalog, Product Media,
Categories, Pricing & Discounts, Inventory, Search, Cart, Checkout, Orders, Payments,
Delivery, WhatsApp Integration, Notifications, Audit, Configuration, Reporting.

Suggested Spring package layout mirrors this: `com.example.store.{auth, catalog,
category, inventory, cart, checkout, order, payment, delivery, whatsapp, notification,
audit, configuration, common}` — business rules live in services/domain, not
controllers.

`CheckoutService` is the most important service: it orchestrates idempotency check →
load cart → server-side pricing recalculation → inventory reservation → order creation
→ payment initialization → outbox enqueue, all inside one `@Transactional` boundary
(external HTTP calls excluded from that transaction).

Suggested Angular structure: `core/` (auth, guards, interceptors, services, models),
`shared/` (components, pipes, directives, validators), `features/` (catalog,
product-detail, cart, checkout, order-confirmation, and `admin/*` sub-features),
`layout/` (public-layout, admin-layout). Use standalone components and lazy-loaded
feature routes.

## Implementation sequence

Follow this order to minimize rework (per the design doc): DB migrations (Flyway) →
domain models → catalog API → admin security → admin product UI → public catalog UI →
cart → pricing service → inventory → checkout → orders → outbox → WhatsApp → admin
order workflow → card payment → monitoring/hardening.

Reference `doc/mobile_store_implementation_guide.md` for full schema suggestions
(products, categories, inventory, carts, orders, payments, message_outbox, audit_logs,
app_settings), API route layout (`/api/v1/...`), error code conventions, and the phased
MVP checklist — it is the authoritative spec for this build and should be kept in sync
with any deviations made during implementation.
