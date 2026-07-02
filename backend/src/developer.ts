import { Hono } from "hono";
import { Env } from "./types";
import { createResponse } from "./response";

const developerRouter = new Hono<{ Bindings: Env }>();

// 1. SYSTEM METRICS AND DATABASE STATS
developerRouter.get("/stats", async (c) => {
  try {
    const totalUsers = await c.env.REY_DB.prepare("SELECT count(*) as cnt FROM auth_users").first() as any;
    const totalSessions = await c.env.REY_DB.prepare("SELECT count(*) as cnt FROM sessions").first() as any;
    const totalImages = await c.env.REY_DB.prepare("SELECT count(*) as cnt FROM gallery_metadata").first() as any;
    const totalDocs = await c.env.REY_DB.prepare("SELECT count(*) as cnt FROM knowledge_documents").first() as any;
    const totalLogs = await c.env.REY_DB.prepare("SELECT count(*) as cnt FROM system_logs").first() as any;
    const pendingTasks = await c.env.REY_DB.prepare("SELECT count(*) as cnt FROM sync_queue WHERE status = 'pending'").first() as any;
    
    // Average API response latency from AI stats (as proxy)
    const avgAiLatency = await c.env.REY_DB.prepare("SELECT avg(latency_ms) as avg_lat FROM ai_statistics").first() as any;

    const stats = {
      users: totalUsers ? totalUsers.cnt : 0,
      sessions: totalSessions ? totalSessions.cnt : 0,
      images_tracked: totalImages ? totalImages.cnt : 0,
      docs_indexed: totalDocs ? totalDocs.cnt : 0,
      log_entries: totalLogs ? totalLogs.cnt : 0,
      sync_queue_size: pendingTasks ? pendingTasks.cnt : 0,
      avg_ai_latency_ms: avgAiLatency && avgAiLatency.avg_lat ? Math.round(avgAiLatency.avg_lat) : 0
    };

    return createResponse(c, true, 200, "Developer stats compiled", stats);
  } catch (err: any) {
    return createResponse(c, false, 500, "Failed to compile developer stats", { errors: [err.message] });
  }
});

// 2. RETRIEVE SYSTEM CONFIGURATION SUMMARY
developerRouter.get("/config", async (c) => {
  try {
    const rows = await c.env.REY_DB.prepare(
      "SELECT config_key, length(config_value) as val_len FROM configuration"
    ).all();
    return createResponse(c, true, 200, "Configuration directory loaded", rows.results);
  } catch (err: any) {
    return createResponse(c, false, 500, "Failed to load config directory", { errors: [err.message] });
  }
});

// 3. AUTO-GENERATED API DOCUMENTATION
developerRouter.get("/docs", (c) => {
  const documentation = {
    api_title: "ReY Platform Ecosystem API Specification",
    version: "v1",
    base_url: "/api/v1",
    authentication: "Bearer Token required in Authorization header for protected paths.",
    endpoints: [
      {
        path: "/auth/login",
        method: "POST",
        description: "Authenticate viewer profile and retrieve bearer session token",
        request_body: { username: "string", password: "sadf" },
        response: { success: "boolean", data: { token: "jwt-token-string" } }
      },
      {
        path: "/gallery/list",
        method: "GET",
        description: "List tracked images metadata from SQLite gallery_metadata",
        response: { success: "boolean", data: [{ image_id: "uuid", file_name: "image.png" }] }
      },
      {
        path: "/location/current",
        method: "GET",
        description: "Get current location of owner device",
        response: { success: "boolean", data: { latitude: 45.46, longitude: 9.19 } }
      },
      {
        path: "/location/refresh",
        method: "POST",
        description: "Queue refresh location task to APK worker",
        response: { success: "boolean", data: { taskId: "task-uuid" } }
      },
      {
        path: "/sync/pull",
        method: "GET",
        description: "Fetch pending queue tasks for APK sync worker loops",
        response: { success: "boolean", data: [{ task_id: "t-001", task_type: "refresh_location" }] }
      },
      {
        path: "/sync/complete",
        method: "POST",
        description: "Record sync task completion result to database",
        request_body: { taskId: "t-001", status: "completed" },
        response: { success: "boolean", message: "Task recorded" }
      },
      {
        path: "/health",
        method: "GET",
        description: "Detailed ecosystem status checks",
        response: { success: "boolean", data: { status: "healthy", services: {} } }
      }
    ],
    error_codes: {
      "401": "Unauthorized. Token invalid or expired.",
      "404": "Endpoint or resource not found.",
      "500": "Internal server error. Database or worker runtime failure.",
      "503": "Service unavailable. Critical backing store offline."
    }
  };

  return createResponse(c, true, 200, "API Documentation generated successfully", documentation);
});

export default developerRouter;
