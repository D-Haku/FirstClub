-- FirstClub Membership Program schema (H2)
--
-- This script is executed by Spring Boot at startup because
-- spring.jpa.defer-datasource-initialization=true and ddl-auto=none.
-- DROP IF EXISTS guards make the script safe to run repeatedly against
-- in-memory or persistent H2 databases.
--
-- Note: "ORDER" is a reserved word in SQL, so the orders table is named
-- "orders" everywhere (entity, repository, schema). H2 2.x supports
-- partial (filtered) unique indexes via the "WHERE" clause used below
-- to enforce at-most-one ACTIVE subscription per user.

DROP INDEX IF EXISTS ux_subscription_active_user;
DROP TABLE IF EXISTS idempotency_record;
DROP TABLE IF EXISTS subscription_history;
DROP TABLE IF EXISTS subscription;
DROP TABLE IF EXISTS benefit_config;
DROP TABLE IF EXISTS plan_tier_price;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id          VARCHAR(64)  NOT NULL,
    email       VARCHAR(255),
    cohort      VARCHAR(64),
    created_at  TIMESTAMP    NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

CREATE TABLE plan_tier_price (
    id        BIGINT        NOT NULL AUTO_INCREMENT,
    plan      VARCHAR(32)   NOT NULL,
    tier      VARCHAR(32)   NOT NULL,
    amount    DECIMAL(19,4) NOT NULL,
    currency  VARCHAR(3)    NOT NULL,
    CONSTRAINT pk_plan_tier_price PRIMARY KEY (id),
    CONSTRAINT ux_plan_tier_price_plan_tier UNIQUE (plan, tier)
);

CREATE TABLE subscription (
    id              VARCHAR(64)   NOT NULL,
    user_id         VARCHAR(64)   NOT NULL,
    plan            VARCHAR(32)   NOT NULL,
    tier            VARCHAR(32)   NOT NULL,
    price_charged   DECIMAL(19,4) NOT NULL,
    status          VARCHAR(32)   NOT NULL,
    start_at        TIMESTAMP     NOT NULL,
    end_at          TIMESTAMP     NOT NULL,
    canceled_at     TIMESTAMP     NULL,
    version         BIGINT        NOT NULL,
    idempotency_key VARCHAR(128)  NULL,
    -- Generated column that surfaces the user_id only while the
    -- subscription is ACTIVE. Combined with a UNIQUE index below this
    -- enforces "at most one ACTIVE subscription per user" without
    -- requiring a partial index (which H2 2.x does not support).
    -- Unique indexes in H2 ignore NULLs, so non-ACTIVE rows do not
    -- collide with one another.
    active_user_id  VARCHAR(64) AS (CASE WHEN status = 'ACTIVE' THEN user_id END),
    CONSTRAINT pk_subscription PRIMARY KEY (id)
);

CREATE UNIQUE INDEX ux_subscription_active_user
    ON subscription(active_user_id);

CREATE TABLE subscription_history (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    subscription_id  VARCHAR(64)  NOT NULL,
    previous_tier    VARCHAR(32)  NOT NULL,
    new_tier         VARCHAR(32)  NOT NULL,
    changed_at       TIMESTAMP    NOT NULL,
    reason           VARCHAR(255),
    CONSTRAINT pk_subscription_history PRIMARY KEY (id)
);

CREATE TABLE benefit_config (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    tier            VARCHAR(32)    NOT NULL,
    benefit_id      VARCHAR(64)    NOT NULL,
    execution_order INT            NOT NULL,
    params_json     VARCHAR(2000),
    CONSTRAINT pk_benefit_config PRIMARY KEY (id)
);

CREATE TABLE orders (
    id         VARCHAR(64)   NOT NULL,
    user_id    VARCHAR(64)   NOT NULL,
    total      DECIMAL(19,4) NOT NULL,
    placed_at  TIMESTAMP     NOT NULL,
    CONSTRAINT pk_orders PRIMARY KEY (id)
);

CREATE TABLE idempotency_record (
    user_id        VARCHAR(64)  NOT NULL,
    idem_key       VARCHAR(128) NOT NULL,
    operation      VARCHAR(64)  NOT NULL,
    request_hash   VARCHAR(128) NOT NULL,
    response_json  CLOB,
    created_at     TIMESTAMP    NOT NULL,
    CONSTRAINT pk_idempotency_record PRIMARY KEY (user_id, idem_key, operation)
);
