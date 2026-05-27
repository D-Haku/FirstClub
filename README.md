# FirstClub Membership Program

Backend for a tiered subscription membership system. Users subscribe to a (Plan × Tier) combination, get a configurable bundle of benefits applied at checkout, and receive an advisory tier recommendation based on their order history and cohort.

Runnable, demoable, with a complete REST surface, optimistic-locking based concurrency, and idempotent write paths.

## Stack

| Layer | Choice | Why |
|---|---|---|
| Language | Java 17 | Modern records, sealed types, switch expressions, pattern matching keep the domain model concise |
| Framework | Spring Boot 3.2 | Standard for production Java services; brings DI, transactions, validation, web, retry, and test support out of the box |
| Web | Spring Web (Servlet) | Familiar `@RestController` programming model and RFC 7807 `ProblemDetail` support |
| Persistence | Spring Data JPA + Hibernate 6 | Repository abstraction, derived queries, `@Version` optimistic locking |
| Database | H2 (embedded, in-memory) | Zero-setup demo. Schema lives in `schema.sql`; switching to Postgres only needs a driver swap |
| Validation | Jakarta Bean Validation | Declarative `@NotBlank`/`@NotNull`/`@Valid` on request DTOs |
| Retry | Spring Retry | `@Retryable` on tier change for graceful recovery from optimistic-lock losers |
| JSON | Jackson | Records serialize cleanly with no extra config; used for request/response and idempotency-cache payloads |
| Build | Maven | Spring Boot's reference build tool; reproducible without IDE setup |

## What it does

### Catalog
- Three plans (Monthly / Quarterly / Yearly) and three tiers (Silver / Gold / Platinum) with a 9-row pricing matrix.
- Per-tier benefit chain (configured as data, not code) made up of:
  - `FreeDeliveryBenefit` — waives the delivery fee
  - `ExtraDiscountBenefit` — percentage discount, `rate` parameter read from JSON config
  - `ExclusiveDealsBenefit` — flags eligible items, records a marketing reward
  - `PrioritySupportBenefit` — non-monetary priority-support flag
- Optional per-user **tier recommendation** computed from pluggable criteria:
  - `OrderCountCriterion` — trailing-90-day order count
  - `MonthlySpendCriterion` — trailing-6-month average monthly spend
  - `CohortCriterion` — cohort label override (`EARLY_ADOPTER` → PLATINUM, `LOYAL` → GOLD, `NEW` → SILVER)

### User actions
- `POST /api/v1/subscriptions` — subscribe to a (plan, tier), idempotent on `idempotencyKey`
- `POST /api/v1/subscriptions/{userId}/change-tier` — upgrade / downgrade tier on an active subscription
- `POST /api/v1/subscriptions/{userId}/cancel` — cancel; idempotent
- `GET /api/v1/subscriptions/{userId}/current` — current membership view + tier benefits
- `GET /api/v1/catalog?userId=...` — catalog with optional recommendation
- `POST /api/v1/checkout/apply-membership` — apply the user's benefit chain to a cart, idempotent on `idempotencyKey`

### Concurrency and data integrity
- `Subscription.@Version` optimistic locking. Tier change uses `@Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = 25ms × 2.0)`; after retries are exhausted a `@Recover` method translates the failure to HTTP 409.
- A generated `active_user_id` column on `subscription` (NULL when not ACTIVE) backed by a unique index enforces "at most one ACTIVE subscription per user" at the database level. Race losers surface as `DataIntegrityViolationException` and are mapped to 409.
- `IdempotencyService.runOnce(userId, key, operation, supplier)` persists serialized responses keyed by `(userId, key, operation)`. Replays return the cached response. Concurrent inserters that lose the race re-fetch and return the persisted response — at-least-once safe.
- `TierRecommendationService` uses `ConcurrentHashMap.compute(...)` so concurrent fillers for the same user collapse onto a single computation; failures fall back to SILVER so recommendation never blocks a subscription operation.

### Why these patterns

- **Strategy + Chain for benefits.** Adding a new benefit means writing one new `@Component` implementing `Benefit` and adding a `BenefitConfig` row — no edits to `BenefitEngine` or the controllers. The engine catches per-benefit `RuntimeException`s and records them on the response so a single misbehaving benefit can't fail checkout.
- **Strategy + Aggregator for tier recommendation.** Each criterion contributes a `(suggestion, strength)` vote; the service sums `strength × weight` per tier with deterministic tiebreakers. Adding a new criterion is purely additive.
- **Optimistic locking instead of pessimistic.** Membership operations are short-lived and concurrent contention on the same subscription is rare. `@Version` plus a small retry budget gives correctness without the throughput hit of row locks.
- **Idempotency at the application layer.** Subscribe and checkout are exposed as POSTs that clients may retry on transient errors. Persisting the response under a composite key gives at-least-once delivery without duplicate side effects.
- **Read-time `EXPIRED` projection.** Active queries filter `status = ACTIVE AND endAt > :now` so a stale ACTIVE row past its end is never considered active. No background job needed.

## Run it

Requirements: Java 17+ and Maven 3.9+.

```bash
mvn -DskipTests spring-boot:run
```

- App: http://localhost:8080
- H2 console: http://localhost:8080/h2 (JDBC URL `jdbc:h2:mem:membership`, user `sa`, blank password)
- Default profile is `demo`, which seeds the catalog plus three users with distinct order histories.

### Demo users seeded on startup

| User       | Cohort          | Order history           | Recommended tier |
|------------|-----------------|-------------------------|------------------|
| `u-bronze` | `NEW`           | 2 × $10                 | SILVER           |
| `u-mid`    | `LOYAL`         | 8 × $50                 | GOLD             |
| `u-vip`    | `EARLY_ADOPTER` | 20 × $100               | PLATINUM         |

### Quick API tour

```bash
# Get the catalog with a recommendation
curl -s "http://localhost:8080/api/v1/catalog?userId=u-vip" | jq

# Subscribe (idempotent on idempotencyKey)
curl -s -X POST http://localhost:8080/api/v1/subscriptions \
  -H 'Content-Type: application/json' \
  -d '{"userId":"u-mid","plan":"MONTHLY","tier":"GOLD","idempotencyKey":"sub-1"}' | jq

# Upgrade tier
curl -s -X POST http://localhost:8080/api/v1/subscriptions/u-mid/change-tier \
  -H 'Content-Type: application/json' \
  -d '{"targetTier":"PLATINUM"}' | jq

# Current membership
curl -s http://localhost:8080/api/v1/subscriptions/u-mid/current | jq

# Apply benefits at checkout (idempotent)
curl -s -X POST http://localhost:8080/api/v1/checkout/apply-membership \
  -H 'Content-Type: application/json' \
  -d '{
        "userId":"u-mid",
        "idempotencyKey":"co-1",
        "cart":{
          "items":[{"sku":"S1","qty":2,"unitPrice":12.50}],
          "subtotal":25.00,
          "deliveryFee":5.00
        }
      }' | jq

# Cancel
curl -s -X POST http://localhost:8080/api/v1/subscriptions/u-mid/cancel | jq
```

## Sample responses

Every response below is a real capture from a fresh `mvn spring-boot:run` against the seeded demo profile.

### `GET /api/v1/catalog` (anonymous)

```json
{
  "plans": [
    { "code": "MONTHLY", "durationDays": 30 },
    { "code": "QUARTERLY", "durationDays": 90 },
    { "code": "YEARLY", "durationDays": 365 }
  ],
  "tiers": [
    { "code": "SILVER", "rank": 1, "benefits": ["FREE_DELIVERY"] },
    { "code": "GOLD", "rank": 2, "benefits": ["FREE_DELIVERY", "EXTRA_DISCOUNT"] },
    { "code": "PLATINUM", "rank": 3, "benefits": ["FREE_DELIVERY", "EXTRA_DISCOUNT", "EXCLUSIVE_DEALS", "PRIORITY_SUPPORT"] }
  ],
  "prices": [
    { "plan": "MONTHLY",   "tier": "SILVER",   "amount": 4.9900,   "currency": "USD" },
    { "plan": "MONTHLY",   "tier": "GOLD",     "amount": 9.9900,   "currency": "USD" },
    { "plan": "MONTHLY",   "tier": "PLATINUM", "amount": 14.9900,  "currency": "USD" },
    { "plan": "QUARTERLY", "tier": "SILVER",   "amount": 12.9900,  "currency": "USD" },
    { "plan": "QUARTERLY", "tier": "GOLD",     "amount": 26.9900,  "currency": "USD" },
    { "plan": "QUARTERLY", "tier": "PLATINUM", "amount": 39.9900,  "currency": "USD" },
    { "plan": "YEARLY",    "tier": "SILVER",   "amount": 49.9900,  "currency": "USD" },
    { "plan": "YEARLY",    "tier": "GOLD",     "amount": 99.9900,  "currency": "USD" },
    { "plan": "YEARLY",    "tier": "PLATINUM", "amount": 149.9900, "currency": "USD" }
  ],
  "recommendedTier": null
}
```

### Tier recommendations per seeded user

```bash
$ curl -s "http://localhost:8080/api/v1/catalog?userId=u-bronze" | jq '.recommendedTier'
"SILVER"

$ curl -s "http://localhost:8080/api/v1/catalog?userId=u-mid" | jq '.recommendedTier'
"GOLD"

$ curl -s "http://localhost:8080/api/v1/catalog?userId=u-vip" | jq '.recommendedTier'
"PLATINUM"
```

The criteria see different signals per user (cohort label + trailing order count + monthly spend) and the deterministic aggregator picks one tier.

### Subscribe + idempotent replay + duplicate-active conflict

```bash
$ curl -s -X POST http://localhost:8080/api/v1/subscriptions \
    -H 'Content-Type: application/json' \
    -d '{"userId":"u-mid","plan":"MONTHLY","tier":"GOLD","idempotencyKey":"sub-1"}'
```
```json
{
  "id": "3e843d66-bc37-4d32-b71e-9a190209bc79",
  "userId": "u-mid",
  "plan": "MONTHLY",
  "tier": "GOLD",
  "priceCharged": 9.99,
  "status": "ACTIVE",
  "startAt": "2026-05-27T19:52:26.227324Z",
  "endAt":   "2026-06-26T19:52:26.227324Z",
  "canceledAt": null,
  "version": 0
}
```

Replay with the same `idempotencyKey` returns the same id and version (no second row created):

```bash
$ curl -s -X POST http://localhost:8080/api/v1/subscriptions \
    -H 'Content-Type: application/json' \
    -d '{"userId":"u-mid","plan":"MONTHLY","tier":"GOLD","idempotencyKey":"sub-1"}' \
    | jq '{id, version}'
{
  "id": "3e843d66-bc37-4d32-b71e-9a190209bc79",
  "version": 0
}
```

A second subscribe attempt without a matching key is rejected by the `active_user_id` unique constraint:

```bash
$ curl -s -o /tmp/r.json -w 'HTTP %{http_code}\n' -X POST http://localhost:8080/api/v1/subscriptions \
    -H 'Content-Type: application/json' \
    -d '{"userId":"u-mid","plan":"YEARLY","tier":"PLATINUM"}'
HTTP 409
```
```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "User u-mid already has an active subscription 3e843d66-bc37-4d32-b71e-9a190209bc79",
  "instance": "/api/v1/subscriptions"
}
```

### Change tier (preserves plan + endAt, bumps `@Version`)

```bash
$ curl -s -X POST http://localhost:8080/api/v1/subscriptions/u-mid/change-tier \
    -H 'Content-Type: application/json' \
    -d '{"targetTier":"PLATINUM"}'
```
```json
{
  "id": "3e843d66-bc37-4d32-b71e-9a190209bc79",
  "userId": "u-mid",
  "plan": "MONTHLY",
  "tier": "PLATINUM",
  "priceCharged": 9.99,
  "status": "ACTIVE",
  "startAt": "2026-05-27T19:52:26.227324Z",
  "endAt":   "2026-06-26T19:52:26.227324Z",
  "canceledAt": null,
  "version": 1
}
```

### Current membership

```bash
$ curl -s http://localhost:8080/api/v1/subscriptions/u-mid/current | jq
```
```json
{
  "active": true,
  "subscription": {
    "id": "3e843d66-bc37-4d32-b71e-9a190209bc79",
    "userId": "u-mid",
    "plan": "MONTHLY",
    "tier": "PLATINUM",
    "priceCharged": 9.99,
    "status": "ACTIVE",
    "startAt": "2026-05-27T19:52:26.227324Z",
    "endAt":   "2026-06-26T19:52:26.227324Z",
    "canceledAt": null,
    "version": 1
  },
  "benefits": ["FREE_DELIVERY", "EXTRA_DISCOUNT", "EXCLUSIVE_DEALS", "PRIORITY_SUPPORT"]
}
```

### Apply membership at checkout — PLATINUM member

```bash
$ curl -s -X POST http://localhost:8080/api/v1/checkout/apply-membership \
    -H 'Content-Type: application/json' \
    -d '{
          "userId":"u-mid",
          "idempotencyKey":"co-1",
          "cart": {
            "items":[{"sku":"S1","qty":2,"unitPrice":12.50}],
            "subtotal":25.00,
            "deliveryFee":5.00
          }
        }'
```
```json
{
  "subtotal": 22.5,
  "deliveryFee": 0.0,
  "totalAdjustment": -8.125,
  "prioritySupport": true,
  "appliedBenefits": [
    { "id": "FREE_DELIVERY",   "adjustment": -5.0,    "description": "Free delivery waived $5.0000" },
    { "id": "EXTRA_DISCOUNT",  "adjustment": -2.5,    "description": "Extra discount 10% applied" },
    { "id": "EXCLUSIVE_DEALS", "adjustment": -0.625,  "description": "Exclusive deals applied to 1 items" },
    { "id": "PRIORITY_SUPPORT","adjustment":  0.0,    "description": "Priority support enabled" }
  ],
  "failures": []
}
```

The chain ran `FREE_DELIVERY → EXTRA_DISCOUNT (10%) → EXCLUSIVE_DEALS → PRIORITY_SUPPORT` in `executionOrder`, each adjustment recorded on the response, `subtotal` and `deliveryFee` mutated by the strategies, and `totalAdjustment` aggregated.

### Apply membership at checkout — non-member passthrough

```bash
$ curl -s -X POST http://localhost:8080/api/v1/checkout/apply-membership \
    -H 'Content-Type: application/json' \
    -d '{
          "userId":"u-bronze",
          "cart": {
            "items":[{"sku":"S1","qty":1,"unitPrice":12.50}],
            "subtotal":12.50,
            "deliveryFee":5.00
          }
        }'
```
```json
{
  "subtotal": 12.5,
  "deliveryFee": 5.0,
  "totalAdjustment": 0,
  "prioritySupport": false,
  "appliedBenefits": [],
  "failures": []
}
```

### Cancel + post-cancel current

```bash
$ curl -s -X POST http://localhost:8080/api/v1/subscriptions/u-mid/cancel | jq '{tier, status, canceledAt}'
{
  "tier": "PLATINUM",
  "status": "CANCELED",
  "canceledAt": "2026-05-27T19:52:31.408Z"
}

$ curl -s http://localhost:8080/api/v1/subscriptions/u-mid/current | jq
{
  "active": false,
  "subscription": null,
  "benefits": null
}
```

### Concurrency invariant — at most one ACTIVE subscription per user

Five simultaneous subscribes for `u-bronze` with no idempotency key. Exactly one wins:

```bash
$ for i in 1 2 3 4 5; do
    curl -s -o /dev/null -w '%{http_code}\n' -X POST http://localhost:8080/api/v1/subscriptions \
      -H 'Content-Type: application/json' \
      -d '{"userId":"u-bronze","plan":"MONTHLY","tier":"SILVER"}' &
  done; wait | sort | uniq -c
   1 201
   4 409
```

The `active_user_id` generated column + unique index on `subscription` enforces the invariant at the database level; race losers surface as `DataIntegrityViolationException` and are mapped to HTTP 409 by `GlobalExceptionHandler`.

## Configuration

`application.yml`:

| Key                                | Default | Purpose                                                                |
|------------------------------------|---------|------------------------------------------------------------------------|
| `membership.recommendation.ttl`    | `PT5M`  | Freshness window for the in-memory tier recommendation cache           |
| `membership.retry.maxAttempts`     | `3`     | Max attempts for `@Retryable` on optimistic-lock losses (informational; the annotation hardcodes 3) |
| `spring.profiles.active`           | `demo`  | The `demo` profile enables `SeedDataLoader`                            |
| `spring.datasource.url`            | `jdbc:h2:mem:membership;DB_CLOSE_DELAY=-1` | In-memory H2 |

## Project layout

```
src/main/java/com/firstclub/membership
├── MembershipProgramApplication.java   // @SpringBootApplication + @EnableRetry
├── controller/                         // REST controllers + DTOs (thin transport layer)
│   ├── CatalogController
│   ├── SubscriptionController
│   ├── CheckoutController
│   └── dto/                            // request/response records with Jakarta validation
├── service/                            // Application services
│   ├── MembershipCatalogService
│   ├── SubscriptionService             // @Transactional + @Retryable + idempotency
│   ├── CheckoutIntegrationService
│   └── IdempotencyService
├── domain/                             // JPA entities + enums
│   ├── User, Subscription, SubscriptionHistory
│   ├── PlanTierPrice, BenefitConfig, IdempotencyRecord, Order
│   └── MembershipPlan, MembershipTier, BenefitId, SubscriptionStatus
├── repository/                         // Spring Data JPA repositories
├── benefit/                            // Strategy + Chain
│   ├── Benefit (interface), BenefitEngine, BenefitRegistry, CheckoutContext
│   └── impl/ FreeDeliveryBenefit, ExtraDiscountBenefit, ExclusiveDealsBenefit, PrioritySupportBenefit
├── tier/                               // Strategy + Aggregator
│   ├── TierCriterion, TierCriterionRegistry, TierRecommendationService
│   └── impl/ OrderCountCriterion, MonthlySpendCriterion, CohortCriterion
├── exception/                          // ValidationException, NotFoundException, ConflictException, MissingPriceException
└── config/                             // ClockConfig, SeedDataLoader, GlobalExceptionHandler

src/main/resources
├── application.yml
└── schema.sql                          // H2 DDL with generated active_user_id + unique index
```

## Extensibility cheat sheet

- **New benefit** → `@Component implements Benefit`, declare a `BenefitId`, add `BenefitConfig` rows. Done.
- **New tier criterion** → `@Component implements TierCriterion` with id/weight/evaluate. The aggregator picks it up automatically.
- **New tier or plan** → add an enum value, add `PlanTierPrice` rows, optionally extend the `BenefitConfig` seed.
- **Swap H2 for Postgres** → swap the JDBC driver in `pom.xml`, change `spring.datasource.url`, and adjust `schema.sql` (the partial-active uniqueness can use a real partial unique index in Postgres).

## Status

- `mvn package` builds clean (69 source files compile).
- App boots on port 8080, seeds 9 prices / 7 benefit configs / 3 users / 30 orders, every endpoint smoke-tested end to end.
