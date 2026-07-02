# ReY Platform API Reference Specification

All endpoints are prefix versioned as `/api/v1/*`. Response objects conform to the standardized JSON envelope.

## Standard JSON Envelope

```json
{
  "success": true,
  "timestamp": "2026-07-02T14:42:27Z",
  "requestId": "uuid-v4-format",
  "statusCode": 200,
  "message": "Status description text",
  "data": {},
  "metadata": null,
  "errors": []
}
```

---

## 1. Authentication Endpoints

### POST `/auth/login`
Logs user into the system and returns authorization bearer tokens.
- **Payload**:
  ```json
  { "username": "Rozuly", "password": "..." }
  ```
- **Returns**: `token`, `expiresAt`, `username`.

### POST `/auth/logout`
Protected. Invalidates current session cache.
- **Headers**: `Authorization: Bearer <token>`

### GET `/auth/validate`
Protected. Verifies if session token is still valid.

### POST `/auth/forgot-password`
Generates reset codes and queues SMS notification task.
- **Payload**:
  ```json
  { "username": "Rozuly" }
  ```

### POST `/auth/verify-recovery-code`
Verifies OTP code and yields 5-minute transient password reset tokens.
- **Payload**:
  ```json
  { "username": "Rozuly", "code": "123456" }
  ```

### POST `/auth/reset-password`
Overwrites current password and revokes all active sessions.
- **Payload**:
  ```json
  { "username": "Rozuly", "resetToken": "...", "newPassword": "..." }
  ```

---

## 2. Gallery Endpoints

### GET `/gallery/list`
Protected. Returns active, approved gallery elements from database.

### POST `/gallery/metadata`
Protected. Called by APK to upsert cache indices.
- **Payload**: Image ID, hash, file details, visibility status.

### POST `/gallery/log-view`
Protected. Increments view counters and durations logs.
- **Payload**:
  ```json
  { "image_id": "...", "event_type": "view", "duration_seconds": 12.4 }
  ```

---

## 3. Location Endpoints

### GET `/location/current`
Protected. Returns latest GPS coordinates.

### POST `/location/update`
Protected. Called by APK to post fresh location telemetry logs.
- **Payload**: lat, lng, altitude, accuracy, speed, heading.

### POST `/location/refresh`
Protected. Called by Website to append location query task to sync queue.

---

## 4. AI & RAG Endpoints

### POST `/ai/ask`
Protected. RAG pipeline query entry. Searches D1 document matches, builds systems prompt wrapper, queries Gemini.
- **Payload**:
  ```json
  { "message": "What is the owner's goal?", "conversationHistory": [] }
  ```

### POST `/ai/config`
Protected. Updates provider, model details, or system prompts parameters.

---

## 5. Sync Endpoints

### GET `/sync/pull`
Protected. Polled by APK to retrieve pending commands list.

### POST `/sync/complete`
Protected. Updates sync tasks statuses to completed or failed.
- **Payload**:
  ```json
  { "taskId": "...", "status": "completed", "errorInfo": "" }
  ```
