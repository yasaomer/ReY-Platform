# ReY Platform Installation Guide

This document describes how to install the three major components of the ReY Platform.

## 1. Android Admin Application (Private OML)

### Requirements
- Android device running Android 7.0 (API 24) or higher.
- Cellular connection with active SIM (required for sending SMS codes).

### Steps
1. Navigate to the built APK directory: `/android/app/build/outputs/apk/debug/`.
2. Transfer the `app-debug.apk` file to your Android phone.
3. Tap the file on your device and click **Install**.
4. Allow unknown source installations if prompted.

---

## 2. Serverless API Coordinator (Backend Worker)

### Requirements
- Cloudflare free account.
- Node.js environment.

### Steps
1. Navigate to `/backend`.
2. Log in to Cloudflare using the CLI:
   ```bash
   npx wrangler login
   ```
3. Initialize the D1 database:
   ```bash
   npx wrangler d1 execute rey_db --file=db/schema.sql
   ```
4. Deploy the Worker script:
   ```bash
   npm run deploy
   ```

---

## 3. Viewer Platform (Website)

### Requirements
- Built static assets from `/website/dist`.

### Steps
1. Go to the Cloudflare dashboard -> **Workers & Pages**.
2. Create a new **Pages** application.
3. Select **Upload assets** and upload the contents of the `/website/dist` folder.
4. Deploy.
