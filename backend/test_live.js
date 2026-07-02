// Live Integration Test Runner for ReY Platform API Gateway
// Sends real HTTP requests to verify routing, D1 schema responses, and authentication rules.

async function runLiveTests() {
  const targetUrl = "http://localhost:8787/api/v1";
  console.log("==========================================");
  console.log(`  Live API Integration Test: ${targetUrl}`);
  console.log("==========================================");

  let passed = 0;
  let failed = 0;

  async function test(name, endpoint, options, expectedStatus, validationFn) {
    try {
      const response = await fetch(`${targetUrl}${endpoint}`, {
        ...options,
        headers: {
          "Content-Type": "application/json",
          ...options.headers
        }
      });

      const body = await response.json().catch(() => null);

      if (response.status === expectedStatus && (!validationFn || validationFn(body))) {
        console.log(`[PASS] ${name}`);
        passed++;
      } else {
        console.log(`[FAIL] ${name}`);
        console.log(`       Expected status: ${expectedStatus}, Got: ${response.status}`);
        console.log("       Response Body:", body);
        failed++;
      }
    } catch (e) {
      console.log(`[ERROR] ${name}: Connection failed (${e.message})`);
      failed++;
    }
  }

  // 1. Health check
  await test(
    "GET /health is online",
    "/health",
    { method: "GET" },
    200,
    (body) => body.success === true && body.data.status === "healthy"
  );

  // 2. Auth block
  await test(
    "GET /gallery/list blocks unauthorized viewers",
    "/gallery/list",
    { method: "GET" },
    401,
    (body) => body.success === false
  );

  // 3. Login checks
  await test(
    "POST /auth/login handles empty request payloads",
    "/auth/login",
    {
      method: "POST",
      body: JSON.stringify({})
    },
    400,
    (body) => body.success === false
  );

  console.log("\n==========================================");
  console.log(`Live Tests Finished. Passed: ${passed}, Failed: ${failed}`);
  console.log("==========================================");
}

runLiveTests().catch(console.error);
