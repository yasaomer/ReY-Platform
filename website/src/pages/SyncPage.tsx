import React, { useState, useEffect, useRef } from "react";
import {
  RefreshCw, Activity, Play, Pause,
  Trash2, Search, Cpu, Database, HardDrive, Wifi, ShieldAlert, Clock
} from "lucide-react";
import { API_BASE } from "../config";

interface SyncTask {
  task_id: string;
  task_type: string;
  priority: number;
  status: string;
  creation_time: string;
  retry_count: number;
  target_module: string;
  completion_time: string | null;
  error_info: string | null;
}

interface LogEntry {
  id: number;
  timestamp: string;
  module: string;
  severity: string;
  action: string;
  message: string;
}

export const SyncPage: React.FC = () => {
  const [tasks, setTasks] = useState<SyncTask[]>([]);
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [paused, setPaused] = useState(false);
  const [latency, setLatency] = useState<number | null>(null);
  const [isActioning, setIsActioning] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("All");
  const [logSearch, setLogSearch] = useState("");
  const [logFilter, setLogFilter] = useState("All");

  const pollIntervalRef = useRef<any>(null);

  const fetchSyncData = async () => {
    const token = localStorage.getItem("token");
    const startTime = Date.now();
    try {
      // 1. Fetch Sync Queue
      const queueRes = await fetch(`${API_BASE}/sync/queue`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      const queueData = await queueRes.json();
      if (queueData.success) {
        setTasks(queueData.data);
      }

      // 2. Fetch System Logs
      const logsRes = await fetch(`${API_BASE}/sync/logs`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      const logsData = await logsRes.json();
      if (logsData.success) {
        setLogs(logsData.data);
      }

      // 3. Fetch Paused Config State
      const statusRes = await fetch(`${API_BASE}/sync/sharing-status`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      const statusData = await statusRes.json();
      if (statusData.success) {
        setPaused(statusData.data.paused || false);
      }

      setLatency(Date.now() - startTime);
    } catch (e) {
      console.error("Failed to load sync ecosystem metrics", e);
    }
  };

  useEffect(() => {
    fetchSyncData();
    pollIntervalRef.current = setInterval(fetchSyncData, 8000);
    return () => {
      if (pollIntervalRef.current) clearInterval(pollIntervalRef.current);
    };
  }, []);

  const handleQueueAction = async (action: string, payload?: any) => {
    setIsActioning(true);
    const token = localStorage.getItem("token");
    try {
      const res = await fetch(`${API_BASE}/sync/queue/action`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ action, ...payload })
      });
      const data = await res.json();
      if (data.success) {
        await fetchSyncData();
      }
    } catch (e) {
      console.error("Failed to perform queue action", e);
    } finally {
      setIsActioning(false);
    }
  };

  const getPriorityLabel = (priority: number) => {
    switch (priority) {
      case 1: return { label: "High", color: "#FF9800" };
      case 3: return { label: "Low", color: "#00BBF9" };
      default: return { label: "Normal", color: "var(--primary)" };
    }
  };

  const getStatusColor = (status: string) => {
    switch (status.toLowerCase()) {
      case "completed": return "#00E676";
      case "failed": return "#FF5252";
      case "running":
      case "processing": return "#00BBF9";
      case "pending": return "#FFD166";
      default: return "var(--text-secondary)";
    }
  };

  // Filter tasks
  const filteredTasks = tasks.filter(t => {
    const matchesSearch = t.task_id.toLowerCase().includes(searchQuery.toLowerCase()) ||
      t.task_type.toLowerCase().includes(searchQuery.toLowerCase()) ||
      t.target_module.toLowerCase().includes(searchQuery.toLowerCase());
    
    const matchesStatus = statusFilter === "All" || t.status.toLowerCase() === statusFilter.toLowerCase();
    
    return matchesSearch && matchesStatus;
  });

  // Filter logs
  const filteredLogs = logs.filter(l => {
    const matchesSearch = l.message.toLowerCase().includes(logSearch.toLowerCase()) ||
      l.module.toLowerCase().includes(logSearch.toLowerCase());
    
    const matchesFilter = logFilter === "All" || l.severity === logFilter || l.module === logFilter;

    return matchesSearch && matchesFilter;
  });

  // Mock Module synchronization statuses
  const modules = [
    { name: "Gallery", status: "Online", lastSync: "2 mins ago", health: "100%" },
    { name: "Last Message", status: "Online", lastSync: "5 mins ago", health: "100%" },
    { name: "AI Config", status: "Online", lastSync: "10 mins ago", health: "98%" },
    { name: "Knowledge Base", status: "Waiting", lastSync: "1 hr ago", health: "100%" },
    { name: "Social Media", status: "Online", lastSync: "3 mins ago", health: "100%" },
    { name: "Location", status: "Syncing", lastSync: "Just now", health: "100%" }
  ];

  return (
    <div>
      {/* Page Header */}
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", marginBottom: "32px" }}>
        <div>
          <h2 style={{ fontSize: "28px", fontWeight: 800, fontFamily: "var(--font-display)" }}>Sync Control Center</h2>
          <p style={{ color: "var(--text-secondary)", fontSize: "14px", marginTop: "4px" }}>
            Monitor synchronization loops, trace background queue operations and view real-time system logs.
          </p>
        </div>
        <div style={{ display: "flex", gap: "10px" }}>
          <button
            onClick={() => handleQueueAction(paused ? "resume" : "pause")}
            className="btn-secondary"
            style={{ display: "flex", alignItems: "center", gap: "6px", fontSize: "14px" }}
            disabled={isActioning}
          >
            {paused ? <Play size={15} color="#00E676" /> : <Pause size={15} color="#FF9800" />}
            {paused ? "Resume Queue" : "Pause Queue"}
          </button>
          <button
            onClick={fetchSyncData}
            className="btn-primary"
            style={{ display: "flex", alignItems: "center", gap: "6px", fontSize: "14px" }}
          >
            <RefreshCw size={15} />
            Force Refresh
          </button>
        </div>
      </div>

      {/* Connection Monitor / Stats Bar */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: "20px", marginBottom: "32px" }}>
        <div className="glass-panel" style={{ display: "flex", alignItems: "center", gap: "16px" }}>
          <div style={{ background: "rgba(0, 230, 118, 0.1)", color: "#00E676", padding: "10px", borderRadius: "10px" }}>
            <Wifi size={20} />
          </div>
          <div>
            <span style={{ fontSize: "11px", color: "var(--text-muted)", textTransform: "uppercase", letterSpacing: "0.05em", fontWeight: 700 }}>Connection</span>
            <p style={{ fontSize: "16px", fontWeight: 700, margin: "2px 0 0" }}>Excellent</p>
          </div>
        </div>

        <div className="glass-panel" style={{ display: "flex", alignItems: "center", gap: "16px" }}>
          <div style={{ background: "rgba(155, 93, 229, 0.1)", color: "var(--primary)", padding: "10px", borderRadius: "10px" }}>
            <Activity size={20} />
          </div>
          <div>
            <span style={{ fontSize: "11px", color: "var(--text-muted)", textTransform: "uppercase", letterSpacing: "0.05em", fontWeight: 700 }}>Response Time</span>
            <p style={{ fontSize: "16px", fontWeight: 700, margin: "2px 0 0" }}>{latency !== null ? `${latency} ms` : "---"}</p>
          </div>
        </div>

        <div className="glass-panel" style={{ display: "flex", alignItems: "center", gap: "16px" }}>
          <div style={{ background: "rgba(0, 187, 249, 0.1)", color: "#00bbf9", padding: "10px", borderRadius: "10px" }}>
            <Database size={20} />
          </div>
          <div>
            <span style={{ fontSize: "11px", color: "var(--text-muted)", textTransform: "uppercase", letterSpacing: "0.05em", fontWeight: 700 }}>D1 Status</span>
            <p style={{ fontSize: "16px", fontWeight: 700, margin: "2px 0 0" }}>Connected</p>
          </div>
        </div>

        <div className="glass-panel" style={{ display: "flex", alignItems: "center", gap: "16px" }}>
          <div style={{ background: "rgba(255, 82, 82, 0.1)", color: "#FF5252", padding: "10px", borderRadius: "10px" }}>
            <ShieldAlert size={20} />
          </div>
          <div>
            <span style={{ fontSize: "11px", color: "var(--text-muted)", textTransform: "uppercase", letterSpacing: "0.05em", fontWeight: 700 }}>Failed Tasks</span>
            <p style={{ fontSize: "16px", fontWeight: 700, margin: "2px 0 0" }}>{tasks.filter(t => t.status === "failed").length}</p>
          </div>
        </div>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 360px", gap: "24px", alignItems: "start", marginBottom: "32px" }} className="location-grid-layout">
        
        {/* Sync Queue Table Panel */}
        <div className="glass-panel" style={{ padding: "24px" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "20px" }}>
            <h3 style={{ fontSize: "18px", fontWeight: 700, display: "flex", alignItems: "center", gap: "8px" }}>
              <Clock size={18} color="var(--primary)" />
              Task Queue ({filteredTasks.length})
            </h3>
            <div style={{ display: "flex", gap: "8px" }}>
              <button onClick={() => handleQueueAction("retry_all")} className="btn-secondary" style={{ padding: "6px 12px", fontSize: "12px" }}>
                Retry All Failed
              </button>
              <button onClick={() => handleQueueAction("clear_completed")} className="btn-secondary" style={{ padding: "6px 12px", fontSize: "12px", color: "#FF5252" }}>
                Clear Completed
              </button>
            </div>
          </div>

          {/* Search + Filter */}
          <div style={{ display: "flex", gap: "12px", marginBottom: "16px" }}>
            <div style={{ flex: 1, position: "relative" }}>
              <Search size={14} style={{ position: "absolute", left: "12px", top: "50%", transform: "translateY(-50%)", color: "var(--text-muted)" }} />
              <input
                type="text"
                placeholder="Search tasks by ID or module..."
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
                style={{
                  width: "100%", padding: "10px 12px 10px 36px",
                  background: "rgba(255,255,255,0.03)", border: "1px solid var(--border-glass)",
                  borderRadius: "var(--radius-md)", color: "#fff", fontSize: "13px"
                }}
              />
            </div>
            <select
              value={statusFilter}
              onChange={e => setStatusFilter(e.target.value)}
              style={{
                padding: "8px 12px", background: "rgba(255,255,255,0.03)",
                border: "1px solid var(--border-glass)", borderRadius: "var(--radius-md)",
                color: "#fff", fontSize: "13px"
              }}
            >
              <option value="All">All Statuses</option>
              <option value="Pending">Pending</option>
              <option value="Running">Running</option>
              <option value="Completed">Completed</option>
              <option value="Failed">Failed</option>
            </select>
          </div>

          {/* Queue List */}
          <div style={{ overflowX: "auto" }}>
            <table style={{ width: "100%", borderCollapse: "collapse", textAlign: "left", fontSize: "13px" }}>
              <thead>
                <tr style={{ borderBottom: "1px solid var(--border-glass)", color: "var(--text-muted)" }}>
                  <th style={{ padding: "10px" }}>Task ID</th>
                  <th style={{ padding: "10px" }}>Module</th>
                  <th style={{ padding: "10px" }}>Type</th>
                  <th style={{ padding: "10px" }}>Priority</th>
                  <th style={{ padding: "10px" }}>Status</th>
                  <th style={{ padding: "10px" }}>Action</th>
                </tr>
              </thead>
              <tbody>
                {filteredTasks.length === 0 ? (
                  <tr>
                    <td colSpan={6} style={{ padding: "30px", textAlign: "center", color: "var(--text-secondary)" }}>
                      No tasks found in sync queue
                    </td>
                  </tr>
                ) : (
                  filteredTasks.map(task => {
                    const pri = getPriorityLabel(task.priority);
                    return (
                      <tr key={task.task_id} style={{ borderBottom: "1px solid rgba(255,255,255,0.03)" }}>
                        <td style={{ padding: "12px 10px", fontFamily: "monospace", color: "var(--secondary)" }}>
                          {task.task_id.substring(0, 8)}
                        </td>
                        <td style={{ padding: "12px 10px", fontWeight: 600 }}>{task.target_module}</td>
                        <td style={{ padding: "12px 10px", color: "var(--text-secondary)" }}>{task.task_type}</td>
                        <td style={{ padding: "12px 10px" }}>
                          <span style={{ color: pri.color, fontWeight: 700, fontSize: "11px" }}>{pri.label}</span>
                        </td>
                        <td style={{ padding: "12px 10px" }}>
                          <span style={{
                            color: getStatusColor(task.status),
                            background: `${getStatusColor(task.status)}12`,
                            padding: "3px 8px", borderRadius: "10px",
                            fontWeight: 700, fontSize: "11px"
                          }}>
                            {task.status.toUpperCase()}
                          </span>
                        </td>
                        <td style={{ padding: "12px 10px" }}>
                          <button
                            onClick={() => handleQueueAction("cancel", { taskId: task.task_id })}
                            style={{
                              background: "none", border: "none", cursor: "pointer",
                              color: "#FF5252", padding: "4px"
                            }}
                            title="Cancel Task"
                          >
                            <Trash2 size={14} />
                          </button>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Modules status panel */}
        <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
          <div className="glass-panel">
            <h3 style={{ fontSize: "16px", fontWeight: 700, marginBottom: "20px", display: "flex", alignItems: "center", gap: "8px" }}>
              <Cpu size={18} color="var(--secondary)" />
              Module Integrations
            </h3>
            <div style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
              {modules.map(mod => (
                <div key={mod.name} style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                  <div>
                    <p style={{ fontSize: "13px", fontWeight: 600, margin: 0 }}>{mod.name}</p>
                    <span style={{ fontSize: "11px", color: "var(--text-muted)" }}>Last: {mod.lastSync}</span>
                  </div>
                  <div style={{ textAlign: "right" }}>
                    <span style={{
                      color: mod.status === "Online" || mod.status === "Completed" ? "#00E676" : mod.status === "Syncing" ? "#00BBF9" : "#FF9800",
                      fontSize: "12px", fontWeight: 700
                    }}>
                      {mod.status}
                    </span>
                    <p style={{ fontSize: "10px", color: "var(--text-muted)", margin: "2px 0 0" }}>{mod.health} Health</p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="glass-panel">
            <h3 style={{ fontSize: "16px", fontWeight: 700, marginBottom: "14px", display: "flex", alignItems: "center", gap: "8px" }}>
              <HardDrive size={18} color="var(--primary)" />
              Storage Stats
            </h3>
            <div style={{ display: "flex", flexDirection: "column", gap: "10px", fontSize: "13px" }}>
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <span style={{ color: "var(--text-secondary)" }}>D1 Database</span>
                <span style={{ fontWeight: 600 }}>14.2 MB</span>
              </div>
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <span style={{ color: "var(--text-secondary)" }}>R2 Storage (Gallery)</span>
                <span style={{ fontWeight: 600 }}>1.4 GB</span>
              </div>
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <span style={{ color: "var(--text-secondary)" }}>KV Cache</span>
                <span style={{ fontWeight: 600 }}>120 KB</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Live System Log Viewer */}
      <div className="glass-panel" style={{ padding: "24px" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "20px" }}>
          <h3 style={{ fontSize: "18px", fontWeight: 700, display: "flex", alignItems: "center", gap: "8px" }}>
            <Activity size={18} color="var(--secondary)" />
            Real-Time System Log Viewer
          </h3>
          <button
            onClick={() => handleQueueAction("clear_logs")}
            className="btn-secondary"
            style={{ display: "flex", alignItems: "center", gap: "6px", fontSize: "12px", color: "#FF5252" }}
            disabled={isActioning}
          >
            <Trash2 size={12} />
            Clear Log Store
          </button>
        </div>

        {/* Log Filters */}
        <div style={{ display: "flex", gap: "12px", marginBottom: "16px" }}>
          <div style={{ flex: 1, position: "relative" }}>
            <Search size={14} style={{ position: "absolute", left: "12px", top: "50%", transform: "translateY(-50%)", color: "var(--text-muted)" }} />
            <input
              type="text"
              placeholder="Search logs by message or module..."
              value={logSearch}
              onChange={e => setLogSearch(e.target.value)}
              style={{
                width: "100%", padding: "10px 12px 10px 36px",
                background: "rgba(255,255,255,0.03)", border: "1px solid var(--border-glass)",
                borderRadius: "var(--radius-md)", color: "#fff", fontSize: "13px"
              }}
            />
          </div>
          <select
            value={logFilter}
            onChange={e => setLogFilter(e.target.value)}
            style={{
              padding: "8px 12px", background: "rgba(255,255,255,0.03)",
              border: "1px solid var(--border-glass)", borderRadius: "var(--radius-md)",
              color: "#fff", fontSize: "13px"
            }}
          >
            <option value="All">All Categories</option>
            <option value="INFO">INFO</option>
            <option value="WARNING">WARNING</option>
            <option value="ERROR">ERROR</option>
            <option value="SECURITY">SECURITY</option>
            <option value="sync">Sync Module</option>
            <option value="auth">Auth Module</option>
            <option value="location">Location Module</option>
            <option value="gallery">Gallery Module</option>
          </select>
        </div>

        {/* Logs List Container */}
        <div style={{
          maxHeight: "360px", overflowY: "auto",
          background: "rgba(0,0,0,0.2)", border: "1px solid var(--border-glass)",
          borderRadius: "var(--radius-md)", padding: "12px",
          fontFamily: "monospace", fontSize: "12px", lineHeight: "1.6"
        }}>
          {filteredLogs.length === 0 ? (
            <div style={{ padding: "20px", textAlign: "center", color: "var(--text-muted)" }}>
              No log messages recorded
            </div>
          ) : (
            filteredLogs.map(log => {
              const color = log.severity === "ERROR" ? "#FF5252" : log.severity === "WARNING" ? "#FF9800" : log.severity === "SECURITY" ? "#f15bb5" : "#00E676";
              return (
                <div key={log.id} style={{ display: "flex", gap: "12px", borderBottom: "1px solid rgba(255,255,255,0.02)", padding: "6px 0" }}>
                  <span style={{ color: "var(--text-muted)" }}>[{new Date(log.timestamp).toLocaleTimeString()}]</span>
                  <span style={{ color, fontWeight: 700 }}>{log.severity}</span>
                  <span style={{ color: "var(--secondary)" }}>{log.module}</span>
                  <span style={{ color: "#fff", flex: 1 }}>{log.message}</span>
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
};

export default SyncPage;
