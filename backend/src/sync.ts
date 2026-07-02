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

export default syncRouter;
