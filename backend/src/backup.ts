import { Hono } from "hono";
import { Env } from "./types";
import { createResponse } from "./response";
import { logEvent } from "./logs";

const backupRouter = new Hono<{ Bindings: Env }>();

// 1. GET ALL BACKUPS
backupRouter.get("/", async (c) => {
  try {
    const backups = await c.env.REY_DB.prepare(
      "SELECT id, name, timestamp, size_bytes, status, file_path_drive FROM system_backups WHERE status = 'available' ORDER BY timestamp DESC"
    ).all();
    return createResponse(c, true, 200, "Backups retrieved successfully", backups.results);
  } catch (err: any) {
    return createResponse(c, false, 500, "Failed to retrieve backups", { errors: [err.message] });
  }
});

// 2. CREATE A NEW BACKUP POINT
backupRouter.post("/create", async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const { name } = body;

  if (!name) {
    return createResponse(c, false, 400, "Backup name is required");
  }

  try {
    // 1. Collect all database metadata to compile the backup point payload
    const configs = await c.env.REY_DB.prepare("SELECT * FROM configuration").all();
    const gallery = await c.env.REY_DB.prepare("SELECT * FROM gallery_metadata").all();
    const message = await c.env.REY_DB.prepare("SELECT * FROM last_message").all();
    const locations = await c.env.REY_DB.prepare("SELECT * FROM location_status").all();
    const providers = await c.env.REY_DB.prepare("SELECT * FROM ai_providers").all();
    const docs = await c.env.REY_DB.prepare("SELECT * FROM knowledge_documents").all();
    const socials = await c.env.REY_DB.prepare("SELECT * FROM configuration WHERE config_key LIKE 'social_media_%'").all();

    const backupPayload = {
      configuration: configs.results,
      gallery_metadata: gallery.results,
      last_message: message.results,
      location_status: locations.results,
      ai_providers: providers.results,
      knowledge_documents: docs.results,
      social_media: socials.results
    };

    const payloadString = JSON.stringify(backupPayload);
    const sizeBytes = payloadString.length;
    const backupId = "bk-" + Math.random().toString(36).substring(2, 10);
    const timestamp = new Date().toISOString();

    // 2. Insert backup record
    await c.env.REY_DB.prepare(
      `INSERT INTO system_backups (id, name, timestamp, size_bytes, status, payload_json)
       VALUES (?, ?, ?, ?, 'available', ?)`
    ).bind(backupId, name, timestamp, sizeBytes, payloadString).run();

    await logEvent(c.env, "sync", "INFO", "backup_created", `Backup point '${name}' (${backupId}) created successfully`);

    return createResponse(c, true, 200, "Backup created successfully", { id: backupId, name, sizeBytes });
  } catch (err: any) {
    return createResponse(c, false, 500, "Failed to create backup point", { errors: [err.message] });
  }
});

// 3. RESTORE A BACKUP POINT
backupRouter.post("/restore", async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const { id } = body;

  if (!id) {
    return createResponse(c, false, 400, "Backup ID is required");
  }

  try {
    const backup = await c.env.REY_DB.prepare(
      "SELECT payload_json FROM system_backups WHERE id = ? AND status = 'available'"
    ).bind(id).first() as any;

    if (!backup) {
      return createResponse(c, false, 404, "Backup point not found");
    }

    const payload = JSON.parse(backup.payload_json);

    // Perform database overrides
    if (payload.configuration) {
      for (const row of payload.configuration) {
        await c.env.REY_DB.prepare(
          "INSERT OR REPLACE INTO configuration (config_key, config_value) VALUES (?, ?)"
        ).bind(row.config_key, row.config_value).run();
      }
    }

    if (payload.gallery_metadata) {
      for (const row of payload.gallery_metadata) {
        await c.env.REY_DB.prepare(
          `INSERT OR REPLACE INTO gallery_metadata (image_id, file_name, file_hash, visibility, is_favorite, is_hidden, status, created_time, modified_time)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`
        ).bind(row.image_id, row.file_name, row.file_hash, row.visibility, row.is_favorite, row.is_hidden, row.status, row.created_time, row.modified_time).run();
      }
    }

    if (payload.last_message) {
      for (const row of payload.last_message) {
        await c.env.REY_DB.prepare(
          "INSERT OR REPLACE INTO last_message (id, message_content, created_at, updated_at, version) VALUES (?, ?, ?, ?, ?)"
        ).bind(row.id, row.message_content, row.created_at, row.updated_at, row.version).run();
      }
    }

    if (payload.ai_providers) {
      for (const row of payload.ai_providers) {
        await c.env.REY_DB.prepare(
          `INSERT OR REPLACE INTO ai_providers (name, api_key, base_url, model, temperature, max_tokens, system_prompt, cooldown_seconds, daily_limit)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`
        ).bind(row.name, row.api_key, row.base_url, row.model, row.temperature, row.max_tokens, row.system_prompt, row.cooldown_seconds, row.daily_limit).run();
      }
    }

    await logEvent(c.env, "sync", "INFO", "backup_restored", `Ecosystem state restored to backup point: ${id}`);

    return createResponse(c, true, 200, "System database restored successfully");
  } catch (err: any) {
    return createResponse(c, false, 500, "Failed to restore backup point", { errors: [err.message] });
  }
});

// 4. EXPORT A BACKUP
backupRouter.get("/export/:id", async (c) => {
  const id = c.req.param("id");
  try {
    const backup = await c.env.REY_DB.prepare(
      "SELECT name, payload_json FROM system_backups WHERE id = ? AND status = 'available'"
    ).bind(id).first() as any;

    if (!backup) {
      return createResponse(c, false, 404, "Backup point not found");
    }

    return createResponse(c, true, 200, "Export compiled", {
      name: backup.name,
      payload: backup.payload_json
    });
  } catch (err: any) {
    return createResponse(c, false, 500, "Failed to export backup", { errors: [err.message] });
  }
});

// 5. IMPORT A BACKUP
backupRouter.post("/import", async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const { name, payload } = body;

  if (!name || !payload) {
    return createResponse(c, false, 400, "Backup name and payload are required");
  }

  try {
    const backupId = "bk-" + Math.random().toString(36).substring(2, 10);
    const timestamp = new Date().toISOString();
    const sizeBytes = payload.length;

    await c.env.REY_DB.prepare(
      `INSERT INTO system_backups (id, name, timestamp, size_bytes, status, payload_json)
       VALUES (?, ?, ?, ?, 'available', ?)`
    ).bind(backupId, name, timestamp, sizeBytes, payload).run();

    await logEvent(c.env, "sync", "INFO", "backup_imported", `External backup point '${name}' (${backupId}) imported`);

    return createResponse(c, true, 200, "Backup imported successfully", { id: backupId, name, sizeBytes });
  } catch (err: any) {
    return createResponse(c, false, 500, "Failed to import backup payload", { errors: [err.message] });
  }
});

// 6. DELETE A BACKUP
backupRouter.delete("/:id", async (c) => {
  const id = c.req.param("id");
  try {
    await c.env.REY_DB.prepare(
      "UPDATE system_backups SET status = 'deleted' WHERE id = ?"
    ).bind(id).run();
    await logEvent(c.env, "sync", "INFO", "backup_deleted", `Backup point ${id} removed`);
    return createResponse(c, true, 200, "Backup deleted successfully");
  } catch (err: any) {
    return createResponse(c, false, 500, "Failed to delete backup point", { errors: [err.message] });
  }
});

export default backupRouter;
