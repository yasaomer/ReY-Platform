import { Hono } from "hono";
import { Env } from "./types";
import { createResponse } from "./response";
import { hashPassword, verifyPassword, generateToken, generateVerificationCode } from "./crypto";
import { logEvent } from "./logs";

const authRouter = new Hono<{ Bindings: Env }>();

// Helper to seed default user if none exist
async function seedDefaultUser(db: any) {
  const countRes = await db.prepare("SELECT COUNT(*) as count FROM auth_users").first();
  if (countRes && countRes.count === 0) {
    const defaultUsername = "Rozuly";
    const defaultPassword = "Roza1448404Ali";
    const hashed = await hashPassword(defaultPassword);
    await db.prepare(
      "INSERT INTO auth_users (username, password_hash, backup_phone) VALUES (?, ?, ?)"
    ).bind(defaultUsername, hashed, "").run();
  }
}

// 1. LOGIN
authRouter.post("/login", async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const { username, password } = body;
  
  if (!username || !password) {
    return createResponse(c, false, 400, "Username and password are required");
  }

  const startTime = Date.now();
  await seedDefaultUser(c.env.REY_DB);

  try {
    const user = await c.env.REY_DB.prepare(
      "SELECT * FROM auth_users WHERE username = ?"
    ).bind(username).first() as any;

    if (!user) {
      await logEvent(c.env, "auth", "SECURITY", "failed_login", `User not found: ${username}`);
      return createResponse(c, false, 401, "Invalid username or password");
    }

    const isValid = await verifyPassword(password, user.password_hash);
    if (!isValid) {
      await logEvent(c.env, "auth", "SECURITY", "failed_login", `Incorrect password for: ${username}`);
      return createResponse(c, false, 401, "Invalid username or password");
    }

    // Generate session
    const token = generateToken();
    const expiresAt = new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(); // 24 Hours
    const createdAt = new Date().toISOString();
    const deviceInfo = c.req.header("user-agent") || "unknown";

    await c.env.REY_DB.prepare(
      "INSERT INTO sessions (id, user_id, created_at, expires_at, device_info, is_valid) VALUES (?, ?, ?, ?, ?, 1)"
    ).bind(token, user.id, createdAt, expiresAt, deviceInfo).run();

    // Cache session in KV for fast validation
    await c.env.REY_KV.put(`session:${token}`, JSON.stringify({ userId: user.id, username: user.username }), {
      expirationTtl: 24 * 60 * 60
    });

    await logEvent(c.env, "auth", "INFO", "successful_login", `User logged in: ${username}`, {
      userId: user.id,
      durationMs: Date.now() - startTime
    });

    return createResponse(c, true, 200, "Login successful", {
      token,
      expiresAt,
      username: user.username
    });
  } catch (err: any) {
    await logEvent(c.env, "auth", "ERROR", "login_exception", err.message, {
      exception: err.stack
    });
    return createResponse(c, false, 500, "Internal Server Error");
  }
});

// 2. LOGOUT
authRouter.post("/logout", async (c) => {
  const authHeader = c.req.header("Authorization");
  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    return createResponse(c, false, 401, "Unauthorized");
  }
  const token = authHeader.substring(7);

  try {
    await c.env.REY_DB.prepare(
      "UPDATE sessions SET is_valid = 0 WHERE id = ?"
    ).bind(token).run();
    
    await c.env.REY_KV.delete(`session:${token}`);
    await logEvent(c.env, "auth", "INFO", "logout", "Session terminated successfully");
    
    return createResponse(c, true, 200, "Logged out successfully");
  } catch (err: any) {
    return createResponse(c, false, 500, "Error terminating session", { errors: [err.message] });
  }
});

// 3. VALIDATE SESSION
authRouter.get("/validate", async (c) => {
  const authHeader = c.req.header("Authorization");
  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    return createResponse(c, false, 401, "Unauthorized");
  }
  const token = authHeader.substring(7);

  // Check KV cache first
  const kvSession = await c.env.REY_KV.get(`session:${token}`);
  if (kvSession) {
    const sessionData = JSON.parse(kvSession);
    return createResponse(c, true, 200, "Session is valid", sessionData);
  }

  // Fallback to D1 database
  try {
    const session = await c.env.REY_DB.prepare(
      "SELECT s.*, u.username FROM sessions s JOIN auth_users u ON s.user_id = u.id WHERE s.id = ? AND s.is_valid = 1 AND s.expires_at > ?"
    ).bind(token, new Date().toISOString()).first() as any;

    if (!session) {
      return createResponse(c, false, 401, "Session expired or invalid");
    }

    // Recache in KV
    await c.env.REY_KV.put(`session:${token}`, JSON.stringify({ userId: session.user_id, username: session.username }), {
      expirationTtl: Math.floor((new Date(session.expires_at).getTime() - Date.now()) / 1000)
    });

    return createResponse(c, true, 200, "Session is valid", {
      userId: session.user_id,
      username: session.username
    });
  } catch (err: any) {
    return createResponse(c, false, 500, "Validation error", { errors: [err.message] });
  }
});

// 4. FORGOT PASSWORD WORKFLOW
authRouter.post("/forgot-password", async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const { username } = body;

  if (!username) {
    return createResponse(c, false, 400, "Username is required");
  }

  try {
    const user = await c.env.REY_DB.prepare(
      "SELECT id, backup_phone FROM auth_users WHERE username = ?"
    ).bind(username).first() as any;

    if (!user) {
      // Avoid revealing username exists status, but don't schedule recovery
      return createResponse(c, true, 200, "If the account exists, a recovery code is being sent.");
    }

    // Get configured SIM recovery number from configuration
    const recoveryConfig = await c.env.REY_DB.prepare(
      "SELECT config_value FROM configuration WHERE config_key = 'recovery_phone_number'"
    ).first() as any;
    const phone = recoveryConfig?.config_value || user.backup_phone;

    if (!phone) {
      return createResponse(c, false, 400, "No recovery phone number has been configured on the APK.");
    }

    // Generate random 6 digit OTP
    const code = generateVerificationCode();
    const expiresAt = new Date(Date.now() + 10 * 60 * 1000).toISOString(); // 10 mins

    // Store in KV: recovery_code:<username> -> { code, expiresAt, attemptsLeft: 3 }
    await c.env.REY_KV.put(
      `recovery_code:${username}`,
      JSON.stringify({ code, expiresAt, attemptsLeft: 3 }),
      { expirationTtl: 600 }
    );

    // Queue synchronization task for the APK to send SMS
    const taskId = crypto.randomUUID();
    await c.env.REY_DB.prepare(
      `INSERT INTO sync_queue (task_id, task_type, priority, status, target_module, error_info)
       VALUES (?, 'send_sms_reset', 1, 'pending', 'auth', ?)`
    ).bind(
      taskId,
      JSON.stringify({ phoneNumber: phone, message: `Your ReY recovery code is: ${code}` })
    ).run();

    await logEvent(c.env, "auth", "INFO", "forgot_password_request", `OTP queued for transmission to ${phone}`);

    return createResponse(c, true, 200, "Verification code sent successfully.");
  } catch (err: any) {
    return createResponse(c, false, 500, "Forgot password error", { errors: [err.message] });
  }
});

// 5. VERIFY RECOVERY CODE
authRouter.post("/verify-recovery-code", async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const { username, code } = body;

  if (!username || !code) {
    return createResponse(c, false, 400, "Username and code are required");
  }

  try {
    const rawOtp = await c.env.REY_KV.get(`recovery_code:${username}`);
    if (!rawOtp) {
      return createResponse(c, false, 400, "Code has expired or is invalid");
    }

    const otpData = JSON.parse(rawOtp);
    if (otpData.attemptsLeft <= 0) {
      return createResponse(c, false, 400, "Max attempts exceeded. Please request a new code.");
    }

    if (new Date().toISOString() > otpData.expiresAt) {
      await c.env.REY_KV.delete(`recovery_code:${username}`);
      return createResponse(c, false, 400, "Verification code has expired");
    }

    if (otpData.code !== code) {
      otpData.attemptsLeft -= 1;
      await c.env.REY_KV.put(`recovery_code:${username}`, JSON.stringify(otpData), { expirationTtl: 600 });
      await logEvent(c.env, "auth", "WARNING", "otp_failed", `Incorrect OTP attempt for user ${username}. Attempts left: ${otpData.attemptsLeft}`);
      return createResponse(c, false, 400, `Incorrect verification code. Attempts remaining: ${otpData.attemptsLeft}`);
    }

    // Valid code! Delete code, and generate a temporary reset token valid for 5 mins
    await c.env.REY_KV.delete(`recovery_code:${username}`);
    
    const resetToken = generateToken();
    await c.env.REY_KV.put(`reset_token:${username}`, resetToken, { expirationTtl: 300 });

    await logEvent(c.env, "auth", "INFO", "otp_success", `OTP verified successfully for user ${username}`);

    return createResponse(c, true, 200, "Code verified successfully.", { resetToken });
  } catch (err: any) {
    return createResponse(c, false, 500, "Verification error", { errors: [err.message] });
  }
});

// 6. RESET PASSWORD
authRouter.post("/reset-password", async (c) => {
  const body = await c.req.json().catch(() => ({}));
  const { username, resetToken, newPassword } = body;

  if (!username || !resetToken || !newPassword) {
    return createResponse(c, false, 400, "Username, reset token, and new password are required");
  }

  try {
    const cachedToken = await c.env.REY_KV.get(`reset_token:${username}`);
    if (!cachedToken || cachedToken !== resetToken) {
      return createResponse(c, false, 400, "Invalid or expired reset token");
    }

    await c.env.REY_KV.delete(`reset_token:${username}`);
    
    const hashed = await hashPassword(newPassword);
    
    // Update DB
    await c.env.REY_DB.prepare(
      "UPDATE auth_users SET password_hash = ?, updated_at = ? WHERE username = ?"
    ).bind(hashed, new Date().toISOString(), username).run();

    // Revoke all existing sessions
    await c.env.REY_DB.prepare(
      "UPDATE sessions SET is_valid = 0 WHERE user_id = (SELECT id FROM auth_users WHERE username = ?)"
    ).bind(username).run();

    // Clear session cache: since KV doesn't support wildcard deletion easily, we let them fail-over to DB check
    await logEvent(c.env, "auth", "SECURITY", "password_reset", `Password successfully reset for user: ${username}`);

    return createResponse(c, true, 200, "Password updated successfully. Please log in again.");
  } catch (err: any) {
    return createResponse(c, false, 500, "Password reset error", { errors: [err.message] });
  }
});

export default authRouter;
