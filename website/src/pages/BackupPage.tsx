import React, { useState, useEffect, useRef } from "react";
import {
  Shield, KeyRound, HardDrive, Trash2, Download, Upload,
  RefreshCw, CheckCircle, AlertTriangle, Play
} from "lucide-react";
import { API_BASE } from "../config";

interface BackupRecord {
  id: string;
  name: string;
  timestamp: string;
  size_bytes: number;
  file_path_drive: string | null;
}

export const BackupPage: React.FC = () => {
  const [backups, setBackups] = useState<BackupRecord[]>([]);
  const [isProcessing, setIsProcessing] = useState(false);
  const [backupName, setBackupName] = useState("");
  const [statusMsg, setStatusMsg] = useState("");
  const [isSuccess, setIsSuccess] = useState<boolean | null>(null);
  
  // Biometric / security config toggles
  const [fingerprintLogin, setFingerprintLogin] = useState(true);
  const [requirePassword, setRequirePassword] = useState(true);
  const [autoLockDuration, setAutoLockDuration] = useState("30s");

  const fileInputRef = useRef<HTMLInputElement>(null);

  const fetchBackups = async () => {
    const token = localStorage.getItem("token");
    try {
      const res = await fetch(`${API_BASE}/backup`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      const data = await res.json();
      if (data.success) {
        setBackups(data.data);
      }
    } catch (e) {
      console.error("Failed to load backups list", e);
    }
  };

  useEffect(() => {
    fetchBackups();
  }, []);

  const triggerAlert = (msg: string, success = true) => {
    setStatusMsg(msg);
    setIsSuccess(success);
    setTimeout(() => {
      setStatusMsg("");
      setIsSuccess(null);
    }, 4000);
  };

  const handleCreateBackup = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!backupName.trim()) return;
    setIsProcessing(true);
    const token = localStorage.getItem("token");
    try {
      const res = await fetch(`${API_BASE}/backup/create`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ name: backupName })
      });
      const data = await res.json();
      if (data.success) {
        triggerAlert(`Backup '${backupName}' created successfully!`);
        setBackupName("");
        await fetchBackups();
      } else {
        triggerAlert(data.message || "Failed to create backup point", false);
      }
    } catch (e: any) {
      triggerAlert(e.message || "Network error occurred", false);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleRestoreBackup = async (id: string, name: string) => {
    if (!window.confirm(`Are you absolutely sure you want to restore the ecosystem to: ${name}? All current D1/SQLite tables will be overwritten.`)) {
      return;
    }
    setIsProcessing(true);
    const token = localStorage.getItem("token");
    try {
      const res = await fetch(`${API_BASE}/backup/restore`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ id })
      });
      const data = await res.json();
      if (data.success) {
        triggerAlert(`✓ Ecosystem successfully restored to backup: ${name}`);
      } else {
        triggerAlert(data.message || "Failed to restore state", false);
      }
    } catch (e: any) {
      triggerAlert(e.message || "Failed to contact restore gateway", false);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleDeleteBackup = async (id: string) => {
    if (!window.confirm("Permanently delete this restore point?")) return;
    const token = localStorage.getItem("token");
    try {
      const res = await fetch(`${API_BASE}/backup/${id}`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${token}` }
      });
      const data = await res.json();
      if (data.success) {
        triggerAlert("Backup point deleted");
        await fetchBackups();
      }
    } catch (e: any) {
      triggerAlert(e.message || "Deletion failed", false);
    }
  };

  const handleExportBackup = async (id: string, name: string) => {
    const token = localStorage.getItem("token");
    try {
      const res = await fetch(`${API_BASE}/backup/export/${id}`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      const data = await res.json();
      if (data.success) {
        // Trigger local json download
        const blob = new Blob([data.data.payload], { type: "application/json" });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = `${name.replace(/\s+/g, "_")}_backup.json`;
        link.click();
        triggerAlert("Backup export download completed!");
      }
    } catch (e: any) {
      triggerAlert("Export failed", false);
    }
  };

  const handleImportFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = async (event) => {
      const payload = event.target?.result as string;
      const token = localStorage.getItem("token");
      try {
        const res = await fetch(`${API_BASE}/backup/import`, {
          method: "POST",
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json"
          },
          body: JSON.stringify({ name: `Imported ${file.name}`, payload })
        });
        const data = await res.json();
        if (data.success) {
          triggerAlert("External backup payload imported successfully!");
          await fetchBackups();
        } else {
          triggerAlert(data.message || "Failed to parse import", false);
        }
      } catch (e: any) {
        triggerAlert("Import request failed", false);
      }
    };
    reader.readAsText(file);
  };

  return (
    <div>
      {/* Page Header */}
      <div style={{ marginBottom: "32px" }}>
        <h2 style={{ fontSize: "28px", fontWeight: 800, fontFamily: "var(--font-display)" }}>Security & Backups</h2>
        <p style={{ color: "var(--text-secondary)", fontSize: "14px", marginTop: "4px" }}>
          Generate database restore points, manage biometric locks, handle exports, and secure your Private OML ecosystem.
        </p>
      </div>

      {/* Alert banner */}
      {statusMsg && (
        <div className="glass-panel" style={{
          marginBottom: "24px", padding: "14px 20px", display: "flex", alignItems: "center", gap: "10px",
          borderLeft: `3px solid ${isSuccess ? "#00E676" : "#FF5252"}`,
          color: isSuccess ? "#00E676" : "#FF5252", fontWeight: 600
        }}>
          {isSuccess ? <CheckCircle size={18} /> : <AlertTriangle size={18} />}
          {statusMsg}
        </div>
      )}

      <div style={{ display: "grid", gridTemplateColumns: "1fr 360px", gap: "24px", alignItems: "start" }} className="location-grid-layout">
        
        {/* Left Side: Backup Point Creator + List */}
        <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
          
          {/* Create Backup */}
          <div className="glass-panel" style={{ padding: "24px" }}>
            <h3 style={{ fontSize: "18px", fontWeight: 700, marginBottom: "16px", display: "flex", alignItems: "center", gap: "8px" }}>
              <HardDrive size={18} color="var(--primary)" />
              Create Restore Point
            </h3>
            <form onSubmit={handleCreateBackup} style={{ display: "flex", gap: "12px" }}>
              <input
                type="text"
                placeholder="Name your backup point (e.g. Pre-upgrade restore)"
                className="form-input"
                style={{ flex: 1 }}
                value={backupName}
                onChange={e => setBackupName(e.target.value)}
                disabled={isProcessing}
                required
              />
              <button type="submit" className="btn-primary" disabled={isProcessing} style={{ padding: "10px 20px" }}>
                {isProcessing ? <RefreshCw className="animate-spin" size={16} /> : "Save Point"}
              </button>
            </form>
          </div>

          {/* Backup List */}
          <div className="glass-panel" style={{ padding: "24px" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "20px" }}>
              <h3 style={{ fontSize: "18px", fontWeight: 700 }}>Restore Points Directory</h3>
              <div>
                <input
                  type="file"
                  accept=".json"
                  ref={fileInputRef}
                  onChange={handleImportFile}
                  style={{ display: "none" }}
                />
                <button
                  onClick={() => fileInputRef.current?.click()}
                  className="btn-secondary"
                  style={{ display: "flex", alignItems: "center", gap: "6px", fontSize: "12px" }}
                >
                  <Upload size={13} /> Import Backup File
                </button>
              </div>
            </div>

            <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
              {backups.length === 0 ? (
                <div style={{ padding: "40px 20px", textAlign: "center", color: "var(--text-secondary)", fontSize: "14px" }}>
                  No backup points recorded yet.
                </div>
              ) : (
                backups.map(bk => (
                  <div key={bk.id} className="glass-panel" style={{
                    display: "flex", justifyContent: "space-between", alignItems: "center",
                    padding: "16px", background: "rgba(255,255,255,0.01)", border: "1px solid rgba(255,255,255,0.03)"
                  }}>
                    <div>
                      <p style={{ fontSize: "14px", fontWeight: 700, margin: 0 }}>{bk.name}</p>
                      <span style={{ fontSize: "11px", color: "var(--text-secondary)" }}>
                        {bk.id} • {new Date(bk.timestamp).toLocaleString()} • {(bk.size_bytes / 1024).toFixed(1)} KB
                      </span>
                    </div>

                    <div style={{ display: "flex", gap: "6px" }}>
                      <button
                        onClick={() => handleRestoreBackup(bk.id, bk.name)}
                        className="btn-secondary"
                        style={{ padding: "6px 12px", fontSize: "11px", display: "flex", alignItems: "center", gap: "4px" }}
                        disabled={isProcessing}
                      >
                        <Play size={11} /> Restore
                      </button>
                      <button
                        onClick={() => handleExportBackup(bk.id, bk.name)}
                        className="btn-secondary"
                        style={{ padding: "6px" }}
                        title="Export JSON"
                      >
                        <Download size={13} />
                      </button>
                      <button
                        onClick={() => handleDeleteBackup(bk.id)}
                        className="btn-secondary"
                        style={{ padding: "6px", color: "#FF5252" }}
                        title="Delete"
                      >
                        <Trash2 size={13} />
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>

        {/* Right Side: Security Config & Status */}
        <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
          
          {/* Security Config */}
          <div className="glass-panel" style={{ padding: "24px" }}>
            <h3 style={{ fontSize: "16px", fontWeight: 700, marginBottom: "20px", display: "flex", alignItems: "center", gap: "8px" }}>
              <Shield size={18} color="var(--secondary)" />
              APK Security Parameters
            </h3>
            
            <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <div>
                  <span style={{ fontSize: "13px", fontWeight: 600, display: "block" }}>Biometric Fingerprint</span>
                  <span style={{ fontSize: "11px", color: "var(--text-secondary)" }}>Verify fingerprint before opening</span>
                </div>
                <input
                  type="checkbox"
                  checked={fingerprintLogin}
                  onChange={e => setFingerprintLogin(e.target.checked)}
                  style={{ width: "16px", height: "16px" }}
                />
              </div>

              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <div>
                  <span style={{ fontSize: "13px", fontWeight: 600, display: "block" }}>Require Passcode</span>
                  <span style={{ fontSize: "11px", color: "var(--text-secondary)" }}>Require passcode fallback always</span>
                </div>
                <input
                  type="checkbox"
                  checked={requirePassword}
                  onChange={e => setRequirePassword(e.target.checked)}
                  style={{ width: "16px", height: "16px" }}
                />
              </div>

              <div>
                <label style={{ fontSize: "12px", color: "var(--text-secondary)", display: "block", marginBottom: "6px" }}>Auto-Lock Timeout</label>
                <select
                  value={autoLockDuration}
                  onChange={e => setAutoLockDuration(e.target.value)}
                  style={{
                    width: "100%", padding: "8px", background: "rgba(255,255,255,0.03)",
                    border: "1px solid var(--border-glass)", borderRadius: "var(--radius-md)",
                    color: "#fff", fontSize: "13px"
                  }}
                >
                  <option value="30s">30 Seconds</option>
                  <option value="1m">1 Minute</option>
                  <option value="5m">5 Minutes</option>
                  <option value="10m">10 Minutes</option>
                  <option value="immediately">Lock Immediately</option>
                </select>
              </div>
            </div>
          </div>

          {/* Drive & Encryption Info */}
          <div className="glass-panel" style={{ padding: "24px" }}>
            <h3 style={{ fontSize: "16px", fontWeight: 700, marginBottom: "16px", display: "flex", alignItems: "center", gap: "8px" }}>
              <KeyRound size={18} color="var(--primary)" />
              System Encryption
            </h3>
            <div style={{ display: "flex", flexDirection: "column", gap: "12px", fontSize: "12px" }}>
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <span style={{ color: "var(--text-secondary)" }}>Keys Cache Store</span>
                <span style={{ fontWeight: 600, color: "#00E676" }}>Android Keystore</span>
              </div>
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <span style={{ color: "var(--text-secondary)" }}>Passcode Hash</span>
                <span style={{ fontWeight: 600 }}>SHA-256 (Salted)</span>
              </div>
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <span style={{ color: "var(--text-secondary)" }}>Drive Folder Path</span>
                <span style={{ fontWeight: 600 }}>Private OML/Backups/</span>
              </div>
            </div>
          </div>

        </div>

      </div>
    </div>
  );
};

export default BackupPage;
