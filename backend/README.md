# ReY Platform Coordinator Service (Backend Worker)

This directory contains the Cloudflare Worker serverless code representing the API Gateway and synchronization hub.

## Folder Organization

- **[`/db`](file:///C:/Users/yasao/.gemini/antigravity/scratch/ReY-Platform/backend/db)**: Contains SQLite schema.sql initialization schemas.
- **[`/src`](file:///C:/Users/yasao/.gemini/antigravity/scratch/ReY-Platform/backend/src)**: Hono sub-routers for authentication, location tracking, gallery sync, RAG search matching, and telemetry analytics reporting.
- **[`wrangler.toml`](file:///C:/Users/yasao/.gemini/antigravity/scratch/ReY-Platform/backend/wrangler.toml)**: System environment bindings and parameters definitions.

## Key APIs

- `/api/v1/auth/*`: Session validation and PBKDF2 recovery flows.
- `/api/v1/sync/*`: Long poll queues pulling synchronization tasks (like SMS code dispatches).
- `/api/v1/gallery/*`: Upserting metadata caches.
- `/api/v1/location/*`: Coordinating coordinates refreshes between website and APK.
- `/api/v1/ai/*`: Keywords matching RAG index pipeline communicating with Gemini.

Refer to the global **[`/documentation/API.md`](file:///C:/Users/yasao/.gemini/antigravity/scratch/ReY-Platform/documentation/API.md)** file for full parameters specification.
