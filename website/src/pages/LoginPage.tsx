import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { KeyRound, User, Loader2, ArrowLeft, Send, ShieldAlert } from "lucide-react";

export const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  
  // Forgot password flow states
  const [flowStep, setFlowStep] = useState<"login" | "forgot" | "verify" | "reset">("login");
  const [recoveryCode, setRecoveryCode] = useState("");
  const [resetToken, setResetToken] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [infoMessage, setInfoMessage] = useState("");

  const API_BASE = "http://localhost:8787/api/v1";

  // Login Submit
  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username || !password) return;
    setIsLoading(true);
    setErrorMessage("");

    try {
      const response = await fetch(`${API_BASE}/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password })
      });
      const res = await response.json();
      
      if (!response.ok || !res.success) {
        throw new Error(res.message || "Invalid credentials");
      }
      
      // Store token
      localStorage.setItem("token", res.data.token);
      localStorage.setItem("username", res.data.username);
      navigate("/");
    } catch (err: any) {
      setErrorMessage(err.message);
    } finally {
      setIsLoading(false);
    }
  };

  // Forgot password request code
  const handleForgotPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username) {
      setErrorMessage("Please enter your username first.");
      return;
    }
    setIsLoading(true);
    setErrorMessage("");
    setInfoMessage("");

    try {
      const response = await fetch(`${API_BASE}/auth/forgot-password`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username })
      });
      const res = await response.json();
      
      if (!response.ok || !res.success) {
        throw new Error(res.message || "Failed to trigger recovery");
      }
      
      setFlowStep("verify");
      setInfoMessage("Verification code has been requested. It will be sent via SMS through the owner's linked device.");
    } catch (err: any) {
      setErrorMessage(err.message);
    } finally {
      setIsLoading(false);
    }
  };

  // Verify OTP recovery code
  const handleVerifyCode = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!recoveryCode) return;
    setIsLoading(true);
    setErrorMessage("");
    setInfoMessage("");

    try {
      const response = await fetch(`${API_BASE}/auth/verify-recovery-code`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, code: recoveryCode })
      });
      const res = await response.json();
      
      if (!response.ok || !res.success) {
        throw new Error(res.message || "Verification failed");
      }
      
      setResetToken(res.data.resetToken);
      setFlowStep("reset");
    } catch (err: any) {
      setErrorMessage(err.message);
    } finally {
      setIsLoading(false);
    }
  };

  // Reset Password Submit
  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newPassword || newPassword !== confirmPassword) {
      setErrorMessage("Passwords do not match");
      return;
    }
    setIsLoading(true);
    setErrorMessage("");

    try {
      const response = await fetch(`${API_BASE}/auth/reset-password`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, resetToken, newPassword })
      });
      const res = await response.json();
      
      if (!response.ok || !res.success) {
        throw new Error(res.message || "Password reset failed");
      }
      
      setFlowStep("login");
      setInfoMessage("Password reset successfully. Please log in with your new credentials.");
      setPassword("");
    } catch (err: any) {
      setErrorMessage(err.message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div style={{
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      minHeight: "100vh",
      background: "var(--bg-base)",
      padding: "20px"
    }}>
      <div className="glass-panel" style={{
        width: "100%",
        maxWidth: "440px",
        padding: "40px",
        borderRadius: "24px"
      }}>
        {/* Brand Header */}
        <div style={{ textAlign: "center", marginBottom: "32px" }}>
          <div style={{
            width: "60px",
            height: "60px",
            background: "var(--primary-gradient)",
            borderRadius: "50%",
            margin: "0 auto 16px auto",
            boxShadow: "0 0 30px var(--primary-glow)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center"
          }}>
            <KeyRound size={28} color="#fff" />
          </div>
          <h1 style={{ fontSize: "28px", fontWeight: 800, fontFamily: "var(--font-display)", letterSpacing: "-0.03em" }}>ReY</h1>
          <p style={{ fontSize: "14px", color: "var(--text-secondary)", marginTop: "4px" }}>Memory Vault</p>
        </div>

        {/* Message banners */}
        {errorMessage && (
          <div style={{
            background: "rgba(239, 68, 68, 0.1)",
            border: "1px solid rgba(239, 68, 68, 0.2)",
            color: "#f87171",
            padding: "12px 16px",
            borderRadius: "var(--radius-md)",
            fontSize: "14px",
            marginBottom: "24px",
            display: "flex",
            alignItems: "center",
            gap: "8px"
          }}>
            <ShieldAlert size={18} />
            {errorMessage}
          </div>
        )}

        {infoMessage && (
          <div style={{
            background: "rgba(16, 185, 129, 0.1)",
            border: "1px solid rgba(16, 185, 129, 0.2)",
            color: "#34d399",
            padding: "12px 16px",
            borderRadius: "var(--radius-md)",
            fontSize: "14px",
            marginBottom: "24px"
          }}>
            {infoMessage}
          </div>
        )}

        {/* LOGIN STEP */}
        {flowStep === "login" && (
          <form onSubmit={handleLogin} style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
            <div>
              <label style={{ display: "block", fontSize: "14px", color: "var(--text-secondary)", marginBottom: "8px", fontWeight: 500 }}>Username</label>
              <div style={{ position: "relative" }}>
                <input
                  type="text"
                  placeholder="Enter username"
                  className="form-input"
                  style={{ paddingLeft: "44px" }}
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  required
                />
                <User size={18} style={{ position: "absolute", left: "16px", top: "50%", transform: "translateY(-50%)", color: "var(--text-muted)" }} />
              </div>
            </div>

            <div>
              <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "8px" }}>
                <label style={{ fontSize: "14px", color: "var(--text-secondary)", fontWeight: 500 }}>Password</label>
                <button
                  type="button"
                  onClick={() => setFlowStep("forgot")}
                  style={{ background: "none", border: "none", color: "var(--primary)", fontSize: "13px", cursor: "pointer", fontWeight: 500 }}
                >
                  Forgot?
                </button>
              </div>
              <div style={{ position: "relative" }}>
                <input
                  type="password"
                  placeholder="Enter password"
                  className="form-input"
                  style={{ paddingLeft: "44px" }}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
                <KeyRound size={18} style={{ position: "absolute", left: "16px", top: "50%", transform: "translateY(-50%)", color: "var(--text-muted)" }} />
              </div>
            </div>

            <button type="submit" className="btn-primary" style={{ width: "100%", justifyContent: "center", marginTop: "8px" }} disabled={isLoading}>
              {isLoading ? <Loader2 className="animate-spin" size={20} /> : "Sign In"}
            </button>
          </form>
        )}

        {/* FORGOT PASSWORD: STEP 1 (REQUEST OTP) */}
        {flowStep === "forgot" && (
          <form onSubmit={handleForgotPassword} style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
            <p style={{ fontSize: "14px", color: "var(--text-secondary)", lineHeight: "1.6" }}>
              Enter your username to request a password reset verification code.
            </p>
            <div>
              <label style={{ display: "block", fontSize: "14px", color: "var(--text-secondary)", marginBottom: "8px", fontWeight: 500 }}>Username</label>
              <input
                type="text"
                placeholder="Enter username"
                className="form-input"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
              />
            </div>
            <div style={{ display: "flex", gap: "12px", marginTop: "8px" }}>
              <button type="button" className="btn-secondary" style={{ flex: 1, justifyContent: "center" }} onClick={() => setFlowStep("login")}>
                <ArrowLeft size={16} /> Back
              </button>
              <button type="submit" className="btn-primary" style={{ flex: 1, justifyContent: "center" }} disabled={isLoading}>
                {isLoading ? <Loader2 className="animate-spin" size={16} /> : <><Send size={16} /> Send</>}
              </button>
            </div>
          </form>
        )}

        {/* FORGOT PASSWORD: STEP 2 (VERIFY OTP) */}
        {flowStep === "verify" && (
          <form onSubmit={handleVerifyCode} style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
            <p style={{ fontSize: "14px", color: "var(--text-secondary)", lineHeight: "1.6" }}>
              Enter the 6-digit verification code sent to the owner's mobile device via SMS.
            </p>
            <div>
              <label style={{ display: "block", fontSize: "14px", color: "var(--text-secondary)", marginBottom: "8px", fontWeight: 500 }}>Verification Code</label>
              <input
                type="text"
                maxLength={6}
                placeholder="000000"
                className="form-input"
                style={{ textAlign: "center", fontSize: "20px", letterSpacing: "8px", paddingLeft: "24px" }}
                value={recoveryCode}
                onChange={(e) => setRecoveryCode(e.target.value.replace(/\D/g, ""))}
                required
              />
            </div>
            <div style={{ display: "flex", gap: "12px", marginTop: "8px" }}>
              <button type="button" className="btn-secondary" style={{ flex: 1, justifyContent: "center" }} onClick={() => setFlowStep("forgot")}>
                <ArrowLeft size={16} /> Back
              </button>
              <button type="submit" className="btn-primary" style={{ flex: 1, justifyContent: "center" }} disabled={isLoading}>
                {isLoading ? <Loader2 className="animate-spin" size={16} /> : "Verify Code"}
              </button>
            </div>
          </form>
        )}

        {/* FORGOT PASSWORD: STEP 3 (PASSWORD RESET) */}
        {flowStep === "reset" && (
          <form onSubmit={handleResetPassword} style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
            <p style={{ fontSize: "14px", color: "var(--text-secondary)", lineHeight: "1.6" }}>
              Define a new secure password for your ReY viewer profile.
            </p>
            <div>
              <label style={{ display: "block", fontSize: "14px", color: "var(--text-secondary)", marginBottom: "8px", fontWeight: 500 }}>New Password</label>
              <input
                type="password"
                placeholder="Enter new password"
                className="form-input"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                required
              />
            </div>
            <div>
              <label style={{ display: "block", fontSize: "14px", color: "var(--text-secondary)", marginBottom: "8px", fontWeight: 500 }}>Confirm Password</label>
              <input
                type="password"
                placeholder="Confirm new password"
                className="form-input"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
              />
            </div>
            <button type="submit" className="btn-primary" style={{ width: "100%", justifyContent: "center", marginTop: "8px" }} disabled={isLoading}>
              {isLoading ? <Loader2 className="animate-spin" size={20} /> : "Change Password"}
            </button>
          </form>
        )}
      </div>
    </div>
  );
};
export default LoginPage;
