# Notification Center

<p align="center">
  <a href="https://openjdk.org/"><img src="https://img.shields.io/badge/Java-21-ED8B00?style=flat&logo=openjdk&logoColor=white" alt="Java" /></a>
  <a href="https://spring.io/projects/spring-boot"><img src="https://img.shields.io/badge/Spring_Boot-3.2-6DB33F?style=flat&logo=springboot&logoColor=white" alt="Spring Boot" /></a>
  <a href="https://www.postgresql.org/"><img src="https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat&logo=postgresql&logoColor=white" alt="PostgreSQL" /></a>
  <a href="https://www.docker.com/"><img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=flat&logo=docker&logoColor=white" alt="Docker" /></a>
  <br />
  <a href="https://react.dev/"><img src="https://img.shields.io/badge/React-19-61DAFB?style=flat&logo=react&logoColor=black" alt="React" /></a>
  <a href="https://www.typescriptlang.org/"><img src="https://img.shields.io/badge/TypeScript-5.x-3178C6?style=flat&logo=typescript&logoColor=white" alt="TypeScript" /></a>
  <a href="https://vite.dev/"><img src="https://img.shields.io/badge/Vite-6.x-646CFF?style=flat&logo=vite&logoColor=white" alt="Vite" /></a>
  <a href="https://tailwindcss.com/"><img src="https://img.shields.io/badge/Tailwind_CSS-4-06B6D4?style=flat&logo=tailwindcss&logoColor=white" alt="Tailwind CSS" /></a>
</p>

Centralized notification receiver and dashboard for self-hosted services and homelab infrastructure. It ingests alerts over HTTP POST, verifies service API tokens, streams updates in real time to a web dashboard, and dispatches Web Push notifications to mobile devices.


---

## Architecture and Design

### Ingestion and Token Security
- External services send alerts to `POST /api/v1/notifications` using a Bearer token (`nc_live_*`).
- Tokens are stored as SHA-256 hashes with a visible prefix for identification. Raw tokens are generated once and never stored in plain text.
- The `source` field (e.g. `studyzen`, `proxmox`) is bound to the token and determined by the server upon authentication, preventing callers from spoofing origin services.
- Alert deduplication via `dedupKey`: when an unread notification with the same key exists, its message and timestamp are updated instead of creating a duplicate row.

### Dashboard and Access Control
- Single-user web interface with no public registration.
- Web authentication uses BCrypt password verification and issues `HttpOnly`, `SameSite=Strict` session cookies.
- Login endpoint includes in-memory rate limiting with temporary lockouts against brute-force attempts.
- Tokens can be created with granular scopes (`notify` for ingestion, `read` for consuming services such as the Home widget).
- Responsive UI designed for mobile, tablet, and desktop viewports, featuring touch-friendly cards and a dedicated mobile bottom navigation bar.

### Real-Time Delivery and Web Push
- Dashboard updates use Server-Sent Events (`GET /api/v1/stream`) with unbuffered proxy streaming and periodic heartbeats.
- Native Web Push delivery (RFC 8291 / RFC 8292) using ECDH NIST P-256 (secp256r1) and VAPID authentication, delivering mobile push notifications without relying on third-party push services.
- Background delivery with automatic subscription pruning upon receiving HTTP 404 or 410 Gone responses from browser push services (FCM, APNs, Mozilla Autopush).
- Installable Progressive Web App (PWA) with a dedicated service worker (`sw.js`) and web app manifest.

---

## Tech Stack

- **Backend**: Java 21, Spring Boot 3.2, PostgreSQL 16.
- **Frontend**: React 19, TypeScript, Vite, Tailwind CSS 4.
- **Containers**: Docker Compose.

---

## Directory Structure

```text
NotificationCenter/
├── backend/                  # Spring Boot service
│   ├── src/main/java/com/frandm/notificationcenter/
│   │   ├── auth/             # Session login, logout, me endpoint
│   │   ├── ingest/           # POST /api/v1/notifications (Bearer notify scope)
│   │   ├── notification/     # CRUD, filtering, read status
│   │   ├── push/             # Web Push (VAPID keypair generation and dispatch)
│   │   ├── security/         # Dual authentication filter (tokens and session cookies)
│   │   ├── stream/           # SSE emitter service and unbuffered endpoint
│   │   └── token/            # Token generation, hashing, and lifecycle
│   ├── src/main/resources/
│   │   ├── db/migration/     # Flyway SQL migrations
│   │   └── application.yml
│   └── pom.xml
├── frontend-web/             # React dashboard
│   ├── public/               # Service worker (sw.js) and manifest.webmanifest
│   ├── src/
│   │   ├── components/       # Filter bar, alert cards, token manager, test modal, navbar
│   │   ├── context/          # Authentication state provider
│   │   ├── hooks/            # SSE subscription hook with auto-reconnect
│   │   ├── services/         # Axios API clients and Push registration
│   │   └── types/            # TypeScript interfaces
│   └── package.json
├── docker-compose.yml        # Multi-container deployment definition
└── .env.example              # Environment variables template
```

---

## Getting Started

### Prerequisites
- Docker and Docker Compose (production / containerized run)
- Or locally: Java 21+, Maven 3.9+, Node.js 20+, PostgreSQL 16

### 1. Environment Configuration
Copy the template and set passwords, secrets, and VAPID keys:

```bash
cp .env.example .env
```

Ensure `JWT_SECRET`, `DB_PASSWORD`, and `ADMIN_PASSWORD` are defined before launching.

### 2. Run with Docker Compose

```bash
docker compose up -d --build
```

### 3. Local Development

Backend:
```bash
cd backend
mvn spring-boot:run
```

Frontend:
```bash
cd frontend-web
npm install
npm run dev
```

---

## API Overview

### Ingestion

**`POST /api/v1/notifications`**

Headers:
```http
Authorization: Bearer nc_live_0123456789abcdef...
Content-Type: application/json
```

Body:
```json
{
  "title": "Upcoming Exam",
  "body": "Exam for tomorrow...",
  "level": "WARNING",
  "url": "https://studyzen.frandm.dev",
  "dedupKey": "studyzen"
}
```

Supported levels: `INFO`, `SUCCESS`, `WARNING`, `ERROR`.

### Management and Consumption

- `GET /api/v1/notifications` — List alerts with query filters (`source`, `read`, `level`, `page`, `size`).
- `GET /api/v1/notifications/sources` — Distinct list of reporting sources.
- `GET /api/v1/notifications/unread-count` — Count of unread notifications.
- `PATCH /api/v1/notifications/{id}/read` — Mark an alert as read.
- `POST /api/v1/notifications/read-all` — Mark all alerts as read.
- `DELETE /api/v1/notifications/{id}` — Delete an alert.
- `GET /api/v1/stream` — Connect to the SSE event stream.
- `GET /api/v1/tokens` — List existing API tokens.
- `POST /api/v1/tokens` — Create an API token (displays raw token once).
- `PATCH /api/v1/tokens/{id}/revoke` — Revoke an active token.
- `DELETE /api/v1/tokens/{id}` — Delete an API token.
- `POST /api/v1/push/generate-keys` — Generate a fresh VAPID EC P-256 keypair.
- `GET /api/v1/push/vapid-public-key` — Retrieve active public VAPID key.
- `POST /api/v1/push/subscribe` — Register a browser push subscription.
- `POST /api/v1/auth/login` — Authenticate admin session.
- `POST /api/v1/auth/logout` — Invalidate admin session.
- `GET /api/v1/auth/me` — Verify current session status.

---

## Build and Verification

Run backend unit and integration tests:
```powershell
mvn test
```

Run frontend production bundle check:
```powershell
npm --prefix frontend-web run build
```
