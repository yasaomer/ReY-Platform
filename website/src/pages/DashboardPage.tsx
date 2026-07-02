import React, { useState, useEffect } from "react";
import { Activity, Image as ImageIcon, Bot, AlertTriangle, RefreshCw, HardDrive } from "lucide-react";
import { API_BASE } from "../config";

interface AnalyticsData {
  sync: {
    lastSyncTime: string | null;
    lastSyncStatus: string;
    totalErrors: number;
  };
  gallery: {
    totalViews: number;
    mostViewedImage: string;
    mostViewedViews: number;
  };
  ai: {
    successfulRequests: number;
    failedRequests: number;
    averageLatencyMs: number;
  };
  knowledgeBase: {
    totalDocuments: number;
  };
  system: {
    activeErrorsCount: number;
  };
}

export const DashboardPage: React.FC = () => {
  const [data, setData] = useState<AnalyticsData | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const username = localStorage.getItem("username") || "Visitor";

  const fetchDashboard = async (silent = false) => {
    if (!silent) setIsLoading(true);
    else setIsRefreshing(true);
    
    const token = localStorage.getItem("token");
    try {
      const response = await fetch(`${API_BASE}/analytics/dashboard`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      const res = await response.json();
      if (res.success) {
        setData(res.data);
      }
    } catch (e) {
      console.error("Failed to load analytics dashboard data", e);
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  };

  useEffect(() => {
    fetchDashboard();
  }, []);

  const formatDate = (isoString: string | null) => {
    if (!isoString) return "Never";
    const date = new Date(isoString);
    return date.toLocaleString();
  };

  return (
    <div>
      {/* Hero Section */}
      <section style={{
        background: "linear-gradient(135deg, rgba(155, 93, 229, 0.15) 0%, rgba(0, 245, 212, 0.05) 100%)",
        border: "1px solid var(--border-glass)",
        borderRadius: "24px",
        padding: "48px 40px",
        marginBottom: "40px",
        position: "relative",
        overflow: "hidden"
      }}>
        <div style={{ zIndex: 2, position: "relative" }}>
          <h2 style={{ fontSize: "36px", fontWeight: 800, fontFamily: "var(--font-display)", marginBottom: "8px" }}>
            Welcome back, <span style={{ background: "var(--primary-gradient)", WebkitBackgroundClip: "text", WebkitTextFillColor: "transparent" }}>{username}</span>
          </h2>
          <p style={{ color: "var(--text-secondary)", fontSize: "16px", maxWidth: "600px" }}>
            Your secure gateway to synchronization status, location updates, personal media archives, and intelligent assistant query processing.
          </p>
        </div>
        {/* Glow decorative block */}
        <div style={{
          position: "absolute",
          top: "-50px",
          right: "-50px",
          width: "200px",
          height: "200px",
          borderRadius: "50%",
          background: "var(--primary-glow)",
          filter: "blur(80px)"
        }} />
      </section>

      {/* Synchronize Trigger */}
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "24px" }}>
        <h3 style={{ fontSize: "20px", fontWeight: 700, fontFamily: "var(--font-display)" }}>Ecosystem Status</h3>
        <button
          onClick={() => fetchDashboard(true)}
          className="btn-secondary"
          style={{ padding: "8px 16px", fontSize: "14px" }}
          disabled={isRefreshing}
        >
          <RefreshCw size={16} className={isRefreshing ? "animate-spin" : ""} />
          {isRefreshing ? "Updating..." : "Refresh Status"}
        </button>
      </div>

      {isLoading ? (
        // Loading skeletons
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(260px, 1fr))", gap: "24px" }}>
          {[1, 2, 3, 4].map(n => (
            <div key={n} className="glass-panel shimmer-placeholder" style={{ height: "140px" }} />
          ))}
        </div>
      ) : data ? (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(260px, 1fr))", gap: "24px" }}>
          
          {/* Card 1: Sync status */}
          <div className="glass-panel" style={{ display: "flex", gap: "20px", alignItems: "flex-start" }}>
            <div style={{
              background: "rgba(0, 245, 212, 0.1)",
              color: "var(--secondary)",
              padding: "12px",
              borderRadius: "var(--radius-md)"
            }}>
              <Activity size={24} />
            </div>
            <div>
              <p style={{ color: "var(--text-secondary)", fontSize: "14px" }}>Sync Status</p>
              <h4 style={{ fontSize: "22px", margin: "4px 0" }}>
                {data.sync.lastSyncStatus === 'completed' ? 'Synced' : 'Idle'}
              </h4>
              <p style={{ fontSize: "12px", color: "var(--text-muted)" }}>
                Last: {formatDate(data.sync.lastSyncTime)}
              </p>
            </div>
          </div>

          {/* Card 2: Gallery statistics */}
          <div className="glass-panel" style={{ display: "flex", gap: "20px", alignItems: "flex-start" }}>
            <div style={{
              background: "rgba(155, 93, 229, 0.1)",
              color: "var(--primary)",
              padding: "12px",
              borderRadius: "var(--radius-md)"
            }}>
              <ImageIcon size={24} />
            </div>
            <div>
              <p style={{ color: "var(--text-secondary)", fontSize: "14px" }}>Gallery Views</p>
              <h4 style={{ fontSize: "22px", margin: "4px 0" }}>{data.gallery.totalViews}</h4>
              <p style={{ fontSize: "12px", color: "var(--text-muted)" }}>
                Popular: {data.gallery.mostViewedImage.substring(0, 15)}...
              </p>
            </div>
          </div>

          {/* Card 3: AI system */}
          <div className="glass-panel" style={{ display: "flex", gap: "20px", alignItems: "flex-start" }}>
            <div style={{
              background: "rgba(0, 187, 249, 0.1)",
              color: "#00bbf9",
              padding: "12px",
              borderRadius: "var(--radius-md)"
            }}>
              <Bot size={24} />
            </div>
            <div>
              <p style={{ color: "var(--text-secondary)", fontSize: "14px" }}>AI Interactions</p>
              <h4 style={{ fontSize: "22px", margin: "4px 0" }}>{data.ai.successfulRequests}</h4>
              <p style={{ fontSize: "12px", color: "var(--text-muted)" }}>
                Latency: {data.ai.averageLatencyMs}ms avg
              </p>
            </div>
          </div>

          {/* Card 4: Knowledge Base documents */}
          <div className="glass-panel" style={{ display: "flex", gap: "20px", alignItems: "flex-start" }}>
            <div style={{
              background: "rgba(241, 91, 181, 0.1)",
              color: "#f15bb5",
              padding: "12px",
              borderRadius: "var(--radius-md)"
            }}>
              <HardDrive size={24} />
            </div>
            <div>
              <p style={{ color: "var(--text-secondary)", fontSize: "14px" }}>Knowledge base</p>
              <h4 style={{ fontSize: "22px", margin: "4px 0" }}>{data.knowledgeBase.totalDocuments} Docs</h4>
              <p style={{ fontSize: "12px", color: "var(--text-muted)" }}>
                Alerts: {data.system.activeErrorsCount} reports active
              </p>
            </div>
          </div>
        </div>
      ) : (
        <div className="glass-panel" style={{ textAlign: "center", padding: "40px 20px" }}>
          <AlertTriangle size={40} color="#f59e0b" style={{ marginBottom: "16px" }} />
          <p style={{ color: "var(--text-secondary)" }}>Failed to fetch ecosystem analytics. Check server logs.</p>
        </div>
      )}
    </div>
  );
};
export default DashboardPage;
