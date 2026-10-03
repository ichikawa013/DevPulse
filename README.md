# DevPulse

A real-time developer activity platform built as a microservices system, where developers can post updates, browse a feed, and (soon) react, comment, and receive live notifications.

This project is being built in phases as a hands-on exploration of microservice architecture, GraphQL, event-driven design (Kafka), and real-time delivery (WebSockets), with a focus on the small design decisions that affect scalability and performance.

## Architecture

```mermaid
flowchart TB
    Client["Client browser"]
    Gateway["api-gateway<br/>JWT validation, rate limiting, proxy"]

    Client --> Gateway

    Gateway --> UserSvc["user-service<br/>Auth, JWT, profile"]
    Gateway --> FeedSvc["feed-service<br/>Posts, GraphQL"]
    Gateway --> MediaSvc["media-service<br/>MinIO uploads"]

    UserSvc --> UserDB[("devpulse_users")]
    FeedSvc --> FeedDB[("devpulse_feed")]
    MediaSvc --> MinIO[("MinIO storage")]

    FeedSvc -- publishes PostEvent --> Topic{{"post-events<br/>Kafka topic"}}
    Topic -- consumes --> NotifSvc["notification-service<br/>Consume, persist, push"]
    NotifSvc --> NotifDB[("devpulse_notifications")]
    NotifSvc -- STOMP over WebSocket --> Client
```

**Design principles:**
- Each service owns its own database (schema-per-service) — no shared tables across services.
- The gateway is a pure proxy: it validates JWTs and rate-limits once at the edge, then forwards a trusted `X-User-Id` header downstream. No other service re-validates JWTs.
- Each service owns its own GraphQL schema; the gateway does not aggregate or expose GraphQL itself.
- Cross-service communication for events (e.g. new posts triggering notifications) goes through Kafka, not synchronous service-to-service calls.

## Tech stack

- **Language / framework:** Java 25, Spring Boot 3.5.14, Spring Cloud 2025.0.0, Spring Cloud Gateway (webmvc)
- **API:** GraphQL (Spring for GraphQL)
- **Database:** PostgreSQL 18, one schema per service, Flyway migrations
- **Caching / rate limiting:** Redis (Lettuce), Bucket4j
- **Messaging:** Apache Kafka
- **Object storage:** MinIO
- **Auth:** JWT (jjwt), Spring Security
- **AI / Observability:** Spring AI 1.1.6
- **Orchestration:** Docker Compose (local), Kubernetes (kind for local cluster)
- **Load testing:** k6

## Services

| Service | Port | Responsibility |
|---|---|---|
| `api-gateway` | 8080 | JWT validation, rate limiting, routing to downstream services |
| `user-service` | 8081 | Registration, login, JWT issuance, refresh tokens, profile |
| `feed-service` | 8082 | Posts, cursor-paginated feed, GraphQL subscriptions, Kafka producer |
| `notification-service` | 8083 | Kafka consumer, persists and pushes notifications over STOMP/WebSocket |
| `media-service` | 8084 | Server-side image upload for post images and profile pictures via MinIO (scaffolded, not yet implemented) |
| `ai` | — | Spring AI MCP server exposing observability tools: Kafka lag, pod status, logs, and latency |
| `agent` | — | Agentic client (Gemini-backed) that calls the `ai` MCP server to answer observability queries |

## Load testing

The async pipeline (HTTP → Kafka → Redis pub/sub → WebSocket) is load-tested end-to-end with k6, including a multi-instance Redis fanout check and gateway rate-limit validation. See [`load-tests/README.md`](./load-tests/README.md) for results, methodology, and known limitations.

## Progress

- [x] **Phase 1** — Project scaffolding: Spring Boot, PostgreSQL, Flyway, Docker Compose, GraphQL, Kafka, and initial architecture design across all services.
- [x] **Phase 2** — `user-service`: registration, JWT (access + refresh tokens in Redis), login, logout, profile queries/mutations.
- [x] **Phase 3** — `api-gateway`: JWT validation filter, Redis-backed rate limiting (Bucket4j), request routing, identity header propagation to downstream services.
- [x] **Phase 4** — `feed-service`: post creation/deletion, cursor-based (keyset) pagination, Relay-style GraphQL connections, GraphQL subscriptions over `graphql-ws`, Kafka producer emitting post events.
- [x] **Phase 5** — `notification-service`: Kafka consumer for post events, persisted notifications, real-time delivery over STOMP/WebSocket, Redis pub/sub for cross-instance fanout.
- [x] **Phase 5.1** — Reactions: `feed-service` reaction mutation (create/update) publishing to a `reaction-events` Kafka topic, consumed by `notification-service` and delivered through the same WebSocket pipeline as post notifications.
- [ ] **Phase 6** — `media-service`: server-side image upload for post images and profile pictures. Client sends a multipart file directly to `media-service` (no presigned MinIO URLs), which validates the real file type (Apache Tika, not the client-supplied `Content-Type`), compresses/resizes it, stores it in MinIO, and returns a URL — which the client then passes into the existing `createPost`/`updateProfile` mutations. Exposed as a plain REST multipart endpoint rather than GraphQL, since GraphQL's multipart upload spec adds more setup than it's worth here. No Kafka event on upload — this flow needs no cross-service messaging.
- [x] **Phase 6.1** — Kubernetes deployment: service deployment manifests in `k8/` for gateway, user-service, feed-service, and notification-service; local `kind` cluster configuration for local development and testing.
- [x] **Phase 6.2** — Observability: AI-powered observability agent with Spring AI integration, exposing tools to query Kafka lag, pod status, system logs, and inter-service latency — enabling real-time cluster troubleshooting.

## Releases

| Version | Phase | Key Features |
|---|---|---|
| [v0.2.0](https://github.com/ichikawa013/DevPulse/releases/tag/v0.2.0) | Phase 2 | User service: registration, login, JWT (access + refresh tokens), profile management |
| [v0.3.0](https://github.com/ichikawa013/DevPulse/releases/tag/v0.3.0) | Phase 3 | API gateway: JWT validation, Redis-backed rate limiting (Bucket4j), request routing |
| [v0.4.0](https://github.com/ichikawa013/DevPulse/releases/tag/v0.4.0) | Phase 4 | Feed service: post creation/deletion, cursor-based pagination, GraphQL subscriptions, Kafka producer |
| [v0.5.0](https://github.com/ichikawa013/DevPulse/releases/tag/v0.5.0) | Phase 5 | Notification service: Kafka consumer, Redis pub/sub fanout, STOMP/WebSocket delivery |
| [v0.5.1](https://github.com/ichikawa013/DevPulse/releases/tag/v0.5.1) | Phase 5.1 | Reactions: GraphQL mutation in feed-service, Kafka producer, consumer delivery in notification-service |
| [v0.5.2](https://github.com/ichikawa013/DevPulse/releases/tag/v0.5.2) | Phase 5.2 | Fix: IP address-based rate limiting restoration |
| [v0.6.0](https://github.com/ichikawa013/DevPulse/releases/tag/v0.6.0) | Phase 6.1 + 6.2 | Kubernetes deployment (kind), AI observability agent (Spring AI + MCP) |

### Key design decisions (Phase 4)

**Cursor-based pagination over offset pagination.** Offset pagination becomes inefficient as the dataset grows — the database still has to scan and discard every skipped row, and results can shift if new rows are inserted mid-scroll. Feed pagination instead uses a composite index on `(created_at DESC, id DESC)`, where each page's cursor is the last row seen rather than a row count. This gives stable ordering under concurrent writes and maps directly onto Relay-style GraphQL connections (`edges` / `pageInfo`).

**UUIDv7 over UUIDv4 for primary keys.** Random UUIDs (v4) fragment the B-tree index over time, since every insert lands in a random position. UUIDv7 embeds a timestamp prefix, so IDs are naturally sortable and inserts stay roughly sequential — combining the collision-resistance of a UUID with the index locality of an auto-increment key.

### Key design decisions (Phase 5)

**Redis pub/sub for cross-instance WebSocket delivery.** A user's WebSocket connection is only held by one `notification-service` instance at a time, so a naive setup would miss users connected to a different instance than the one that consumed the Kafka event. Publishing each notification to a Redis channel lets every instance subscribe and forward to its own locally-connected clients, decoupling "which instance received the Kafka message" from "which instance holds the user's socket."

### Key design decisions (Phase 6)

**Kubernetes-first deployment.** Service manifests in `k8/` are written to deploy the gateway, user-service, feed-service, and notification-service to a local `kind` cluster. This enables testing multi-instance scaling, cross-zone networking, and cluster-level observability patterns without leaving the development machine.

**AI-powered observability.** The `ai` module exposes observability tools via Spring AI (KafkaLagTool, PodStatusTool, LogTool, LatencyTool), enabling LLM-driven queries like "What's the current Kafka lag?" or "Which pod is consuming the most CPU?" The `agent` module wraps these as an MCP server, integrating with IDE and AI tooling for real-time cluster insights.

## Kubernetes deployment

Deploy to a local `kind` cluster:

1. Create the cluster with port mapping:
   ```bash
   kind create cluster --config kind_config.yaml
   ```
2. Build and load images into kind:
   ```bash
   docker build -t api-gateway:latest ./services/api-gateway
   kind load docker-image api-gateway:latest
   # Repeat for each service
   ```
3. Apply service manifests:
   ```bash
   kubectl apply -f k8/
   ```
4. Forward the API gateway port:
   ```bash
   kubectl port-forward svc/api-gateway 8080:8080
   ```

The gateway is then accessible at `http://localhost:8080`.

### Observability in Kubernetes

The `ai` and `agent` modules provide real-time observability:

- **Kafka Lag Tool** — queries current consumer group lag per partition
- **Pod Status Tool** — lists pods, their resource usage (CPU/memory), and restart counts
- **Log Tool** — streams logs from any pod in the cluster
- **Latency Tool** — measures inter-service request latencies and histograms

These tools are exposed as MCP server endpoints, allowing integration with IDE plugins, AI chat interfaces, or custom dashboards.

### Testing observability with load and failure scenarios

Generate logs and metrics for the observability agent to consume:

```bash
docker compose up -d
cd load-tests
k6 run -e VUS=10 loadtest.js
k6 run -e VUS=25 loadtest.js
k6 run -e VUS=50 loadtest.js
```

For crash testing and log generation, scale service replicas:

```bash
# Scale notification-service to 3 replicas (watch for failover)
kubectl scale deployment notification-service --replicas=3

# Scale back to 1 replica
kubectl scale deployment notification-service --replicas=1

# Kill a pod to trigger restart logs
kubectl delete pod <pod-name> -n default
```

The observability agent will track Kafka lag, pod failures, recovery times, and service latencies throughout these scenarios.

## Local development setup

**Prerequisites:** Docker, JDK 25, IntelliJ IDEA (or any IDE with Maven support)

1. Start infrastructure (PostgreSQL, Redis, Kafka, MinIO):
   ```bash
   docker compose up -d
   ```
2. Run each service from IntelliJ run configurations (or `mvn spring-boot:run` per module).
3. Notes on local config:
    - PostgreSQL should be addressed as `127.0.0.1:5432`, not `localhost`, to avoid a Windows IPv6 resolution issue.
    - Redis runs on `127.0.0.1:6379`.
    - Each service runs its own Flyway migrations on startup against its own database.

## Testing GraphQL directly

Each service exposes its own GraphiQL UI for local testing, independent of the gateway:

- `user-service`: `http://localhost:8081/graphiql`
- `feed-service`: `http://localhost:8082/graphiql`

When testing a service directly (bypassing the gateway), you can manually set the `X-User-Id` header in GraphiQL's headers panel to simulate an authenticated request, since the gateway is normally what stamps this header after validating a JWT.

To test the full authenticated flow through the gateway, obtain a JWT via `user-service`'s `login` mutation, then send it as `Authorization: Bearer <token>` to `http://localhost:8080/<service>/graphql` — no `X-User-Id` header needed, since the gateway derives and attaches it from the token.