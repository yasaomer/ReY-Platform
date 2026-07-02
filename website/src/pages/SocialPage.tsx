import React, { useState, useEffect } from "react";
import { Share2, Copy, Check, Instagram, Facebook, MessageCircle, Smartphone } from "lucide-react";

interface SocialPlatform {
  name: string;
  enabled: boolean;
  username: string;
  link: string;
  notes: string;
}

export const SocialPage: React.FC = () => {
  const [platforms, setPlatforms] = useState<SocialPlatform[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [copiedIndex, setCopiedIndex] = useState<number | null>(null);

  const API_BASE = "http://localhost:8787/api/v1";

  const fetchSocials = async () => {
    setIsLoading(true);
    const token = localStorage.getItem("token");
    try {
      // Fetch configurations
      const response = await fetch(`${API_BASE}/ai/config`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      const res = await response.json();
      if (res.success) {
        // Look for social config key
        const socialConfigRaw = res.data.find((c: any) => c.config_key === "social_media_platforms");
        if (socialConfigRaw) {
          setPlatforms(JSON.parse(socialConfigRaw.config_value));
        } else {
          // Default mock/empty platforms if none synced yet
          setPlatforms([
            { name: "Instagram", enabled: true, username: "@rozuly_life", link: "https://instagram.com/rozuly_life", notes: "Personal photos feed" },
            { name: "Facebook", enabled: true, username: "Rozuly Ali", link: "https://facebook.com/rozuly", notes: "Main profile" },
            { name: "Messenger", enabled: false, username: "rozuly_chat", link: "https://m.me/rozuly", notes: "Instant replies" },
            { name: "Snapchat", enabled: true, username: "roza_snap", link: "https://snapchat.com/add/roza_snap", notes: "Daily snaps" }
          ]);
        }
      }
    } catch (e) {
      console.error("Failed to load social accounts configuration", e);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchSocials();
  }, []);

  const handleCopy = (text: string, index: number) => {
    navigator.clipboard.writeText(text);
    setCopiedIndex(index);
    setTimeout(() => setCopiedIndex(null), 2000);
  };

  const getPlatformIcon = (name: string) => {
    switch (name.toLowerCase()) {
      case "instagram": return <Instagram size={24} />;
      case "facebook": return <Facebook size={24} />;
      case "messenger": return <MessageCircle size={24} />;
      default: return <Smartphone size={24} />;
    }
  };

  const getPlatformColor = (name: string) => {
    switch (name.toLowerCase()) {
      case "instagram": return "linear-gradient(45deg, #f09433 0%, #e6683c 25%, #dc2743 50%, #cc2366 75%, #bc1888 100%)";
      case "facebook": return "#1877f2";
      case "messenger": return "linear-gradient(45deg, #006aff 0%, #00e4ff 100%)";
      case "snapchat": return "#fffc00";
      default: return "var(--primary-gradient)";
    }
  };

  // Only display completed and enabled cards (Section 61, 94)
  const activePlatforms = platforms.filter(p => p.enabled);

  return (
    <div>
      <div style={{ marginBottom: "32px" }}>
        <h2 style={{ fontSize: "28px", fontWeight: 800, fontFamily: "var(--font-display)" }}>Social Accounts</h2>
        <p style={{ color: "var(--text-secondary)", fontSize: "14px", marginTop: "4px" }}>
          Contact information and links shared intentionally by the owner
        </p>
      </div>

      {isLoading ? (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))", gap: "24px" }}>
          {[1, 2, 3].map(n => (
            <div key={n} className="glass-panel shimmer-placeholder" style={{ height: "180px" }} />
          ))}
        </div>
      ) : activePlatforms.length === 0 ? (
        <div className="glass-panel" style={{ textAlign: "center", padding: "80px 20px" }}>
          <Share2 size={48} color="var(--text-muted)" style={{ marginBottom: "16px" }} />
          <h3 style={{ fontSize: "18px", marginBottom: "8px", fontWeight: 600 }}>No Accounts Shared</h3>
          <p style={{ color: "var(--text-secondary)", fontSize: "14px", maxWidth: "340px", margin: "0 auto" }}>
            The owner hasn't shared any social media accounts yet.
          </p>
        </div>
      ) : (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))", gap: "24px" }}>
          {activePlatforms.map((platform, idx) => (
            <div
              key={platform.name}
              className="glass-panel"
              style={{
                display: "flex",
                flexDirection: "column",
                justifyContent: "space-between",
                height: "100%",
                border: "1px solid var(--border-glass)",
                position: "relative",
                overflow: "hidden"
              }}
            >
              <div>
                {/* Header branding */}
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "20px" }}>
                  <div style={{
                    background: getPlatformColor(platform.name),
                    color: platform.name.toLowerCase() === 'snapchat' ? '#000' : '#fff',
                    padding: "10px",
                    borderRadius: "var(--radius-md)",
                    display: "flex"
                  }}>
                    {getPlatformIcon(platform.name)}
                  </div>
                  <span style={{ fontSize: "12px", color: "var(--text-muted)", fontWeight: 500 }}>
                    Active
                  </span>
                </div>

                <h3 style={{ fontSize: "20px", fontWeight: 700, fontFamily: "var(--font-display)", marginBottom: "4px" }}>
                  {platform.name}
                </h3>
                <p style={{ color: "var(--text-secondary)", fontSize: "14px", marginBottom: "16px" }}>
                  {platform.notes}
                </p>
              </div>

              {/* Copy action field */}
              <div style={{
                background: "rgba(255, 255, 255, 0.03)",
                border: "1px solid var(--border-glass)",
                borderRadius: "var(--radius-md)",
                padding: "8px 12px",
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between",
                gap: "8px"
              }}>
                <span style={{ fontSize: "14px", fontWeight: 500, fontFamily: "monospace", overflow: "hidden", textOverflow: "ellipsis" }}>
                  {platform.username}
                </span>
                
                <button
                  onClick={() => handleCopy(platform.username, idx)}
                  style={{
                    background: "none",
                    border: "none",
                    color: copiedIndex === idx ? "var(--secondary)" : "var(--text-secondary)",
                    cursor: "pointer",
                    display: "flex",
                    padding: "6px",
                    borderRadius: "4px",
                    transition: "var(--transition-fast)"
                  }}
                  onMouseEnter={(e) => (e.currentTarget.style.background = "rgba(255, 255, 255, 0.05)")}
                  onMouseLeave={(e) => (e.currentTarget.style.background = "transparent")}
                >
                  {copiedIndex === idx ? <Check size={16} /> : <Copy size={16} />}
                </button>
              </div>
              
              {platform.link && (
                <a
                  href={platform.link}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="btn-secondary"
                  style={{
                    marginTop: "16px",
                    justifyContent: "center",
                    padding: "10px",
                    fontSize: "14px"
                  }}
                >
                  Visit Platform Profile
                </a>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
export default SocialPage;
