import React, { useState, useEffect } from "react";
import { 
  MessageSquare, Calendar, Milestone, ShieldCheck, 
  ArrowUp, Play, Instagram, ExternalLink, Link as LinkIcon 
} from "lucide-react";
import { API_BASE } from "../config";

interface MessageData {
  message_content: string;
  created_at: string;
  updated_at: string;
  version: number;
}

const LastMessagePage: React.FC = () => {
  const [msg, setMsg] = useState<MessageData | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [showScrollTop, setShowScrollTop] = useState(false);
  const [scrollProgress, setScrollProgress] = useState(0);

  // Load message content
  const fetchLastMessage = async () => {
    const token = localStorage.getItem("token");
    try {
      const response = await fetch(`${API_BASE}/sync/last-message`, {
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

  // Scroll tracking for progress and return-to-top button
  useEffect(() => {
    fetchLastMessage();

    const handleScroll = () => {
      const totalScroll = document.documentElement.scrollHeight - window.innerHeight;
      if (totalScroll > 0) {
        setScrollProgress((window.scrollY / totalScroll) * 100);
      }
      setShowScrollTop(window.scrollY > 400);
    };

    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  const formatDate = (isoString: string) => {
    const date = new Date(isoString);
    return date.toLocaleString();
  };

  const scrollToTop = () => {
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  // ==========================================
  // DYNAMIC EMBEDS PARSER ENGINE
  // ==========================================
  const parseEmbeds = (content: string) => {
    if (!content) return null;

    // Split text by lines to extract URL blocks naturally
    const lines = content.split(/\n|<br\s*\/?>/i);

    return lines.map((line, idx) => {
      const trimmed = line.trim();

      // 1. YouTube Video Embed Detector
      const youtubeMatch = trimmed.match(/https?:\/\/(www\.)?(youtube\.com\/watch\?v=|youtu\.be\/)([a-zA-Z0-9_-]+)/);
      if (youtubeMatch) {
        const videoId = youtubeMatch[3];
        return (
          <div key={idx} className="embed-container" style={{ margin: "24px 0", borderRadius: "12px", overflow: "hidden" }}>
            <div style={{ position: "relative", paddingBottom: "56.25%", height: 0 }}>
              <iframe
                title="YouTube Video"
                src={`https://www.youtube.com/embed/${videoId}`}
                frameBorder="0"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowFullScreen
                style={{ position: "absolute", top: 0, left: 0, width: "100%", height: "100%" }}
              />
            </div>
          </div>
        );
      }

      // 2. Instagram Reels / Post Embed Detector
      const instagramMatch = trimmed.match(/https?:\/\/(www\.)?instagram\.com\/(p|reel|tv)\/([a-zA-Z0-9_-]+)/);
      if (instagramMatch) {
        const type = instagramMatch[2];
        const postId = instagramMatch[3];
        return (
          <div key={idx} className="glass-panel" style={{ margin: "24px 0", padding: "20px", display: "flex", flexDirection: "column", gap: "16px" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
              <div style={{ background: "linear-gradient(45deg, #f09433 0%, #e6683c 25%, #dc2743 50%, #cc2366 75%, #bc1888 100%)", padding: "8px", borderRadius: "50%", display: "flex", alignItems: "center", justifyContent: "center" }}>
                <Instagram size={20} color="white" />
              </div>
              <div>
                <h4 style={{ fontSize: "14px", fontWeight: 700, margin: 0 }}>Instagram {type === "reel" ? "Reel" : "Post"}</h4>
                <p style={{ color: "var(--text-secondary)", fontSize: "11px", margin: 0 }}>ID: {postId}</p>
              </div>
            </div>
            
            <div style={{ position: "relative", height: "160px", background: "rgba(255,255,255,0.05)", borderRadius: "8px", display: "flex", alignItems: "center", justifyContent: "center" }}>
              <Play size={40} color="var(--primary)" style={{ opacity: 0.8 }} />
            </div>

            <a 
              href={`https://instagram.com/${type}/${postId}`} 
              target="_blank" 
              rel="noopener noreferrer"
              className="action-button-primary"
              style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "8px", padding: "10px", borderRadius: "8px", textDecoration: "none", fontSize: "13px" }}
            >
              <span>View on Instagram</span>
              <ExternalLink size={14} />
            </a>
          </div>
        );
      }

      // 3. Direct Image Links Detector
      const imageMatch = trimmed.match(/https?:\/\/[^\s]+?\.(jpg|jpeg|png|gif|webp|svg)/i);
      if (imageMatch) {
        const imageUrl = imageMatch[0];
        return (
          <div key={idx} className="glass-panel" style={{ margin: "24px 0", padding: "8px", borderRadius: "12px", overflow: "hidden", display: "inline-block", maxWidth: "100%" }}>
            <img 
              src={imageUrl} 
              alt="Embedded dispatch visual" 
              loading="lazy"
              style={{ width: "100%", maxHeight: "500px", objectFit: "contain", borderRadius: "8px" }} 
            />
          </div>
        );
      }

      // 4. Other Raw URL Links Detector
      const linkMatch = trimmed.match(/^https?:\/\/[^\s]+$/);
      if (linkMatch) {
        const url = linkMatch[0];
        return (
          <a key={idx} href={url} target="_blank" rel="noopener noreferrer" className="glass-panel" style={{ margin: "16px 0", padding: "16px", display: "flex", alignItems: "center", gap: "12px", textDecoration: "none", color: "var(--text-primary)" }}>
            <div style={{ background: "rgba(255,255,255,0.05)", padding: "8px", borderRadius: "6px" }}>
              <LinkIcon size={18} color="var(--secondary)" />
            </div>
            <div style={{ flex: 1, overflow: "hidden" }}>
              <span style={{ fontSize: "14px", fontWeight: 600, display: "block", textOverflow: "ellipsis", overflow: "hidden", whiteSpace: "nowrap" }}>{url}</span>
              <span style={{ color: "var(--text-secondary)", fontSize: "11px", display: "flex", alignItems: "center", gap: "4px" }}>
                <span>Open external resource</span>
                <ExternalLink size={10} />
              </span>
            </div>
          </a>
        );
      }

      // Default line: Render standard rich text HTML
      return (
        <p 
          key={idx} 
          style={{ marginBottom: "16px", fontSize: "17px", lineHeight: "1.8", color: "var(--text-primary)", fontWeight: 400 }}
          dangerouslySetInnerHTML={{ __html: line }}
        />
      );
    });
  };

  return (
    <div style={{ maxWidth: "720px", margin: "0 auto", padding: "0 16px", paddingBottom: "80px", position: "relative" }}>
      
      {/* Scroll progress bar */}
      <div style={{
        position: "fixed",
        top: 0,
        left: 0,
        height: "3.5px",
        background: "linear-gradient(to right, var(--primary), var(--secondary))",
        width: `${scrollProgress}%`,
        zIndex: 9999,
        transition: "width 0.1s ease-out"
      }} />

      <div style={{ marginBottom: "32px", animation: "fadeIn 0.6s ease-out" }}>
        <h2 style={{ fontSize: "36px", fontWeight: 800, fontFamily: "var(--font-display)", letterSpacing: "-0.5px" }}>Last Message</h2>
        <p style={{ color: "var(--text-secondary)", fontSize: "14px", marginTop: "4px" }}>
          Current status dispatch written by the owner
        </p>
      </div>

      {isLoading ? (
        <div className="glass-panel shimmer-placeholder" style={{ height: "400px" }} />
      ) : !msg ? (
        <div className="glass-panel" style={{ textAlign: "center", padding: "80px 20px" }}>
          <MessageSquare size={48} color="var(--text-muted)" style={{ marginBottom: "16px" }} />
          <h3 style={{ fontSize: "18px", marginBottom: "8px", fontWeight: 600 }}>No Message Dispatched</h3>
          <p style={{ color: "var(--text-secondary)", fontSize: "14px", maxWidth: "340px", margin: "0 auto" }}>
            The owner has not posted any status messages. Statuses compiled in the Private OML editor will display here.
          </p>
        </div>
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: "32px", animation: "fadeIn 0.8s ease-out" }}>
          
          {/* Main Reading Container */}
          <div className="glass-panel" style={{ padding: "32px 24px", minHeight: "400px", background: "rgba(18, 20, 32, 0.4)" }}>
            
            {/* Parsed message text and embeds flows */}
            <div style={{ fontFamily: "var(--font-sans)" }}>
              {parseEmbeds(msg.message_content)}
            </div>

            {/* Footer metadata */}
            <div style={{
              borderTop: "1px solid var(--border-glass)",
              paddingTop: "24px",
              marginTop: "48px",
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
                <span style={{ color: "var(--secondary)", fontWeight: 600 }}>Verified Sender</span>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Floating Return-To-Top button */}
      {showScrollTop && (
        <button 
          onClick={scrollToTop}
          style={{
            position: "fixed",
            bottom: "32px",
            right: "32px",
            backgroundColor: "var(--primary)",
            color: "black",
            border: "none",
            borderRadius: "50%",
            width: "50px",
            height: "50px",
            cursor: "pointer",
            boxShadow: "0 8px 24px rgba(155, 93, 229, 0.4)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 999,
            transition: "transform 0.2s ease",
            transform: "scale(1)"
          }}
          onMouseEnter={(e) => e.currentTarget.style.transform = "scale(1.1)"}
          onMouseLeave={(e) => e.currentTarget.style.transform = "scale(1)"}
        >
          <ArrowUp size={22} strokeWidth={2.5} />
        </button>
      )}
    </div>
  );
};

export default LastMessagePage;
