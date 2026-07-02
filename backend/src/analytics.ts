import { Hono } from "hono";
import { Env } from "./types";
import { createResponse } from "./response";

const analyticsRouter = new Hono<{ Bindings: Env }>();

// 1. GET AGGREGATED DASHBOARD STATISTICS (for APK & Website)
analyticsRouter.get("/dashboard", async (c) => {
  try {
    // A. Sync stats
    const lastSync = await c.env.REY_DB.prepare(
      "SELECT completion_time, status FROM sync_history ORDER BY timestamp DESC LIMIT 1"
    ).first() as any;

    const syncErrors = await c.env.REY_DB.prepare(
      "SELECT COUNT(*) as count FROM sync_history WHERE status = 'failed'"
    ).first() as any;

    // B. Gallery stats
    const totalViews = await c.env.REY_DB.prepare(
      "SELECT SUM(view_count) as total FROM gallery_metadata"
    ).first() as any;

    const mostViewed = await c.env.REY_DB.prepare(
      "SELECT file_name, view_count FROM gallery_metadata WHERE view_count > 0 ORDER BY view_count DESC LIMIT 1"
    ).first() as any;

    // C. AI stats
    const aiSuccess = await c.env.REY_DB.prepare(
      "SELECT COUNT(*) as count FROM ai_statistics WHERE status = 'success'"
    ).first() as any;

    const aiFail = await c.env.REY_DB.prepare(
      "SELECT COUNT(*) as count FROM ai_statistics WHERE status = 'failed'"
    ).first() as any;

    const aiAvgLatency = await c.env.REY_DB.prepare(
      "SELECT AVG(latency_ms) as avg FROM ai_statistics"
    ).first() as any;

    // D. Log counts
    const errorLogs = await c.env.REY_DB.prepare(
      "SELECT COUNT(*) as count FROM system_logs WHERE severity = 'ERROR'"
    ).first() as any;

    // E. Document indexing count
    const kbDocs = await c.env.REY_DB.prepare(
      "SELECT COUNT(*) as count FROM knowledge_documents"
    ).first() as any;

    return createResponse(c, true, 200, "Dashboard analytics fetched", {
      sync: {
        lastSyncTime: lastSync?.completion_time || null,
        lastSyncStatus: lastSync?.status || "never",
        totalErrors: syncErrors?.count || 0
      },
      gallery: {
        totalViews: totalViews?.total || 0,
        mostViewedImage: mostViewed?.file_name || "none",
        mostViewedViews: mostViewed?.view_count || 0
      },
      ai: {
        successfulRequests: aiSuccess?.count || 0,
        failedRequests: aiFail?.count || 0,
        averageLatencyMs: Math.round(aiAvgLatency?.avg || 0)
      },
      knowledgeBase: {
        totalDocuments: kbDocs?.count || 0
      },
      system: {
        activeErrorsCount: errorLogs?.count || 0
      }
    });
  } catch (err: any) {
    return createResponse(c, false, 500, "Failed to load analytics dashboard", { errors: [err.message] });
  }
});

// 2. REPORT EVENT
analyticsRouter.post("/report", async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const { category, message } = body;

  if (!category || !message) {
    return createResponse(c, false, 400, "Category and message are required");
  }

  try {
    const timestamp = new Date().toISOString();
    await c.env.REY_DB.prepare(
      "INSERT INTO notifications (category, message, timestamp, is_read) VALUES (?, ?, ?, 0)"
    ).bind(category, message, timestamp).run();

    return createResponse(c, true, 200, "Event reported successfully");
  } catch (err: any) {
    return createResponse(c, false, 500, "Failed to record event report", { errors: [err.message] });
  }
});

export default analyticsRouter;
