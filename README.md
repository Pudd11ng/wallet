# 🏦 Distributed Wallet Core Engine

![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4+-brightgreen.svg)
![Architecture](https://img.shields.io/badge/Architecture-Microservices-blue.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)
![Kafka](https://img.shields.io/badge/Kafka-Event%20Driven-black.svg)
![Coverage](https://img.shields.io/badge/Coverage-100%25-success.svg)

> A simple digital wallet backend built on microservices — featuring double-entry bookkeeping, RSA-signed payloads, event-driven processing, and idempotent financial transactions.

---

## 📖 What Is This?

This is a simple**digital e-wallet application** built with **Microservices Architecture**. Users can:

- **Register & authenticate** with JWT-secured endpoints
- **Provision a wallet** linked to their identity
- **Top-up funds** from external sources into the ledger
- **Transfer money peer-to-peer (P2P)** with full ACID guarantees
- **Generate / scan QR codes** for payments (HMAC & DuitNow EMVCo formats)

---

## 🛠️ Tech Stack

| Layer | Technology |
| :--- | :--- |
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.4+ (Web, WebFlux, Security) |
| **Database** | PostgreSQL 15 — strict relational ledger via MyBatis |
| **Cache & Locks** | Redis 7 — token-bucket rate limiting & idempotency locks |
| **Event Broker** | Apache Kafka + Zookeeper — async event-driven communication |
| **Service Discovery** | Netflix Eureka Server |
| **Inter-Service RPC** | gRPC & Protocol Buffers |
| **Security** | JWT, RSA-256 asymmetric signatures, BCrypt hashing |

---

## 🏗️ Architecture Overview

### Multi-Module Maven Monorepo

The project enforces a **strict separation of concerns** while sharing core contracts across the entire microservice ecosystem.

```
wallet-parent/
├── wallet-gateway            → Reactive API Gateway       :8089
├── wallet-service-core       → Financial Execution Engine  :8081
├── wallet-notification       → Async Event Consumer        :8082
├── wallet-auth-service       → Identity Provider           :8083
├── wallet-discovery-server   → Eureka Service Registry     :8761
└── wallet-common             → Shared DTOs & Contracts     (JAR)
```

<details>
<summary><b>📦 Module Details</b> (click to expand)</summary>

| Module | Responsibility |
| :--- | :--- |
| **`wallet-gateway`** | Reactive Spring Cloud Gateway — handles rate limiting, JWT validation, and RSA digital signature verification. All external traffic enters here. |
| **`wallet-service-core`** | The execution engine. Manages the PostgreSQL ledger via MyBatis, coordinates Redis distributed locks, and implements the double-entry bookkeeping model. |
| **`wallet-notification`** | Kafka consumer that processes asynchronous events — logging, push notifications, receipt delivery. |
| **`wallet-auth-service`** | Identity provider managing user credentials, BCrypt encryption, and gRPC-based identity serving for internal calls. |
| **`wallet-discovery-server`** | Netflix Eureka server for dynamic microservice registry and internal routing. |
| **`wallet-common`** | Shared JAR containing Java 21 Record DTOs, base handlers, Protocol Buffer definitions, and exception contracts. |

</details>

---

## 🔒 Security & Reliability

### Authentication
Stateless **JWT tokens** injected into the `Authorization` header on every request.

### Payload Integrity (Non-Repudiation)
Every critical state-changing request must be **mathematically signed** using the client's RSA private key. The Gateway verifies this signature using the stored public key — ensuring amounts or target accounts were **not tampered with** mid-flight.

### Idempotency Guard
A unique `X-Request-ID` header ensures that **network retries never result in duplicate charges**. Redis caches the state of each ID for 24 hours.

### Rate Limiting (Token Bucket via Redis)

| Endpoint | Limit |
| :--- | :--- |
| `POST /transfer` | 2 requests / sec / user |
| `GET /history` | 10 requests / sec / user |

---

## 🗄️ Database Design

### Database — `wallet_db`

| Table | Purpose |
| :--- | :--- |
| `users` | System identities & securely hashed credentials |
| `wallets` | Current available balance per user (with **optimistic locking** to prevent race conditions) |
| `transaction_requests` | API request lifecycle tracking & idempotency enforcement |
| `journal_entries` | **Immutable ledger** — every transaction recorded as paired debits and credits |
| `outbox_events` | Queues Kafka events within the local DB transaction to guarantee **eventual delivery** |

---

## 🚀 Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose

### 1 · Boot the Infrastructure

The included `docker-compose.yml` provisions all backing services:

| Service | Address |
| :--- | :--- |
| PostgreSQL | `localhost:5432` |
| Redis | `localhost:6379` |
| Zookeeper | `localhost:2181` |
| Kafka | `localhost:9092` |

```bash
docker compose up -d
```

### 2 · Launch the Microservices

Start the Spring Boot applications **in this exact order** to ensure successful Eureka registration:

```
1.  DiscoveryServerApplication
2.  AuthApplication
3.  CoreApplication
4.  NotificationApplication
5.  GatewayApplication
```

---

## 🧪 Postman API Walkthrough

> A complete **Postman Collection** and **Environment** are provided in the `public/postman` directory.

### Setup

1. **Import** — Open Postman → *Import* → select `wallet.postman_collection.json` and `Wallet.postman_environment.json`.
2. **Activate** — Set the active environment to **"Wallet"** in the top-right corner.
3. **Configure RSA Key** — The collection includes an automated pre-request script (using `node-forge`) that calculates RSA-256 signatures. Generate a valid RSA private key and paste it into the `rsa_private_key` environment variable.

### Step-by-Step Execution

| Step | Action | Notes |
| :---: | :--- | :--- |
| **1** | **Register & Login** — Run `/auth/register`, then `/auth/login`. | Copy the `token` from the response into the `jwt_token` environment variable. |
| **2** | **Initialize Ledger** — Run `/wallets/initialize`. | The `userId` in the payload must match the one returned during login. |
| **3** | **Top-Up & Transfer** — Run `/wallets/topup`, then `/wallets/transfer`. | See the idempotency note below. |
| **4** | **QR Codes** — Run `/qr/generate`, then `/qr/decode`. | Supports two strategy patterns (see below). |
| **5** | **Verify Ledger** — Run `/wallets/{walletId}/history`. | View immutable double-entry records & real-time balance. |

> [!WARNING]
> **Idempotency Rule** — Clicking *Send* multiple times on a transfer returns a cached `200 OK` without moving funds twice. To execute a **new** transfer, change `X-Request-ID` to a fresh UUID.

#### QR Strategy Patterns

| Strategy | Description |
| :--- | :--- |
| `INTERNAL_HMAC` | Generates a lightweight, proprietary HMAC-signed string. |
| `DUITNOW_TLV` | Generates an EMVCo National Standard string with dynamic CRC-16 checksum calculation. |

---

## 📡 API Reference

> All requests are routed through `wallet-gateway` at **`http://localhost:8089`**.

### Required Security Headers

All state-changing endpoints require these headers:

```
Authorization:   Bearer <jwt_token>
X-Client-Id:     <user_or_merchant_id>
X-Request-Time:  <unix_timestamp_ms>          # Replay-attack prevention
X-Request-ID:    <uuid>                        # Idempotency key
Signature:       algorithm=RSA256, keyVersion=1, signature=<base64_signature>
```

### Example — P2P Transfer

Transfers funds between two internal wallets. Triggers the Facade → Handler Chain → Double-Entry DB transaction.

**`POST /api/v1/wallets/transfer`**

```http
POST /api/v1/wallets/transfer
Content-Type: application/json
Authorization: Bearer eyJhbG...
X-Client-Id: U-882910
X-Request-Time: 1708345050000
X-Request-ID: txn-p2p-7731
Signature: algorithm=RSA256, keyVersion=1, signature=AqFcNDNLXtoIgx...

{
  "fromWalletId": "W-10045",
  "toWalletId": "W-20099",
  "amount": "25.50",
  "remark": "Dinner split"
}
```

**Response `200 OK`**

```json
{
  "resultStatus": "S",
  "resultCode": "SUCCESS",
  "data": {
    "transactionId": "TXN-9921-7731",
    "status": "COMPLETED",
    "timestamp": "2026-02-19T19:54:27Z"
  }
}
```

---

## 📬 Kafka Event Contracts

Once the **Transactional Outbox** safely writes an event to the DB, a background relay pushes it to Kafka for `wallet-notification` to consume.

**Topic:** `wallet.events.transaction.completed`

```json
{
  "eventId": "EVT-88219-uuid",
  "transactionId": "TXN-9921-7731",
  "eventType": "P2P_TRANSFER",
  "senderWalletId": "W-10045",
  "receiverWalletId": "W-20099",
  "amount": "25.50",
  "currency": "MYR",
  "timestamp": 1708345050000
}
```

---

<p align="center">
  Built with ☕ Java 21 · Spring Boot · PostgreSQL · Kafka · Redis
</p>