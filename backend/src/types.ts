import { KVNamespace, D1Database } from "@cloudflare/workers-types";

export interface Env {
  REY_DB: D1Database;
  REY_KV: KVNamespace;
  API_VERSION?: string;
  // External secrets can be set via wrangler secret put
  GEMINI_API_KEY?: string;
  OPENAI_API_KEY?: string;
  DEEPSEEK_API_KEY?: string;
}

export interface StandardResponse<T = any> {
  success: boolean;
  timestamp: string;
  requestId: string;
  statusCode: number;
  message: string;
  data?: T;
  metadata?: any;
  errors?: string[];
}
