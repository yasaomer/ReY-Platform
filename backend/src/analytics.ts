import { Hono } from "hono";
import { Env } from "./types";
import { createResponse } from "./response";
import { logEvent } from "./logs";

const analyticsRouter = new Hono<{ Bindings: Env }>();

// 0. DASHBOARD SUMMARY (called by Website DashboardPage)
analyticsRouter.get("/dashboard", async (c) => {
  try {
    // Sync stats
    const lastSync = await c.env.REY_DB.prepare(
      "SELECT timestamp, status FROM sync_history ORDER BY timestamp DESC LIMIT 1"
    ).first() as any;

    const syncErrors = await c.env.REY_DB.prepare(
      "SELECT COUNT(*) as cnt FROM sync_history WHERE status = 'failed'"
    ).first() as any;

    // Gallery stats
    const galleryViews = await c.env.REY_DB.prepare(
      "SELECT COALESCE(SUM(view_count), 0) as total FROM gallery_metadata"
    ).first() as any;

    const topImage = await c.env.REY_DB.prepare(
      "SELECT file_name, view_count FROM gallery_metadata ORDER BY view_count DESC LIMIT 1"
    ).first() as any;

    // AI stats
    const aiSuccess = await c.env.REY_DB.prepare(
      "SELECT COUNT(*) as cnt FROM ai_statistics WHERE status = 'success'"
    ).first() as any;

    const aiFailed = await c.env.REY_DB.prepare(
      "SELECT COUNT(*) as cnt FROM ai_statistics WHERE status = 'failed'"
    ).first() as any;

    const aiLatency = await c.env.REY_DB.prepare(
      "SELECT COALESCE(AVG(latency_ms), 0) as avg FROM ai_statistics WHERE status = 'success'"
    ).first() as any;

    // Knowledge base
    const kbDocs = await c.env.REY_DB.prepare(
      "SELECT COUNT(*) as cnt FROM knowledge_documents"
    ).first() as any;

    // System errors (recent logs)
    const sysErrors = await c.env.REY_DB.prepare(
      "SELECT COUNT(*) as cnt FROM system_logs WHERE severity = 'ERROR' AND datetime(timestamp) >= datetime('now', '-24 hours')"
    ).first() as any;

    return createResponse(c, true, 200, "Dashboard data loaded", {
      sync: {
        lastSyncTime: lastSync ? lastSync.timestamp : null,
        lastSyncStatus: lastSync ? lastSync.status : "idle",
        totalErrors: syncErrors ? syncErrors.cnt : 0
      },
      gallery: {
        totalViews: galleryViews ? galleryViews.total : 0,
        mostViewedImage: topImage ? topImage.file_name : "None",
        mostViewedViews: topImage ? topImage.view_count : 0
      },
      ai: {
        successfulRequests: aiSuccess ? aiSuccess.cnt : 0,
        failedRequests: aiFailed ? aiFailed.cnt : 0,
        averageLatencyMs: aiLatency ? Math.round(aiLatency.avg) : 0
      },
      knowledgeBase: {
        totalDocuments: kbDocs ? kbDocs.cnt : 0
      },
      system: {
        activeErrorsCount: sysErrors ? sysErrors.cnt : 0
      }
    });
  } catch (err: any) {
    return createResponse(c, false, 500, "Failed to load dashboard data", { errors: [err.message] });
  }
});

// 1. REPORT A VISITOR EVENT (from Website)
analyticsRouter.post("/event", async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const { session_id, event_type, page_name, target_item, duration_seconds } = body;

  if (!session_id || !event_type) {
    return createResponse(c, false, 400, "session_id and event_type are required");
  }

  // Parse headers for platform details
  const userAgent = c.req.header("User-Agent") || "";
  const language = c.req.header("Accept-Language") || "en";
  const ipAddress = c.req.header("CF-Connecting-IP") || "127.0.0.1";

  // Simple parser helpers
  let platform = "Other";
  if (/windows/i.test(userAgent)) platform = "Windows";
  else if (/macintosh|mac os/i.test(userAgent)) platform = "macOS";
  else if (/android/i.test(userAgent)) platform = "Android";
  else if (/iphone|ipad/i.test(userAgent)) platform = "iOS";

  let browser = "Other";
  if (/chrome|crios/i.test(userAgent)) browser = "Chrome";
  else if (/firefox|fxios/i.test(userAgent)) browser = "Firefox";
  else if (/safari/i.test(userAgent) && !/chrome/i.test(userAgent)) browser = "Safari";
  else if (/msie|trident/i.test(userAgent)) browser = "IE";

  try {
    const timestamp = new Date().toISOString();
    await c.env.REY_DB.prepare(
      `INSERT INTO visitor_events (session_id, user_agent, platform, browser, language, ip_address, event_type, page_name, target_item, timestamp, duration_seconds)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
    ).bind(session_id, userAgent, platform, browser, language, ipAddress, event_type, page_name || null, target_item || null, timestamp, duration_seconds || null).run();

    return createResponse(c, true, 200, "Event recorded successfully");
  } catch (err: any) {
    return createResponse(c, false, 500, "Failed to record visitor event", { errors: [err.message] });
  }
});

// 2. GET ADVANCED SYSTEM ANALYTICS SUMMARY
analyticsRouter.get("/stats", async (c) => {
  try {
    const now = new Date();
    const todayStr = now.toISOString().substring(0, 10) + "%";

    // A. Visitor counts
    const totalVisits = await c.env.REY_DB.prepare(
      "SELECT count(distinct session_id) as cnt FROM visitor_events"
    ).first() as any;

    const todayVisits = await c.env.REY_DB.prepare(
      "SELECT count(distinct session_id) as cnt FROM visitor_events WHERE timestamp LIKE ?"
    ).bind(todayStr).first() as any;

    const activeOnline = await c.env.REY_DB.prepare(
      "SELECT count(distinct session_id) as cnt FROM visitor_events WHERE datetime(timestamp) >= datetime('now', '-5 minutes')"
    ).first() as any;

    // B. Platform distributions
    const platforms = await c.env.REY_DB.prepare(
      "SELECT platform, count(distinct session_id) as count FROM visitor_events GROUP BY platform ORDER BY count DESC"
    ).all();

    const browsers = await c.env.REY_DB.prepare(
      "SELECT browser, count(distinct session_id) as count FROM visitor_events GROUP BY browser ORDER BY count DESC"
    ).all();

    // C. Page activity views count
    const pageViews = await c.env.REY_DB.prepare(
      "SELECT page_name, count(*) as count FROM visitor_events WHERE event_type = 'page_view' GROUP BY page_name ORDER BY count DESC"
    ).all();

    // D. Response latency statistics
    const avgLatencyRow = await c.env.REY_DB.prepare(
      "SELECT avg(duration_seconds) as avg_sec FROM visitor_events WHERE event_type = 'page_view'"
    ).first() as any;

    const stats = {
      summary: {
        total_visitors: totalVisits ? totalVisits.cnt : 0,
        today_visitors: todayVisits ? todayVisits.cnt : 0,
        online_now: activeOnline ? activeOnline.cnt : 0,
        average_session_seconds: avgLatencyRow && avgLatencyRow.avg_sec ? Math.round(avgLatencyRow.avg_sec) : 45
      },
      distributions: {
        platforms: platforms.results,
        browsers: browsers.results
      },
      page_views: pageViews.results
    };

    return createResponse(c, true, 200, "Analytics stats compiled", stats);
  } catch (err: any) {
    return createResponse(c, false, 500, "Failed to load analytics statistics", { errors: [err.message] });
  }
});

// 3. FETCH LIVE EVENT logs (with filters & search)
analyticsRouter.get("/events", async (c) => {
  const page = c.req.query("page_name") || "";
  const event = c.req.query("event_type") || "";

  try {
    let query = "SELECT * FROM visitor_events";
    const bindings: string[] = [];

    if (page && event) {
      query += " WHERE page_name = ? AND event_type = ?";
      bindings.push(page, event);
    } else if (page) {
      query += " WHERE page_name = ?";
      bindings.push(page);
    } else if (event) {
      query += " WHERE event_type = ?";
      bindings.push(event);
    }

    query += " ORDER BY timestamp DESC LIMIT 100";

    const stmt = c.env.REY_DB.prepare(query);
    const rows = bindings.length > 0 ? await stmt.bind(...bindings).all() : await stmt.all();

    return createResponse(c, true, 200, "Visitor logs loaded", rows.results);
  } catch (err: any) {
    return createResponse(c, false, 500, "Failed to retrieve visitor events list", { errors: [err.message] });
  }
});

// 4. REPORT GENERATOR
analyticsRouter.get("/report", async (c) => {
  const type = c.req.query("type") || "daily";

  try {
    const totalViews = await c.env.REY_DB.prepare("SELECT count(*) as cnt FROM visitor_events").first() as any;
    const errorsCount = await c.env.REY_DB.prepare("SELECT count(*) as cnt FROM visitor_events WHERE event_type = 'error'").first() as any;
    const loginsCount = await c.env.REY_DB.prepare("SELECT count(*) as cnt FROM visitor_events WHERE event_type = 'login'").first() as any;

    const report = {
      report_type: type,
      timestamp: new Date().toISOString(),
      metrics: {
        total_requests: totalViews ? totalViews.cnt : 0,
        failed_requests: errorsCount ? errorsCount.cnt : 0,
        visitor_logins: loginsCount ? loginsCount.cnt : 0,
        sync_status: "operational"
      }
    };

    return createResponse(c, true, 200, `${type} report generated successfully`, report);
  } catch (err: any) {
    return createResponse(c, false, 500, "Failed to compile report", { errors: [err.message] });
  }
});

export default analyticsRouter;
