# ReY Platform Developer Guide

This document describes the code base architecture, modular organization, and guidelines for extending the ReY Platform.

## 1. Project Folder Tree

```
ReY-Platform/
├── android/            # Android administrator application (Kotlin / Compose)
│   ├── app/src/main/
│   │   ├── AndroidManifest.xml # Permissions, background services declaration
│   │   └── java/com/example/privateoml/
│   │       ├── MainActivity.kt  # Root entry, biometric locks
│   │       ├── Navigation.kt    # Compose navigation flow
│   │       ├── data/            # Local SQLite database caching and repos
│   │       ├── services/        # Location and task sync background services
│   │       └── ui/              # Setup, Lock, Dashboard, Editor Compose screens
├── website/            # Reader website (React / TypeScript / Vite)
│   ├── src/
│   │   ├── main.tsx          # Router routing gates
│   │   ├── index.css         # Glassmorphism dark mode system design CSS
│   │   ├── layouts/          # Responsive AppLayout wraps
│   │   └── pages/            # Login, Dashboard, Gallery, Location, Chat pages
└── backend/            # API Gateway sync server (Cloudflare Worker)
    ├── db/schema.sql   # Normalized D1 SQLite tables schema
    └── src/
        ├── index.ts    # Main app routers wrapper and gate middlewares
        ├── auth.ts     # Credentials, session tokens and OTP handlers
        ├── sync.ts     # Pull/Complete queues for Android workers
        ├── gallery.ts  # Caching metadata lists and logs view counts
        ├── location.ts # Telometry registers and APK queries triggers
        └── ai.ts       # RAG context selection and Gemini prompts compilations
```

---

## 2. Local Configuration Registry (Android App)

The app saves telemetry state in local key-value parameters inside its SQLite configuration table:
- `is_setup_completed` (`true` / `false`): Redirects on startup.
- `app_password_hash` (SHA-256 Hex): Validates LockScreen passcodes.
- `server_url` (String URL): Backend API Gateway path.
- `session_token` (String Token): Bearer authentication header.
- `gallery_folder_path` (String Path): Directory containing images to monitor.
- `gemini_api_key` (String Key): Local backup AI configuration.
- `recovery_phone_number` (String Phone): SMS recipient for resets.
- `last_sync_time` (ISO Date): Timestamp tracker.

---

## 3. Extending AI Providers (RAG Adapter Pattern)

To add another provider (e.g., DeepSeek, OpenAI) in the backend RAG pipeline:
1. Update `backend/src/ai.ts` inside the `/ask` route.
2. Define a provider call helper (e.g. `callDeepSeek` using `fetch` against the specific API).
3. Bind the request headers to the relevant environment key (e.g. `c.env.DEEPSEEK_API_KEY`).
4. Read provider variables from `ai_providers` SQL configuration.
