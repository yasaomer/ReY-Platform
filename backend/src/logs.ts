import { Env } from "./types";

export type LogSeverity = "INFO" | "WARNING" | "ERROR" | "SECURITY";

export async function logEvent(
  env: Env,
  module: string,
  severity: LogSeverity,
  action: string,
  message: string,
  options?: {
    exception?: string;
    durationMs?: number;
    userId?: number;
    requestId?: string;
  }
) {
  try {
    const timestamp = new Date().toISOString();
    
    // 1. Insert into system_logs
    await env.REY_DB.prepare(
      `INSERT INTO system_logs (timestamp, module, severity, action, message, exception, duration_ms, user_id, request_id)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`
    ).bind(
      timestamp,
      module,
      severity,
      action,
      message,
      options?.exception || null,
      options?.durationMs || null,
      options?.userId || null,
      options?.requestId || null
    ).run();

    // 2. If it's a security event, warning, or error, insert a notification alert
    if (severity === "ERROR" || severity === "WARNING" || severity === "SECURITY") {
      let category = severity.toLowerCase();
      if (severity === "SECURITY") category = "security";
      
      await env.REY_DB.prepare(
        `INSERT INTO notifications (category, message, timestamp, is_read)
         VALUES (?, ?, ?, 0)`
      ).bind(
        category,
        `[${module} - ${action}] ${message}`,
        timestamp
      ).run();
    }
  } catch (e) {
    console.error("Failed to write to logging database:", e);
  }
}
