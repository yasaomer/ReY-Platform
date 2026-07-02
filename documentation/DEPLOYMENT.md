# ReY Platform Deployment Guide

This guide describes how to deploy the serverless backend worker and the static website to Cloudflare's free tiers.

## Prerequisites

1. Install Node.js v20+ and npm.
2. Sign up for a free [Cloudflare Account](https://dash.cloudflare.com/).
3. Install the Cloudflare Wrangler CLI globally (optional, package devDependencies include it):
   ```bash
   npm install -g wrangler
   ```

---

## 1. Backend Deployment (Cloudflare Workers)

### Step A: Authenticate Wrangler CLI
Log in to your Cloudflare account from your command line:
```bash
npx wrangler login
```

### Step B: Create D1 Database & KV Namespace
Create the D1 database:
```bash
npx wrangler d1 create rey_db
```
*Note down the Database ID returned in the output.*

Create the KV Namespace:
```bash
npx wrangler kv:namespace create REY_KV
```
*Note down the KV ID returned.*

### Step C: Configure wrangler.toml
Open `backend/wrangler.toml` and update the bindings with your actual IDs:
```toml
[[kv_namespaces]]
binding = "REY_KV"
id = "YOUR_KV_ID"

[[d1_databases]]
binding = "REY_DB"
database_name = "rey_db"
database_id = "YOUR_D1_DATABASE_ID"
```

### Step D: Initialize D1 Tables
Bootstrap the SQLite database using the schema schema:
```bash
npx wrangler d1 execute rey_db --file=db/schema.sql
```

### Step E: Configure Secrets (Gemini API Key)
Store your secret Gemini API key securely:
```bash
npx wrangler secret put GEMINI_API_KEY
```
*Enter your Gemini API key when prompted.*

### Step F: Deploy Worker
Compile and publish the worker serverless function:
```bash
npm run deploy
```
*Take note of the final Worker URL (e.g. `https://rey-backend.yoursubdomain.workers.dev`).*

---

## 2. Website Deployment (Cloudflare Pages)

The website is a static single-page React app that connects to the worker API.

### Step A: Configure API Endpoint
Update the fetch host urls in the website pages from `http://localhost:8787/api/v1` to your actual deployed backend URL (e.g., `https://rey-backend.yoursubdomain.workers.dev/api/v1`).

### Step B: Build project assets
Run compilation locally to verify bundles build successfully:
```bash
npm run build
```

### Step C: Deploy to Pages
You can upload the static `/dist` output folder to Cloudflare Pages:
1. Go to Cloudflare Dashboard -> **Workers & Pages** -> **Create Application**.
2. Select **Pages** tab and click **Upload assets**.
3. Choose a project name, upload the `website/dist` folder, and deploy.
