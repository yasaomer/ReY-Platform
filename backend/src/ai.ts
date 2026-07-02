import { Hono } from "hono";
import { Env } from "./types";
import { createResponse } from "./response";
import { logEvent } from "./logs";

const aiRouter = new Hono<{ Bindings: Env }>();

// 1. GET AI STATUS & CONFIG
aiRouter.get("/config", async (c) => {
  try {
    const config = await c.env.REY_DB.prepare(
      "SELECT * FROM ai_providers"
    ).all();
    return createResponse(c, true, 200, "AI configurations retrieved", config.results);
  } catch (err: any) {
    return createResponse(c, false, 500, "Failed to retrieve configurations", { errors: [err.message] });
  }
});

// 2. UPDATE AI CONFIG
aiRouter.post("/config", async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const { name, api_key, base_url, model, temperature, max_tokens, system_prompt, cooldown_seconds, daily_limit } = body;

  if (!name || !model) {
    return createResponse(c, false, 400, "Provider name and model name are required");
  }

  try {
    await c.env.REY_DB.prepare(
      `INSERT INTO ai_providers (name, api_key, base_url, model, temperature, max_tokens, system_prompt, cooldown_seconds, daily_limit)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
       ON CONFLICT(name) DO UPDATE SET
         api_key = excluded.api_key,
         base_url = excluded.base_url,
         model = excluded.model,
         temperature = excluded.temperature,
         max_tokens = excluded.max_tokens,
         system_prompt = excluded.system_prompt,
         cooldown_seconds = excluded.cooldown_seconds,
         daily_limit = excluded.daily_limit`
    ).bind(
      name, api_key || null, base_url || null, model, temperature || 0.7, max_tokens || 2048, system_prompt || null, cooldown_seconds || 0, daily_limit || 100
    ).run();

    await logEvent(c.env, "ai", "INFO", "config_update", `AI configuration updated for provider: ${name}`);
    return createResponse(c, true, 200, "AI Configuration saved successfully");
  } catch (err: any) {
    return createResponse(c, false, 500, "Failed to save configuration", { errors: [err.message] });
  }
});

// 3. RETRIEVE KNOWLEDGE CONTEXT (Simple keyword / phrase indexing search)
async function searchKnowledge(db: any, query: string): Promise<string> {
  const keywords = query
    .toLowerCase()
    .replace(/[^\w\s]/g, '')
    .split(/\s+/)
    .filter(w => w.length > 2); // Exclude stop words / short words

  if (keywords.length === 0) return "";

  // Dynamic SQL to find text segments matching keywords
  const conditions = keywords.map(() => "s.text_content LIKE ?").join(" OR ");
  const sql = `
    SELECT s.text_content, d.title, d.category 
    FROM document_segments s 
    JOIN knowledge_documents d ON s.doc_id = d.id 
    WHERE ${conditions}
    LIMIT 5
  `;

  const binds = keywords.map(kw => `%${kw}%`);

  try {
    const results = await db.prepare(sql).bind(...binds).all();
    if (!results.results || results.results.length === 0) return "";

    return results.results
      .map((row: any) => `[Source Document: ${row.title} (Category: ${row.category})]\n${row.text_content}`)
      .join("\n\n");
  } catch (e) {
    console.error("Knowledge search error:", e);
    return "";
  }
}

// 4. ASK CHATBOT (Handles RAG, prompt optimization and API integration)
aiRouter.post("/ask", async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const { message, conversationHistory } = body; // conversationHistory: [{role: 'user'|'model', text: string}]

  if (!message) {
    return createResponse(c, false, 400, "Message cannot be empty");
  }

  const startTime = Date.now();

  try {
    // A. Fetch current active provider (defaulting to Gemini)
    const provider = await c.env.REY_DB.prepare(
      "SELECT * FROM ai_providers WHERE name = 'gemini'"
    ).first() as any;

    const apiKey = provider?.api_key || c.env.GEMINI_API_KEY;
    const modelName = provider?.model || "gemini-1.5-flash";
    const temperature = provider?.temperature || 0.7;
    const systemPrompt = provider?.system_prompt || "You are ReY, an AI companion designed by the owner to represent them and answer private queries.";

    if (!apiKey) {
      return createResponse(c, false, 500, "AI API key has not been configured. Setup the AI provider in the APK.");
    }

    // B. Search Knowledge Base
    const context = await searchKnowledge(c.env.REY_DB, message);

    // C. Build optimized prompt
    let finalPrompt = "";
    if (context) {
      finalPrompt += `[Retrieved Information Context]\n${context}\n\n`;
      finalPrompt += `Use the above retrieved context to accurately answer the question. If the context is unrelated, fall back on your default system personality instructions.\n\n`;
    }
    finalPrompt += `User Question: ${message}`;

    // D. Call Gemini API
    const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/${modelName}:generateContent?key=${apiKey}`;
    
    // Prepare contents array with history
    const contents = [];
    if (conversationHistory && Array.isArray(conversationHistory)) {
      // Map history roles
      for (const turn of conversationHistory) {
        contents.push({
          role: turn.role === 'model' ? 'model' : 'user',
          parts: [{ text: turn.text }]
        });
      }
    }
    
    // Append the current turn
    contents.push({
      role: 'user',
      parts: [{ text: finalPrompt }]
    });

    const response = await fetch(geminiUrl, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        contents,
        systemInstruction: {
          parts: [{ text: systemPrompt }]
        },
        generationConfig: {
          temperature: temperature,
          maxOutputTokens: provider?.max_tokens || 2048
        }
      })
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Gemini API Error (${response.status}): ${errorText}`);
    }

    const resJson = await response.json() as any;
    const answer = resJson.candidates?.[0]?.content?.parts?.[0]?.text;

    if (!answer) {
      throw new Error("Empty response returned from AI provider");
    }

    const latency = Date.now() - startTime;
    const promptTokens = resJson.usageMetadata?.promptTokenCount || 0;
    const completionTokens = resJson.usageMetadata?.candidatesTokenCount || 0;

    // E. Log analytics
    await c.env.REY_DB.prepare(
      `INSERT INTO ai_statistics (provider, model, latency_ms, prompt_tokens, completion_tokens, status)
       VALUES (?, ?, ?, ?, ?, 'success')`
    ).bind('gemini', modelName, latency, promptTokens, completionTokens).run();

    await logEvent(c.env, "ai", "INFO", "ai_response", `AI responded successfully in ${latency}ms`, {
      durationMs: latency
    });

    return createResponse(c, true, 200, "AI responded successfully", {
      answer,
      latencyMs: latency,
      tokensUsed: promptTokens + completionTokens
    });
  } catch (err: any) {
    const latency = Date.now() - startTime;
    
    // Log error to statistics
    await c.env.REY_DB.prepare(
      `INSERT INTO ai_statistics (provider, model, latency_ms, status, error_message)
       VALUES (?, ?, ?, 'failed', ?)`
    ).bind('gemini', 'gemini-1.5-flash', latency, err.message).run();

    await logEvent(c.env, "ai", "ERROR", "ai_exception", err.message, {
      exception: err.stack,
      durationMs: latency
    });

    return createResponse(c, false, 500, "Temporary AI provider issue. Please try again.", {
      errors: [err.message]
    });
  }
});

export default aiRouter;
