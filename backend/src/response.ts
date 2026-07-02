import { Context } from "hono";
import { StandardResponse } from "./types";

export function createResponse<T = any>(
  c: Context,
  success: boolean,
  statusCode: number,
  message: string,
  data?: T,
  options?: {
    metadata?: any;
    errors?: string[];
  }
): Response {
  const requestId = c.req.header("cf-ray") || crypto.randomUUID();
  const timestamp = new Date().toISOString();
  
  const body: StandardResponse<T> = {
    success,
    timestamp,
    requestId,
    statusCode,
    message,
    data,
    metadata: options?.metadata,
    errors: options?.errors
  };
  
  return c.json(body, statusCode as any);
}
