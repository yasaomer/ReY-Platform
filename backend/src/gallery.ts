import { Hono } from "hono";
import { Env } from "./types";
import { createResponse } from "./response";
import { logEvent } from "./logs";

const galleryRouter = new Hono<{ Bindings: Env }>();

// 1. LIST IMAGES (for authenticated viewers on the website)
galleryRouter.get("/list", async (c) => {
  try {
    const images = await c.env.REY_DB.prepare(
      "SELECT * FROM gallery_metadata WHERE status = 'available' AND is_hidden = 0 ORDER BY created_time DESC"
    ).all();

    return createResponse(c, true, 200, "Gallery loaded", images.results);
  } catch (err: any) {
    return createResponse(c, false, 500, "Failed to load gallery", { errors: [err.message] });
  }
});

// 2. UPDATE METADATA (called by APK during folder monitoring sync)
galleryRouter.post("/metadata", async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const { image_id, file_name, file_hash, thumbnail_id, width, height, size_bytes, created_time, modified_time, visibility, category, is_favorite, is_hidden, status } = body;

  if (!image_id || !file_name || !file_hash) {
    return createResponse(c, false, 400, "Missing required metadata parameters (image_id, file_name, file_hash)");
  }

  try {
    await c.env.REY_DB.prepare(
      `INSERT INTO gallery_metadata (
        image_id, file_name, file_hash, thumbnail_id, width, height, size_bytes,
        created_time, modified_time, visibility, category, is_favorite, is_hidden, status
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      ON CONFLICT(image_id) DO UPDATE SET
        file_name = excluded.file_name,
        file_hash = excluded.file_hash,
        thumbnail_id = excluded.thumbnail_id,
        width = excluded.width,
        height = excluded.height,
        size_bytes = excluded.size_bytes,
        modified_time = excluded.modified_time,
        visibility = excluded.visibility,
        category = excluded.category,
        is_favorite = excluded.is_favorite,
        is_hidden = excluded.is_hidden,
        status = excluded.status`
    ).bind(
      image_id, file_name, file_hash, thumbnail_id || null, width || null, height || null, size_bytes || null,
      created_time, modified_time, visibility || 'public', category || null, is_favorite || 0, is_hidden || 0, status || 'available'
    ).run();

    await logEvent(c.env, "gallery", "INFO", "metadata_update", `Metadata updated for: ${file_name} (${status})`);
    return createResponse(c, true, 200, "Metadata synchronized successfully");
  } catch (err: any) {
    return createResponse(c, false, 500, "Failed to sync metadata", { errors: [err.message] });
  }
});

// 3. LOG IMAGE VIEW (for analytics)
galleryRouter.post("/log-view", async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const { image_id, event_type, duration_seconds } = body; // event_type: view, zoom, download, share

  if (!image_id || !event_type) {
    return createResponse(c, false, 400, "Image ID and Event Type are required");
  }

  try {
    const timestamp = new Date().toISOString();
    
    // Log statistics
    await c.env.REY_DB.prepare(
      "INSERT INTO gallery_statistics (image_id, event_type, timestamp, duration_seconds) VALUES (?, ?, ?, ?)"
    ).bind(image_id, event_type, timestamp, duration_seconds || null).run();

    // Increment overall view count if event is a main view
    if (event_type === 'view') {
      await c.env.REY_DB.prepare(
        "UPDATE gallery_metadata SET view_count = view_count + 1 WHERE image_id = ?"
      ).bind(image_id).run();
    }

    return createResponse(c, true, 200, "Activity logged successfully");
  } catch (err: any) {
    return createResponse(c, false, 500, "Failed to log activity", { errors: [err.message] });
  }
});

export default galleryRouter;
