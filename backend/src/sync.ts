import { Hono } from "hono";
import { Env } from "./types";
import { createResponse } from "./response";
import { logEvent } from "./logs";

const syncRouter = new Hono<{ Bindings: Env }>();

// 1. PULL PENDING TASKS (called by APK background sync loops)
syncRouter.get("/pull", async (c) => {
  try {
    const tasks = await c.env.REY_DB.prepare(
      "SELECT * FROM sync_queue WHERE status = 'pending' ORDER BY priority ASC, creation_time ASC"
    ).all();

    return createResponse(c, true, 200, "Sync tasks retrieved", tasks.results);
  } catch (err: any) {
    return createResponse(c, false, 500, "Failed to pull tasks", { errors: [err.message] });
  }
});

// 2. COMPLETE SYNC TASK (called by APK when finished executing a task)
syncRouter.post("/complete", async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const { taskId, status, errorInfo } = body; // status: completed, failed

  if (!taskId || !status) {
    return createResponse(c, false, 400, "Task ID and status are required");
  }

  try {
    const task = await c.env.REY_DB.prepare(
      "SELECT * FROM sync_queue WHERE task_id = ?"
    ).bind(taskId).first() as any;

    if (!task) {
      return createResponse(c, false, 404, "Task not found in queue");
    }

    const completionTime = new Date().toISOString();

    // Move to sync_history or update queue status
    await c.env.REY_DB.prepare(
      "UPDATE sync_queue SET status = ?, completion_time = ?, error_info = ? WHERE task_id = ?"
    ).bind(status, completionTime, errorInfo || null, taskId).run();

    await c.env.REY_DB.prepare(
      `INSERT INTO sync_history (task_id, task_type, status, timestamp, error_message)
       VALUES (?, ?, ?, ?, ?)`
    ).bind(taskId, task.task_type, status, completionTime, errorInfo || null).run();

    // Delete completed tasks from queue to keep it small
    if (status === "completed") {
      await c.env.REY_DB.prepare(
        "DELETE FROM sync_queue WHERE task_id = ?"
      ).bind(taskId).run();
    }

    await logEvent(c.env, "sync", status === "completed" ? "INFO" : "ERROR", "task_completed", `Task ${taskId} (${task.task_type}) marked as ${status}`);

    return createResponse(c, true, 200, "Task completion recorded successfully");
  } catch (err: any) {
    return createResponse(c, false, 500, "Failed to complete task", { errors: [err.message] });
  }
});

// 3. GET LAST MESSAGE (for Website and APK)
syncRouter.get("/last-message", async (c) => {
  try {
    const msg = await c.env.REY_DB.prepare(
      "SELECT * FROM last_message ORDER BY updated_at DESC LIMIT 1"
    ).first() as any;

    if (!msg) {
      return createResponse(c, true, 200, "No message published yet", { message_content: "" });
    }

    return createResponse(c, true, 200, "Last message retrieved", msg);
  } catch (err: any) {
    return createResponse(c, false, 500, "Failed to get last message", { errors: [err.message] });
  }
});

// 4. PUBLISH LAST MESSAGE (called by APK)
syncRouter.post("/last-message", async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const { message_content } = body;

  if (message_content === undefined) {
    return createResponse(c, false, 400, "message_content parameter is required");
  }

  try {
    const now = new Date().toISOString();
    
    // Check if entry exists to increment version
    const current = await c.env.REY_DB.prepare(
      "SELECT version FROM last_message ORDER BY updated_at DESC LIMIT 1"
    ).first() as any;
    
    const nextVersion = current ? current.version + 1 : 1;

    await c.env.REY_DB.prepare(
      `INSERT INTO last_message (message_content, created_at, updated_at, version)
       VALUES (?, ?, ?, ?)`
    ).bind(message_content, now, now, nextVersion).run();

    await logEvent(c.env, "sync", "INFO", "last_message_published", `New status message published. Version: ${nextVersion}`);

    return createResponse(c, true, 200, "Message published successfully", { version: nextVersion });
  } catch (err: any) {
    return createResponse(c, false, 500, "Failed to publish message", { errors: [err.message] });
  }
});


// 5. GET SOCIAL MEDIA CONFIG (read by Website)
syncRouter.get("/social-config", async (c) => {
  try {
    const row = await c.env.REY_DB.prepare(
      "SELECT config_value FROM configuration WHERE config_key = 'social_media_platforms'"
    ).first() as any;

    const introRow = await c.env.REY_DB.prepare(
      "SELECT config_value FROM configuration WHERE config_key = 'social_media_intro'"
    ).first() as any;

    return createResponse(c, true, 200, "Social config retrieved", {
      platforms : row      ? JSON.parse(row.config_value)   : [],
      intro     : introRow ? introRow.config_value           : ""
    });
  } catch (err: any) {
    return createResponse(c, false, 500, "Failed to retrieve social config", { errors: [err.message] });
  }
});

// 6. POST SOCIAL MEDIA CONFIG (written by APK)
syncRouter.post("/social-config", async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const { platforms, intro } = body;

  try {
    const now = new Date().toISOString();

    if (platforms !== undefined) {
      const val = typeof platforms === "string" ? platforms : JSON.stringify(platforms);
      await c.env.REY_DB.prepare(
        `INSERT INTO configuration (config_key, config_value, updated_at)
         VALUES ('social_media_platforms', ?, ?)
         ON CONFLICT(config_key) DO UPDATE SET config_value = excluded.config_value, updated_at = excluded.updated_at`
      ).bind(val, now).run();
    }

    if (intro !== undefined) {
      await c.env.REY_DB.prepare(
        `INSERT INTO configuration (config_key, config_value, updated_at)
         VALUES ('social_media_intro', ?, ?)
         ON CONFLICT(config_key) DO UPDATE SET config_value = excluded.config_value, updated_at = excluded.updated_at`
      ).bind(String(intro), now).run();
    }

    await logEvent(c.env, "sync", "INFO", "social_config_updated", "Social media configuration synced from APK");
    return createResponse(c, true, 200, "Social media configuration saved successfully");
  } catch (err: any) {
    return createResponse(c, false, 500, "Failed to save social config", { errors: [err.message] });
  }
});

// 7. GET SYNC QUEUE (for Website)
syncRouter.get("/queue", async (c) => {
  try {
    const queue = await c.env.REY_DB.prepare(
      "SELECT * FROM sync_queue ORDER BY priority ASC, creation_time DESC"
    ).all();
    return createResponse(c, true, 200, "Sync queue loaded", queue.results);
  } catch (err: any) {
    return createResponse(c, false, 500, "Failed to load sync queue", { errors: [err.message] });
  }
});

// 8. GET SYSTEM LOGS (for Website log viewer)
syncRouter.get("/logs", async (c) => {
  try {
    const logs = await c.env.REY_DB.prepare(
      "SELECT * FROM system_logs ORDER BY timestamp DESC LIMIT 150"
    ).all();
    return createResponse(c, true, 200, "System logs loaded", logs.results);
  } catch (err: any) {
    return createResponse(c, false, 500, "Failed to load system logs", { errors: [err.message] });
  }
});

// 9. GET SYNC STATUS (sharing-status)
syncRouter.get("/sharing-status", async (c) => {
  try {
    const pausedRow = await c.env.REY_DB.prepare(
      "SELECT config_value FROM configuration WHERE config_key = 'sync_queue_paused'"
    ).first() as any;
    const paused = pausedRow ? pausedRow.config_value === "true" : false;
    return createResponse(c, true, 200, "Ecosystem sharing status", { paused });
  } catch (err: any) {
    return createResponse(c, false, 500, "Failed to read sharing status", { errors: [err.message] });
  }
});

// 10. POST QUEUE ACTION
syncRouter.post("/queue/action", async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const { action, taskId } = body;

  try {
    if (action === "pause") {
      await c.env.REY_DB.prepare(
        `INSERT INTO configuration (config_key, config_value) VALUES ('sync_queue_paused', 'true')
         ON CONFLICT(config_key) DO UPDATE SET config_value = 'true'`
      ).run();
      await logEvent(c.env, "sync", "INFO", "queue_paused", "Sync queue paused by owner");
    } else if (action === "resume") {
      await c.env.REY_DB.prepare(
        `INSERT INTO configuration (config_key, config_value) VALUES ('sync_queue_paused', 'false')
         ON CONFLICT(config_key) DO UPDATE SET config_value = 'false'`
      ).run();
      await logEvent(c.env, "sync", "INFO", "queue_resumed", "Sync queue resumed by owner");
    } else if (action === "retry_all") {
      await c.env.REY_DB.prepare(
        "UPDATE sync_queue SET status = 'pending', error_info = NULL WHERE status = 'failed'"
      ).run();
      await logEvent(c.env, "sync", "INFO", "queue_retry_all", "Re-queued all failed tasks");
    } else if (action === "clear_completed") {
      await c.env.REY_DB.prepare(
        "DELETE FROM sync_queue WHERE status = 'completed'"
      ).run();
      await logEvent(c.env, "sync", "INFO", "queue_clear_completed", "Cleared all completed tasks");
    } else if (action === "clear_logs") {
      await c.env.REY_DB.prepare(
        "DELETE FROM system_logs"
      ).run();
      await logEvent(c.env, "sync", "INFO", "logs_cleared", "System logs cleared by owner");
    } else if (action === "cancel" && taskId) {
      await c.env.REY_DB.prepare(
        "DELETE FROM sync_queue WHERE task_id = ?"
      ).bind(taskId).run();
      await logEvent(c.env, "sync", "INFO", "task_cancelled", `Task ${taskId} cancelled and deleted`);
    }

    return createResponse(c, true, 200, `Action ${action} executed successfully`);
  } catch (err: any) {
    return createResponse(c, false, 500, `Failed to execute action ${action}`, { errors: [err.message] });
  }
});

export default syncRouter;
