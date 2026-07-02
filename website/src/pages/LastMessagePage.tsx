import React, { useState, useEffect } from "react";
import { MessageSquare, Calendar, Milestone, ShieldCheck } from "lucide-react";

interface MessageData {
  message_content: string;
  created_at: string;
  updated_at: string;
  version: number;
}

export const LastMessagePage: React.FC = () => {
  const [msg, setMsg] = useState<MessageData | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const fetchLastMessage = async () => {
    const token = localStorage.getItem("token");
    try {
      const response = await fetch("http://localhost:8787/api/v1/sync/last-message", {
        headers: { Authorization: `Bearer ${token}` }
      });
      const res = await response.json();
      if (res.success && res.data.message_content) {
        setMsg(res.data);
      }
    } catch (e) {
      console.error("Failed to load last message", e);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchLastMessage();
  }, []);

  const formatDate = (isoString: string) => {
    const date = new Date(isoString);
    return date.toLocaleString();
  };

  return (
    <div>
      <div style={{ marginBottom: "32px" }}>
        <h2 style={{ fontSize: "28px", fontWeight: 800, fontFamily: "var(--font-display)" }}>Last Message</h2>
        <p style={{ color: "var(--text-secondary)", fontSize: "14px", marginTop: "4px" }}>
          Current status dispatch written by the owner
        </p>
      </div>

      {isLoading ? (
        <div className="glass-panel shimmer-placeholder" style={{ height: "300px" }} />
      ) : !msg ? (
        <div className="glass-panel" style={{ textAlign: "center", padding: "80px 20px" }}>
          <MessageSquare size={48} color="var(--text-muted)" style={{ marginBottom: "16px" }} />
          <h3 style={{ fontSize: "18px", marginBottom: "8px", fontWeight: 600 }}>No Message Dispatched</h3>
          <p style={{ color: "var(--text-secondary)", fontSize: "14px", maxWidth: "340px", margin: "0 auto" }}>
            The owner has not posted any status messages. Statuses compiled in the Private OML editor will display here.
          </p>
        </div>
      ) : (
        <div style={{ display: "grid", gridTemplateColumns: "1fr", gap: "24px" }}>
          {/* Main message card container */}
          <div className="glass-panel" style={{ minHeight: "350px", display: "flex", flexDirection: "column", justifyContent: "space-between" }}>
            
            {/* Rich text container */}
            <div 
              style={{ 
                lineHeight: "1.8", 
                fontSize: "16px", 
                color: "var(--text-primary)", 
                fontFamily: "var(--font-sans)"
              }}
              dangerouslySetInnerHTML={{ __html: msg.message_content }}
            />

            {/* Footer metadata */}
            <div style={{
              borderTop: "1px solid var(--border-glass)",
              paddingTop: "20px",
              marginTop: "40px",
              display: "flex",
              flexWrap: "wrap",
              gap: "24px",
              color: "var(--text-secondary)",
              fontSize: "13px"
            }}>
              <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                <Calendar size={16} color="var(--text-muted)" />
                <span>Updated: {formatDate(msg.updated_at)}</span>
              </div>
              
              <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                <Milestone size={16} color="var(--text-muted)" />
                <span>Version: {msg.version}</span>
              </div>

              <div style={{ display: "flex", alignItems: "center", gap: "8px", marginLeft: "auto" }}>
                <ShieldCheck size={16} color="var(--secondary)" />
                <span style={{ color: "var(--secondary)", fontWeight: 500 }}>Verified Sender</span>
              </div>
            </div>

          </div>
        </div>
      )}
    </div>
  );
};
export default LastMessagePage;
