import { Hono } from "hono";
import { cors } from "hono/cors";
import { Env } from "./types";
import { createResponse } from "./response";
import authRouter from "./auth";
import galleryRouter from "./gallery";
import locationRouter from "./location";
import aiRouter from "./ai";
import syncRouter from "./sync";
import analyticsRouter from "./analytics";
import { logEvent } from "./logs";

const app = new Hono<{ Bindings: Env }>();

// 1. GLOBAL MIDDLEWARES
app.use("*", cors({
  origin: "*", // Adjust to specific website domain when deploying (e.g. cloudflare pages/github pages url)
  allowHeaders: ["Content-Type", "Authorization"],
  allowMethods: ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
  maxAge: 86400,
}));

// Timing middleware
app.use("*", async (c, next) => {
  const start = Date.now();
  await next();
  const ms = Date.now() - start;
  c.header("X-Response-Time", `${ms}ms`);
});

// 2. HEALTH CHECK API
app.get("/api/v1/health", (c) => {
  return createResponse(c, true, 200, "ReY Platform Backend is online", {
    status: "healthy",
    environment: "production",
    database: "connected"
  });
});

// 3. MOUNT UNPROTECTED ROUTERS
app.route("/api/v1/auth", authRouter);

// 4. AUTHENTICATION SHIELD MIDDLEWARE FOR PROTECTED API PATHS
app.use("/api/v1/*", async (c, next) => {
  const path = c.req.path;
  
  // Skip auth checks on unprotected routes
  if (
    path.endsWith("/auth/login") ||
    path.endsWith("/auth/forgot-password") ||
    path.endsWith("/auth/verify-recovery-code") ||
    path.endsWith("/auth/reset-password") ||
    path.endsWith("/health")
  ) {
    return next();
  }

  const authHeader = c.req.header("Authorization");
  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    return createResponse(c, false, 401, "Authentication required. Bearer Token missing.");
  }
  const token = authHeader.substring(7);

  // Check session cache in KV
  const kvSession = await c.env.REY_KV.get(`session:${token}`);
  if (kvSession) {
    const sessionData = JSON.parse(kvSession);
    c.set("jwtPayload" as any, sessionData);
    return next();
  }

  // Fallback to D1 database check
  try {
    const session = await c.env.REY_DB.prepare(
      "SELECT s.*, u.username FROM sessions s JOIN auth_users u ON s.user_id = u.id WHERE s.id = ? AND s.is_valid = 1 AND s.expires_at > ?"
    ).bind(token, new Date().toISOString()).first() as any;

    if (!session) {
      return createResponse(c, false, 401, "Session expired or invalid");
    }

    const sessionData = { userId: session.user_id, username: session.username };
    
    // Recache session
    await c.env.REY_KV.put(`session:${token}`, JSON.stringify(sessionData), {
      expirationTtl: Math.max(60, Math.floor((new Date(session.expires_at).getTime() - Date.now()) / 1000))
    });

    c.set("jwtPayload" as any, sessionData);
    return next();
  } catch (err: any) {
    return createResponse(c, false, 500, "Database authentication error", { errors: [err.message] });
  }
});

// 5. MOUNT PROTECTED ROUTERS
app.route("/api/v1/gallery", galleryRouter);
app.route("/api/v1/location", locationRouter);
app.route("/api/v1/ai", aiRouter);
app.route("/api/v1/sync", syncRouter);
app.route("/api/v1/analytics", analyticsRouter);

// 6. GLOBAL ERROR HANDLER
app.onError(async (err, c) => {
  console.error("Worker Global Error:", err);
  await logEvent(c.env, "db", "ERROR", "global_server_error", err.message, {
    exception: err.stack
  });
  return createResponse(c, false, 500, "Internal server error occured", { errors: [err.message] });
});

app.notFound((c) => {
  return createResponse(c, false, 404, "Endpoint not found");
});

export default app;
