import React, { useState, useEffect, useCallback } from "react";
import {
  Share2, Copy, Check, ExternalLink, Eye, EyeOff,
  AlertTriangle, RefreshCw
} from "lucide-react";
import { API_BASE } from "../config";

// ──────────────────────────────────────────────────────────
//  TYPES
// ──────────────────────────────────────────────────────────
interface SocialAccount {
  id: string;
  platform: string;
  enabled: boolean;
  isHidden: boolean;
  username: string;
  email: string;
  phone: string;
  password: string;
  link: string;
  notes: string;
}

// ──────────────────────────────────────────────────────────
//  PLATFORM META
// ──────────────────────────────────────────────────────────
const PLATFORM_META: Record<string, { color: string; textColor: string; emoji: string }> = {
  Instagram: { color: "linear-gradient(135deg,#f09433,#e6683c,#dc2743,#cc2366,#bc1888)", textColor: "#fff", emoji: "📸" },
  Facebook:  { color: "#1877f2",  textColor: "#fff", emoji: "👥" },
  Messenger: { color: "linear-gradient(135deg,#006aff,#00e4ff)", textColor: "#fff", emoji: "💬" },
  Snapchat:  { color: "#fffc00",  textColor: "#000", emoji: "👻" },
  Telegram:  { color: "#2ca5e0",  textColor: "#fff", emoji: "✈️" },
  TikTok:    { color: "#010101",  textColor: "#fff", emoji: "🎵" },
  Discord:   { color: "#5865f2",  textColor: "#fff", emoji: "🎮" },
  LinkedIn:  { color: "#0a66c2",  textColor: "#fff", emoji: "💼" },
  Threads:   { color: "#1a1a1a",  textColor: "#fff", emoji: "🧵" },
  YouTube:   { color: "#ff0000",  textColor: "#fff", emoji: "▶️" },
};

function getPlatformMeta(name: string) {
  return PLATFORM_META[name] ?? { color: "var(--primary-gradient)", textColor: "#fff", emoji: "📱" };
}

// ──────────────────────────────────────────────────────────
//  SMALL HELPERS
// ──────────────────────────────────────────────────────────
function useCopy() {
  const [copied, setCopied] = useState<string | null>(null);
  const copy = useCallback((text: string, key: string) => {
    navigator.clipboard.writeText(text);
    setCopied(key);
    setTimeout(() => setCopied(null), 2000);
  }, []);
  return { copied, copy };
}

// ──────────────────────────────────────────────────────────
//  COMPONENT
// ──────────────────────────────────────────────────────────
export const SocialPage: React.FC = () => {
  const [accounts, setAccounts]     = useState<SocialAccount[]>([]);
  const [intro, setIntro]           = useState<string>("");
  const [isLoading, setIsLoading]   = useState(true);
  const [hasError, setHasError]     = useState(false);
  const [revealAll, setRevealAll]   = useState(false);
  const [revealMap, setRevealMap]   = useState<Record<string, boolean>>({});
  const [warned, setWarned]         = useState(false);
  const { copied, copy }            = useCopy();

  // ── Fetch ──────────────────────────────────────────────
  const fetchData = async () => {
    setIsLoading(true);
    setHasError(false);
    const token = localStorage.getItem("token");
    try {
      const res  = await fetch(`${API_BASE}/ai/config`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const data = await res.json();
      if (data.success && Array.isArray(data.data)) {
        const platformsRaw = data.data.find((c: any) => c.config_key === "social_media_platforms");
        const introRaw     = data.data.find((c: any) => c.config_key === "social_media_intro");

        if (platformsRaw) {
          try {
            const parsed: SocialAccount[] = JSON.parse(platformsRaw.config_value);
            // Only show enabled and not hidden with at least a username
            setAccounts(parsed.filter(a => a.enabled && !a.isHidden && (a.username || a.email || a.phone)));
          } catch { /* ignore */ }
        }
        if (introRaw) setIntro(introRaw.config_value ?? "");
      }
    } catch {
      setHasError(true);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => { fetchData(); }, []);

  // ── Reveal password with warning ──────────────────────
  const handleRevealPassword = (id: string) => {
    if (!warned) {
      const ok = window.confirm(
        "⚠️ Security Warning\n\nYou are about to reveal a password on this page.\nMake sure no one else can see your screen.\n\nProceed?"
      );
      if (!ok) return;
      setWarned(true);
    }
    setRevealMap(m => ({ ...m, [id]: !m[id] }));
  };

  const handleRevealAll = () => {
    if (!warned) {
      const ok = window.confirm(
        "⚠️ Security Warning\n\nYou are about to reveal ALL passwords on this page.\nMake sure you are in a private environment.\n\nProceed?"
      );
      if (!ok) return;
      setWarned(true);
    }
    setRevealAll(v => !v);
  };

  const getDisplayIdentifier = (a: SocialAccount) =>
    a.username || a.email || a.phone;

  // ── Render helpers ─────────────────────────────────────
  const shimmer = (
    <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill,minmax(300px,1fr))", gap: "24px" }}>
      {[1,2,3,4].map(n => (
        <div key={n} className="glass-panel shimmer-placeholder" style={{ height: "280px", borderRadius: "var(--radius-lg)" }} />
      ))}
    </div>
  );

  return (
    <div>
      {/* ── PAGE HEADER ───────────────────────────────── */}
      <div style={{ marginBottom: "40px" }}>
        <div style={{ display: "flex", alignItems: "center", gap: "12px", marginBottom: "8px" }}>
          <Share2 size={28} color="var(--primary)" />
          <h1 style={{ fontSize: "32px", fontWeight: 800, fontFamily: "var(--font-display)", margin: 0 }}>
            Social Accounts
          </h1>
        </div>
        <p style={{ color: "var(--text-secondary)", fontSize: "15px", maxWidth: "560px" }}>
          Contact information and profiles shared intentionally by the owner.
        </p>

        {/* Reveal-all toggle */}
        {accounts.some(a => !!a.password) && (
          <button
            onClick={handleRevealAll}
            style={{
              marginTop: "16px",
              display: "flex", alignItems: "center", gap: "6px",
              background: revealAll ? "rgba(255,82,82,0.12)" : "rgba(255,255,255,0.04)",
              border: `1px solid ${revealAll ? "rgba(255,82,82,0.4)" : "var(--border-glass)"}`,
              color: revealAll ? "#FF5252" : "var(--text-secondary)",
              borderRadius: "var(--radius-md)", padding: "8px 14px",
              cursor: "pointer", fontSize: "13px", fontWeight: 600,
              transition: "all 0.2s"
            }}
          >
            {revealAll ? <EyeOff size={15}/> : <Eye size={15}/>}
            {revealAll ? "Hide all passwords" : "Reveal all passwords"}
          </button>
        )}
      </div>

      {/* ── OWNER INTRO ─────────────────────────────────── */}
      {intro && (
        <div
          className="glass-panel"
          style={{
            marginBottom: "36px",
            padding: "28px 32px",
            borderLeft: "4px solid var(--primary)",
            background: "linear-gradient(135deg,rgba(155,93,229,0.06),rgba(0,255,212,0.03))",
          }}
        >
          <p style={{ color: "var(--text-muted)", fontSize: "11px", letterSpacing: "0.1em", textTransform: "uppercase", marginBottom: "12px", fontWeight: 700 }}>
            A note from the owner
          </p>
          {/* Render basic HTML safely */}
          <div
            style={{ color: "var(--text-primary)", fontSize: "15px", lineHeight: 1.8 }}
            dangerouslySetInnerHTML={{ __html: intro }}
          />
        </div>
      )}

      {/* ── LOADING / ERROR / EMPTY ──────────────────────── */}
      {isLoading ? (
        shimmer
      ) : hasError ? (
        <div className="glass-panel" style={{ textAlign: "center", padding: "80px 24px" }}>
          <AlertTriangle size={40} color="var(--text-muted)" style={{ marginBottom: "16px" }} />
          <h3 style={{ fontSize: "18px", marginBottom: "8px", fontWeight: 600 }}>Could not load accounts</h3>
          <p style={{ color: "var(--text-secondary)", fontSize: "14px", marginBottom: "20px" }}>
            The backend may be temporarily unavailable.
          </p>
          <button className="btn-secondary" onClick={fetchData} style={{ display: "inline-flex", alignItems: "center", gap: "6px" }}>
            <RefreshCw size={14} /> Retry
          </button>
        </div>
      ) : accounts.length === 0 ? (
        <div className="glass-panel" style={{ textAlign: "center", padding: "80px 24px" }}>
          <Share2 size={48} color="var(--text-muted)" style={{ marginBottom: "16px" }} />
          <h3 style={{ fontSize: "18px", marginBottom: "8px", fontWeight: 600 }}>No Accounts Shared</h3>
          <p style={{ color: "var(--text-secondary)", fontSize: "14px", maxWidth: "340px", margin: "0 auto" }}>
            The owner hasn't made any social accounts visible yet.
          </p>
        </div>
      ) : (
        /* ── ACCOUNTS GRID ────────────────────────────── */
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill,minmax(300px,1fr))", gap: "24px" }}>
          {accounts.map(account => {
            const meta        = getPlatformMeta(account.platform);
            const identifier  = getDisplayIdentifier(account);
            const showPw      = revealAll || !!revealMap[account.id];

            return (
              <div
                key={account.id}
                className="glass-panel"
                style={{
                  display: "flex", flexDirection: "column", gap: "0",
                  border: "1px solid var(--border-glass)",
                  overflow: "hidden", position: "relative",
                  transition: "transform 0.25s, box-shadow 0.25s",
                  cursor: "default"
                }}
                onMouseEnter={e => {
                  (e.currentTarget as HTMLDivElement).style.transform = "translateY(-4px)";
                  (e.currentTarget as HTMLDivElement).style.boxShadow = "0 16px 48px rgba(0,0,0,0.4)";
                }}
                onMouseLeave={e => {
                  (e.currentTarget as HTMLDivElement).style.transform = "translateY(0)";
                  (e.currentTarget as HTMLDivElement).style.boxShadow = "";
                }}
              >
                {/* Accent strip */}
                <div style={{ height: "4px", background: meta.color }} />

                <div style={{ padding: "24px", display: "flex", flexDirection: "column", gap: "20px", flex: 1 }}>
                  {/* Platform branding */}
                  <div style={{ display: "flex", alignItems: "center", gap: "14px" }}>
                    <div style={{
                      width: "48px", height: "48px", borderRadius: "12px",
                      background: meta.color, flexShrink: 0,
                      display: "flex", alignItems: "center", justifyContent: "center",
                      fontSize: "22px"
                    }}>
                      {account.platform.charAt(0)}
                    </div>
                    <div>
                      <h3 style={{ fontSize: "18px", fontWeight: 700, fontFamily: "var(--font-display)", margin: 0, color: "var(--text-primary)" }}>
                        {account.platform}
                      </h3>
                      {account.notes && (
                        <p style={{ fontSize: "12px", color: "var(--text-secondary)", margin: "2px 0 0" }}>
                          {account.notes}
                        </p>
                      )}
                    </div>
                    <span style={{
                      marginLeft: "auto", fontSize: "10px", fontWeight: 700, letterSpacing: "0.08em",
                      color: "#00E676", background: "rgba(0,230,118,0.1)",
                      padding: "3px 8px", borderRadius: "20px", textTransform: "uppercase"
                    }}>
                      Active
                    </span>
                  </div>

                  {/* Credential rows */}
                  <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
                    {/* Username / Email / Phone */}
                    {identifier && (
                      <CredentialRow
                        label="Username"
                        value={identifier}
                        copyKey={`u-${account.id}`}
                        copied={copied}
                        onCopy={copy}
                      />
                    )}

                    {/* Password */}
                    {account.password && (
                      <div style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                          <span style={{ fontSize: "10px", color: "var(--text-muted)", textTransform: "uppercase", letterSpacing: "0.08em", fontWeight: 700 }}>
                            Password
                          </span>
                          <button
                            onClick={() => handleRevealPassword(account.id)}
                            style={{
                              background: "none", border: "none", cursor: "pointer",
                              color: "var(--text-secondary)", display: "flex", alignItems: "center",
                              gap: "4px", fontSize: "11px", padding: "2px 6px",
                              borderRadius: "4px",
                            }}
                          >
                            {showPw ? <EyeOff size={12}/> : <Eye size={12}/>}
                            {showPw ? "Hide" : "Reveal"}
                          </button>
                        </div>
                        <div style={{
                          background: "rgba(255,255,255,0.03)",
                          border: "1px solid var(--border-glass)",
                          borderRadius: "var(--radius-md)",
                          padding: "9px 12px",
                          display: "flex", alignItems: "center", justifyContent: "space-between", gap: "8px"
                        }}>
                          <span style={{
                            fontSize: "14px", fontFamily: "monospace", fontWeight: 600,
                            overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap",
                            flex: 1,
                            letterSpacing: showPw ? "normal" : "0.15em"
                          }}>
                            {showPw ? account.password : "••••••••••"}
                          </span>
                          {showPw && (
                            <CopyButton
                              copyKey={`p-${account.id}`}
                              copied={copied}
                              onCopy={() => copy(account.password, `p-${account.id}`)}
                            />
                          )}
                        </div>
                      </div>
                    )}
                  </div>

                  {/* Open link */}
                  {account.link && (
                    <a
                      href={account.link}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="btn-secondary"
                      style={{ justifyContent: "center", padding: "10px", fontSize: "13px", gap: "6px", textDecoration: "none" }}
                    >
                      <ExternalLink size={14} />
                      Open {account.platform}
                    </a>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

// ──────────────────────────────────────────────────────────
//  SUB-COMPONENTS
// ──────────────────────────────────────────────────────────

interface CredentialRowProps {
  label: string;
  value: string;
  copyKey: string;
  copied: string | null;
  onCopy: (text: string, key: string) => void;
}

const CredentialRow: React.FC<CredentialRowProps> = ({ label, value, copyKey, copied, onCopy }) => (
  <div>
    <span style={{ fontSize: "10px", color: "var(--text-muted)", textTransform: "uppercase", letterSpacing: "0.08em", fontWeight: 700, display: "block", marginBottom: "6px" }}>
      {label}
    </span>
    <div style={{
      background: "rgba(255,255,255,0.03)",
      border: "1px solid var(--border-glass)",
      borderRadius: "var(--radius-md)",
      padding: "9px 12px",
      display: "flex", alignItems: "center", justifyContent: "space-between", gap: "8px"
    }}>
      <span style={{
        fontSize: "14px", fontWeight: 500, fontFamily: "monospace",
        overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap", flex: 1
      }}>
        {value}
      </span>
      <CopyButton copyKey={copyKey} copied={copied} onCopy={() => onCopy(value, copyKey)} />
    </div>
  </div>
);

interface CopyButtonProps {
  copyKey: string;
  copied: string | null;
  onCopy: () => void;
}

const CopyButton: React.FC<CopyButtonProps> = ({ copyKey, copied, onCopy }) => {
  const isCopied = copied === copyKey;
  return (
    <button
      onClick={onCopy}
      title="Copy to clipboard"
      style={{
        background: isCopied ? "rgba(0,230,118,0.1)" : "none",
        border: "none",
        color: isCopied ? "#00E676" : "var(--text-secondary)",
        cursor: "pointer",
        display: "flex", alignItems: "center", padding: "6px",
        borderRadius: "4px", transition: "all 0.2s", flexShrink: 0
      }}
      onMouseEnter={e => { if (!isCopied) (e.currentTarget as HTMLButtonElement).style.background = "rgba(255,255,255,0.06)"; }}
      onMouseLeave={e => { if (!isCopied) (e.currentTarget as HTMLButtonElement).style.background = "none"; }}
    >
      {isCopied ? <Check size={15}/> : <Copy size={15}/>}
    </button>
  );
};

export default SocialPage;
