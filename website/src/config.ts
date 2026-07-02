export const API_BASE = 
  window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1"
    ? "http://localhost:8787/api/v1"
    : "https://rey-backend.yasaomer123.workers.dev/api/v1";
