# ReY Platform Ecosystem

A complete, premium, private content archive and intelligent assistant communication system. It is designed to act as a personal cloud vault with zero operational server costs by leveraging serverless Cloudflare Workers, Google Drive object storage, and Gemini API capabilities.

## Repository Organization

This workspace is structured as follows:

- **[`/android`](file:///C:/Users/yasao/.gemini/antigravity/scratch/ReY-Platform/android)**: Native Jetpack Compose administrator application (**Private OML**) used only by the owner to manage settings, status messages, GPS coordinates, and gallery folder monitoring.
- **[`/website`](file:///C:/Users/yasao/.gemini/antigravity/scratch/ReY-Platform/website)**: Premium Vite + React + TypeScript viewer website (**ReY**) with glassmorphism layout, interactive OpenStreetMap mapping, lazy loaded gallery, and chatbot console.
- **[`/backend`](file:///C:/Users/yasao/.gemini/antigravity/scratch/ReY-Platform/backend)**: Serverless API Gateway and synchronization hub (**Cloudflare Worker**), using Cloudflare D1 SQL database and KV cache.
- **[`/documentation`](file:///C:/Users/yasao/.gemini/antigravity/scratch/ReY-Platform/documentation)**: Exhaustive manuals and references covering architecture, API endpoints, deployment, developer code guide, and user instructions.
- **[`/deployment`](file:///C:/Users/yasao/.gemini/antigravity/scratch/ReY-Platform/deployment)**: Shell scripts to run automation setups, builds and migrations.

## Project Vision & Philosophy

- **Personal & Premium**: Sleek responsive design styled purely with custom CSS tokens, avoiding generic templates or loud neon/cyberpunk aesthetics.
- **Privacy & Trust**: Direct connection proxy through the owner's Google Drive. The viewer can never edit the owner's data.
- **Event-Driven Sync**: The Android app acts as the master, automatically publishing telemetry location, status changes, and image additions to the backend SQL registry.
- **AI Agent Chat**: Integrated RAG (Retrieval-Augmented Generation) query bot using Gemini to answer viewer questions based on files in the owner's documents collection.
