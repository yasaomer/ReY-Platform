import { Hono } from "hono";
import { Env } from "./types";
import { createResponse } from "./response";
import { logEvent } from "./logs";

const locationRouter = new Hono<{ Bindings: Env }>();

// 1. GET CURRENT LOCATION (for Website)
locationRouter.get("/current", async (c) => {
  try {
    const loc = await c.env.REY_DB.prepare(
      "SELECT * FROM location_status ORDER BY timestamp DESC LIMIT 1"
    ).first() as any;

    if (!loc) {
      return createResponse(c, false, 404, "No location data available");
    }

    return createResponse(c, true, 200, "Current location loaded", loc);
  } catch (err: any) {
    return createResponse(c, false, 500, "Failed to load location", { errors: [err.message] });
  }
});

// 2. UPDATE LOCATION (called by APK)
locationRouter.post("/update", async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const { latitude, longitude, accuracy, altitude, speed, heading, address, provider, timestamp } = body;

  if (latitude === undefined || longitude === undefined || accuracy === undefined) {
    return createResponse(c, false, 400, "Latitude, Longitude and Accuracy are required");
  }

  try {
    const ts = timestamp || new Date().toISOString();
    await c.env.REY_DB.prepare(
      `INSERT INTO location_status (latitude, longitude, accuracy, altitude, speed, heading, address, provider, timestamp)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`
    ).bind(
      latitude, longitude, accuracy, altitude || null, speed || null, heading || null, address || null, provider || 'gps', ts
    ).run();

    await logEvent(c.env, "location", "INFO", "location_update", `Location updated: ${latitude}, ${longitude}`);
    return createResponse(c, true, 200, "Location recorded successfully");
  } catch (err: any) {
    return createResponse(c, false, 500, "Failed to update location", { errors: [err.message] });
  }
});

// 3. REQUEST REFRESH (called by Website)
locationRouter.post("/refresh", async (c) => {
  try {
    // Generate a sync task for the APK to query GPS
    const taskId = crypto.randomUUID();
    await c.env.REY_DB.prepare(
      `INSERT INTO sync_queue (task_id, task_type, priority, status, target_module, error_info)
       VALUES (?, 'refresh_location', 1, 'pending', 'location', '')`
    ).bind(taskId).run();

    await logEvent(c.env, "location", "INFO", "refresh_request", "Location refresh request queued");

    return createResponse(c, true, 200, "Refresh requested. Waiting for APK response...", { taskId });
  } catch (err: any) {
    return createResponse(c, false, 500, "Failed to queue refresh request", { errors: [err.message] });
  }
});

export default locationRouter;
