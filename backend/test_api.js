// Integration Test Runner for Hono API Gateway
// Tests routes, authentication shields, and standard envelopes non-disruptively.

import app from "./src/index.ts";

async function runTests() {
  console.log("==========================================");
  console.log("     ReY Backend API Integration Test    ");
  console.log("==========================================");

  // Mock Env bindings for testing
  const mockEnv = {
    API_VERSION: "v1",
    REY_DB: {
      prepare: (sql) => ({
        bind: () => ({
          first: async () => ({ count: 0 }),
          run: async () => ({ success: true })
        }),
        first: async () => null,
        all: async () => ({ results: [] })
      })
    },
    REY_KV: {
      get: async () => null,
      put: async () => {},
      delete: async () => {}
    }
  };

  let passed = 0;
  let failed = 0;

  async function assert(testName, path, method, headers, body, expectedStatus, checkBody) {
    try {
      const req = new Request(`http://localhost/api/v1${path}`, {
        method,
        headers: new Headers(headers),
        body: body ? JSON.stringify(body) : null
      });

      const res = await app.fetch(req, mockEnv);
      const resStatus = res.status;
      const resJson = await res.json().catch(() => null);

      if (resStatus === expectedStatus && (!checkBody || checkBody(resJson))) {
        console.log(`[PASS] ${testName}`);
        passed++;
      } else {
        console.log(`[FAIL] ${testName}`);
        console.log(`       Expected: Status ${expectedStatus}`);
        console.log(`       Got: Status ${resStatus}, Body:`, resJson);
        failed++;
      }
    } catch (e) {
      console.log(`[ERROR] ${testName}: ${e.message}`);
      failed++;
    }
  }

  // Test 1: Health Check Endpoint
  await assert(
    "GET /health should return 200 and online status",
    "/health",
    "GET",
    {},
    null,
    200,
    (body) => body.success === true && body.data.status === "healthy"
  );

  // Test 2: Auth Shield Verification
  await assert(
    "GET /gallery/list without token should return 401 Unauthorized",
    "/gallery/list",
    "GET",
    {},
    null,
    401,
    (body) => body.success === false && body.message.includes("Token missing")
  );

  // Test 3: Invalid login credentials
  await assert(
    "POST /auth/login with empty credentials should return 400",
    "/auth/login",
    "POST",
    { "Content-Type": "application/json" },
    { username: "", password: "" },
    400,
    (body) => body.success === false
  );

  console.log("\n==========================================");
  console.log(`Test Execution Finished. Passed: ${passed}, Failed: ${failed}`);
  console.log("==========================================");
}

runTests().catch(console.error);
