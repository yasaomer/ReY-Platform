# ReY Platform Architecture Overview

The ReY Platform is a secure, personal content sharing and intelligent companion ecosystem. It is divided into three core logical components: the Administrator (Android App), the Viewer (Website), and the Coordinator (Backend / API Gateway).

## System Component Diagram

```
                        +----------------------------+
                        |     ReY Private Website    |
                        | (Authorized Viewer Client) |
                        +--------------+-------------+
                                       |
                                       | HTTPS requests (JSON / Cursors)
                                       v
                        +----------------------------+
                        |  Cloudflare Worker Backend |
                        |     (API Gateway & Sync)   |
                        +--------------+-------------+
                                       |
                        +--------------+--------------+
                        |                             |
                        v                             v
           +-------------------------+   +-------------------------+
           |     Cloudflare D1/KV    |   |    Google Drive REST    |
           |   (Lightweight Metadata) |   |  (Object Media Storage) |
           +-------------------------+   +------------+------------+
                                                      ^
                                                      | Sync Uploads / Downloads
                                                      |
                                         +------------+------------+
                                         |    Private OML Android  |
                                         |    (Administrator App)  |
                                         +-------------------------+
```

## Core Systems & Flow Processes

### 1. Synchronization Flow (Event-Driven)
The administrator app monitors the configured local directory. When files are added, modified, renamed, or deleted:
1. The app updates its local SQLite cache.
2. The app uploads the image file to the linked Google Drive account.
3. The app notifies the backend API `/api/v1/gallery/metadata` with coordinates, file hash, and new name.
4. The backend stores this in Cloudflare D1.
5. The viewer website loads the changes instantly by querying `/api/v1/gallery/list` and loading binary media direct from the Google Drive proxy.

### 2. Password Recovery (OTP-SIM Channel)
When a viewer initiates a password reset:
1. The website requests a code from `/api/v1/auth/forgot-password`.
2. The backend generates a secure 6-digit OTP, saves the hash in KV with a 10-minute expiry, and appends a `send_sms_reset` task to the `sync_queue` database.
3. The Android app's background worker pulls the task from `/api/v1/sync/pull`.
4. The app sends the OTP via the device's SIM card to the recovery number.
5. The viewer receives the SMS code, types it on the website, which verifies it and unlocks password changes.

### 3. Location Request Mapping
1. Viewer clicks "Refresh Location" on the website location screen.
2. The website issues a request to `/api/v1/location/refresh`.
3. The backend appends a `refresh_location` sync task.
4. The Android background worker polls the sync task, queries the GPS provider, and reports the coordinate payload to `/api/v1/location/update`.
5. The website map component refreshes leaf layers dynamically.
