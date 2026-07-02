import { Hono } from "hono";
import { Env } from "./types";
import { createResponse } from "./response";

const healthRouter = new Hono<{ Bindings: Env }>();

// 1. ADVANCED HEALTH CHECK ENDPOINT
healthRouter.get("/", async (c) => {
  const healthStatus: any = {
    status: "healthy",
    timestamp: new Date().toISOString(),
    services: {}
  };

  let overallHealthy = true;

  // Check D1 Database
  try {
    const start = Date.now();
    await c.env.REY_DB.prepare("SELECT 1").first();
    const duration = Date.now() - start;
    healthStatus.services.database = {
      status: "connected",
      latency_ms: duration
    };
  } catch (err: any) {
    overallHealthy = false;
    healthStatus.services.database = {
      status: "disconnected",
      error: err.message
    };
  }

  // Check KV Namespace
  try {
    const start = Date.now();
    await c.env.REY_KV.put("health_check_ping", "pong", { expirationTtl: 60 });
    const val = await c.env.REY_KV.get("health_check_ping");
    const duration = Date.now() - start;
    healthStatus.services.kv = {
      status: val === "pong" ? "connected" : "degraded",
      latency_ms: duration
    };
  } catch (err: any) {
    overallHealthy = false;
    healthStatus.services.kv = {
      status: "disconnected",
      error: err.message
    };
  }

  // Check Synchronization Queue
  try {
    const pendingCount = await c.env.REY_DB.prepare(
      "SELECT count(*) as cnt FROM sync_queue WHERE status = 'pending'"
    ).first() as any;
    healthStatus.services.synchronization = {
      status: "active",
      pending_tasks: pendingCount ? pendingCount.cnt : 0
    };
  } catch (err: any) {
    healthStatus.services.synchronization = {
      status: "error",
      error: err.message
    };
  }

  // Check AI Provider config
  try {
    const aiConfig = await c.env.REY_DB.prepare(
      "SELECT count(*) as cnt FROM ai_providers WHERE api_key IS NOT NULL AND api_key != ''"
    ).first() as any;
    healthStatus.services.ai_provider = {
      status: "configured",
      configured_providers: aiConfig ? aiConfig.cnt : 0
    };
  } catch (err: any) {
    healthStatus.services.ai_provider = {
      status: "error",
      error: err.message
    };
  }

  // Cloudflare V8 environment stats
  healthStatus.environment = {
    platform: "Cloudflare Workers",
    runtime: "V8 Isolate",
    compatibility_date: c.env.API_VERSION ? "2024-02-22" : "unknown"
  };

  if (!overallHealthy) {
    healthStatus.status = "degraded";
    return createResponse(c, false, 503, "One or more critical services are offline", healthStatus);
  }

  return createResponse(c, true, 200, "All services healthy", healthStatus);
});

export default healthRouter;
