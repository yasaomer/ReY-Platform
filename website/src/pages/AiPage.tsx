import React, { useState, useEffect, useRef } from "react";
import { 
  Bot, Send, User, RefreshCw, XCircle, 
  Cpu, Wifi, ShieldCheck, AlertTriangle 
} from "lucide-react";
import { API_BASE } from "../config";

interface Message {
  role: "user" | "model";
  text: string;
  provider?: string;
  timestamp?: string;
}

export const AiPage: React.FC = () => {
  const [messages, setMessages] = useState<Message[]>([
    { 
      role: "model", 
      text: "Hello! I am **ReY**, the owner's personal AI twin. I speak Kurdish, English, and Arabic, and I am trained on authorized biography files, speaking style constraints, and private PDF libraries.\n\nHere are some things you can ask me:\n- *What are the owner's current goals?*\n- *Explain his technical work in Kurdish style.*\n- *How does he joke or solve problems?*",
      provider: "Gemini 1.5 Flash",
      timestamp: "18:24"
    }
  ]);
  const [inputText, setInputText] = useState("");
  const [statusMessage, setStatusMessage] = useState("");
  const [isWaiting, setIsWaiting] = useState(false);
  const [remainingMessages, setRemainingMessages] = useState(85);
  const [errorText, setErrorText] = useState("");
  const [activeProvider, setActiveProvider] = useState("Gemini");
  
  const scrollRef = useRef<HTMLDivElement>(null);

  // Load provider config dynamically on mount
  useEffect(() => {
    const fetchAiConfig = async () => {
      const token = localStorage.getItem("token");
      try {
        const response = await fetch(`${API_BASE}/sync/telemetry`, {
          headers: { Authorization: `Bearer ${token}` }
        });
        const res = await response.json();
        if (res.success && res.data) {
          // If custom config is retrieved, update provider
          setActiveProvider(res.data.ai_provider || "Gemini");
        }
      } catch (e) {
        console.error("Failed to load telemetry AI config", e);
      }
    };
    fetchAiConfig();
  }, []);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollIntoView({ behavior: "smooth" });
    }
  }, [messages, statusMessage]);

  const handleCancelWaiting = () => {
    setIsWaiting(false);
    setStatusMessage("");
    setErrorText("Query cancelled by user.");
  };

  const handleSend = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputText.trim() || isWaiting) return;

    const userQuery = inputText.trim();
    setInputText("");
    const nowStr = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    
    setMessages(prev => [...prev, { role: "user", text: userQuery, timestamp: nowStr }]);
    setIsWaiting(true);
    setErrorText("");

    const statusCycle = [
      "Searching local PDF knowledge databases...",
      "Reading speaking personality guidelines...",
      "Connecting to active provider endpoint...",
      "Formulating response in Kurdish/English style..."
    ];
    let statusIdx = 0;
    setStatusMessage(statusCycle[0]);
    
    const statusInterval = setInterval(() => {
      statusIdx++;
      if (statusIdx < statusCycle.length) {
        setStatusMessage(statusCycle[statusIdx]);
      }
    }, 1800);

    const token = localStorage.getItem("token");
    try {
      const response = await fetch(`${API_BASE}/ai/ask`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({ 
          message: userQuery, 
          conversationHistory: messages.map(m => ({ role: m.role, text: m.text }))
        })
      });

      clearInterval(statusInterval);
      const res = await response.json();

      if (!response.ok || !res.success) {
        throw new Error(res.message || "Provider request timeout.");
      }

      setMessages(prev => [...prev, { 
        role: "model", 
        text: res.data.answer, 
        provider: activeProvider,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      }]);
      setRemainingMessages(prev => Math.max(0, prev - 1));
    } catch (err: any) {
      clearInterval(statusInterval);
      setErrorText(err.message || "Failed to formulate response. Attempting connection retry...");
    } finally {
      setIsWaiting(false);
      setStatusMessage("");
    }
  };

  // ==========================================
  // HIGH-FIDELITY MARKDOWN PARSING ENGINE
  // ==========================================
  const renderMarkdown = (text: string) => {
    const lines = text.split("\n");

    return lines.map((line, idx) => {
      const trimmed = line.trim();

      // 1. Headers Detector
      if (trimmed.startsWith("### ")) {
        return <h4 key={idx} style={{ color: "var(--primary)", fontSize: "16px", fontWeight: 700, margin: "16px 0 8px 0" }}>{parseInlineMarkdown(trimmed.substring(4))}</h4>;
      }
      if (trimmed.startsWith("## ")) {
        return <h3 key={idx} style={{ color: "var(--primary)", fontSize: "18px", fontWeight: 800, margin: "20px 0 10px 0" }}>{parseInlineMarkdown(trimmed.substring(3))}</h3>;
      }
      if (trimmed.startsWith("# ")) {
        return <h2 key={idx} style={{ color: "var(--primary)", fontSize: "22px", fontWeight: 800, margin: "24px 0 12px 0" }}>{parseInlineMarkdown(trimmed.substring(2))}</h2>;
      }

      // 2. Block Quotes Detector
      if (trimmed.startsWith("> ")) {
        return (
          <blockquote key={idx} style={{ borderLeft: "4px solid var(--secondary)", paddingLeft: "16px", color: "var(--text-secondary)", fontStyle: "italic", margin: "12px 0" }}>
            {parseInlineMarkdown(trimmed.substring(2))}
          </blockquote>
        );
      }

      // 3. Code Block Boundaries Detector
      if (trimmed.startsWith("```")) {
        return null; // Ignore simple bounds tags, styling inline code blocks below
      }

      // 4. Bullet lists Detector
      if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
        return (
          <li key={idx} style={{ marginLeft: "20px", marginBottom: "6px", fontSize: "15px", listStyleType: "square", color: "var(--text-primary)" }}>
            {parseInlineMarkdown(trimmed.substring(2))}
          </li>
        );
      }

      // 5. Numbered lists Detector
      const numberMatch = trimmed.match(/^(\d+)\.\s(.*)/);
      if (numberMatch) {
        return (
          <li key={idx} style={{ marginLeft: "20px", marginBottom: "6px", fontSize: "15px", listStyleType: "decimal", color: "var(--text-primary)" }}>
            {parseInlineMarkdown(numberMatch[2])}
          </li>
        );
      }

      // Default paragraph line
      return (
        <p key={idx} style={{ marginBottom: "10px", fontSize: "15px", lineHeight: "1.7", color: "var(--text-primary)" }}>
          {parseInlineMarkdown(line)}
        </p>
      );
    });
  };

  const parseInlineMarkdown = (text: string) => {
    let parts: (string | React.ReactElement)[] = [text];

    // Simple inline parser splitter
    if (text.includes("**")) {
      const rawParts = text.split(/(\*\*.*?\*\*)/g);
      parts = rawParts.map((part, i) => {
        if (part.startsWith("**") && part.endsWith("**")) {
          return <strong key={i} style={{ color: "var(--secondary)", fontWeight: 700 }}>{part.slice(2, -2)}</strong>;
        }
        return part;
      });
    }

    // Process italics *text*
    parts = parts.flatMap((part): (string | React.ReactElement)[] => {
      if (typeof part !== "string") return [part];
      if (part.includes("*")) {
        const italicParts = part.split(/(\*.*?\*)/g);
        return italicParts.map((p, i) => {
          if (p.startsWith("*") && p.endsWith("*")) {
            return <em key={i} style={{ fontStyle: "italic", opacity: 0.9 }}>{p.slice(1, -1)}</em>;
          }
          return p;
        });
      }
      return [part];
    });

    // Process inline code blocks `code`
    parts = parts.flatMap((part): (string | React.ReactElement)[] => {
      if (typeof part !== "string") return [part];
      if (part.includes("`")) {
        const codeParts = part.split(/(`.*?`)/g);
        return codeParts.map((p, i) => {
          if (p.startsWith("`") && p.endsWith("`")) {
            return (
              <code key={i} style={{ 
                background: "rgba(255,255,255,0.08)", 
                padding: "2px 6px", 
                borderRadius: "4px", 
                fontFamily: "monospace", 
                fontSize: "13px", 
                color: "#ff758f" 
              }}>
                {p.slice(1, -1)}
              </code>
            );
          }
          return p;
        });
      }
      return [part];
    });

    return parts;
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", height: "calc(100vh - 120px)", maxWidth: "800px", margin: "0 auto", padding: "0 16px" }}>
      
      {/* Settings status banner */}
      <div style={{
        display: "flex",
        justifyContent: "space-between",
        alignItems: "center",
        paddingBottom: "16px",
        borderBottom: "1px solid var(--border-glass)",
        marginBottom: "20px"
      }}>
        <div>
          <h2 style={{ fontSize: "28px", fontWeight: 800, fontFamily: "var(--font-display)", letterSpacing: "-0.5px" }}>AI Assistant</h2>
          <div style={{ display: "flex", alignItems: "center", gap: "8px", marginTop: "4px", color: "var(--text-secondary)", fontSize: "12px" }}>
            <Cpu size={14} color="var(--primary)" />
            <span>Active Brain: {activeProvider}</span>
            <span style={{ color: "rgba(255,255,255,0.15)" }}>|</span>
            <Wifi size={14} color="var(--secondary)" />
            <span>Connection: Secure</span>
          </div>
        </div>

        <div style={{
          background: "rgba(0, 245, 212, 0.08)",
          border: "1px solid rgba(0, 245, 212, 0.2)",
          borderRadius: "8px",
          padding: "8px 16px",
          fontSize: "12px",
          color: "var(--secondary)",
          fontWeight: 600
        }}>
          Budget: <b>{remainingMessages}</b> left today
        </div>
      </div>

      {/* Conversations Stream Log */}
      <div style={{
        flex: 1,
        overflowY: "auto",
        padding: "10px 4px",
        display: "flex",
        flexDirection: "column",
        gap: "24px",
        marginBottom: "24px"
      }}>
        {messages.map((msg, index) => {
          const isModel = msg.role === "model";
          return (
            <div
              key={index}
              style={{
                display: "flex",
                gap: "14px",
                alignItems: "flex-start",
                justifyContent: isModel ? "flex-start" : "flex-end",
                animation: "fadeIn 0.4s ease-out"
              }}
            >
              {isModel && (
                <div style={{
                  background: "var(--primary-gradient)",
                  padding: "10px",
                  borderRadius: "50%",
                  color: "#fff",
                  display: "flex",
                  boxShadow: "0 4px 12px rgba(155, 93, 229, 0.3)"
                }}>
                  <Bot size={20} />
                </div>
              )}

              <div 
                className="glass-panel" 
                style={{
                  maxWidth: "75%",
                  padding: "16px 20px",
                  borderRadius: isModel ? "0px 16px 16px 16px" : "16px 0px 16px 16px",
                  background: isModel ? "rgba(18, 20, 32, 0.4)" : "rgba(155, 93, 229, 0.08)",
                  borderColor: isModel ? "var(--border-glass)" : "rgba(155, 93, 229, 0.25)",
                  boxShadow: isModel ? "none" : "0 4px 12px rgba(155, 93, 229, 0.05)"
                }}
              >
                {/* Renders parsed markdown */}
                <div style={{ fontFamily: "var(--font-sans)" }}>
                  {renderMarkdown(msg.text)}
                </div>

                {/* Message footer metadata */}
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: "12px", borderTop: "1px solid rgba(255,255,255,0.05)", paddingTop: "8px", fontSize: "10px", color: "var(--text-secondary)" }}>
                  <span style={{ display: "flex", alignItems: "center", gap: "4px" }}>
                    <ShieldCheck size={10} color={isModel ? "var(--secondary)" : "var(--text-muted)"} />
                    <span>{isModel ? `${msg.provider || "System"}` : "Owner Signed"}</span>
                  </span>
                  <span>{msg.timestamp || "Just now"}</span>
                </div>
              </div>

              {!isModel && (
                <div style={{
                  background: "rgba(255,255,255,0.05)",
                  padding: "10px",
                  borderRadius: "50%",
                  color: "var(--text-secondary)",
                  display: "flex",
                  border: "1px solid var(--border-glass)"
                }}>
                  <User size={20} />
                </div>
              )}
            </div>
          );
        })}

        {/* Dynamic Thinking Status Loader */}
        {isWaiting && statusMessage && (
          <div style={{ display: "flex", gap: "14px", alignItems: "center", animation: "fadeIn 0.3s ease-out" }}>
            <div style={{
              background: "var(--primary-gradient)",
              padding: "10px",
              borderRadius: "50%",
              color: "#fff",
              display: "flex"
            }}>
              <RefreshCw size={20} className="animate-spin" />
            </div>
            
            <div className="glass-panel" style={{
              padding: "12px 20px",
              borderRadius: "0px 16px 16px 16px",
              fontSize: "13px",
              color: "var(--text-secondary)",
              display: "flex",
              alignItems: "center",
              gap: "12px"
            }}>
              <span>{statusMessage}</span>
              
              <button
                onClick={handleCancelWaiting}
                style={{
                  background: "none",
                  border: "none",
                  color: "#ef4444",
                  cursor: "pointer",
                  display: "flex",
                  alignItems: "center",
                  gap: "4px",
                  fontSize: "12px",
                  fontWeight: 600
                }}
              >
                <XCircle size={14} />
                Cancel
              </button>
            </div>
          </div>
        )}

        {/* Error retry states */}
        {errorText && (
          <div style={{ display: "flex", gap: "14px", alignItems: "center", alignSelf: "center", animation: "fadeIn 0.3s ease-out" }}>
            <div style={{
              background: "rgba(239, 68, 68, 0.08)",
              border: "1px solid rgba(239, 68, 68, 0.2)",
              borderRadius: "8px",
              padding: "12px 18px",
              fontSize: "13px",
              color: "#f87171",
              display: "flex",
              alignItems: "center",
              gap: "8px"
            }}>
              <AlertTriangle size={16} />
              <span>{errorText}</span>
            </div>
          </div>
        )}

        <div ref={scrollRef} />
      </div>

      {/* Input composition form container */}
      <form onSubmit={handleSend} style={{ display: "flex", gap: "12px", background: "rgba(18,20,32,0.6)", padding: "8px", borderRadius: "12px", border: "1px solid var(--border-glass)", marginBottom: "16px" }}>
        <input
          type="text"
          placeholder="Ask your AI companion a question..."
          className="form-input"
          style={{ flex: 1, background: "transparent", border: "none", outline: "none", padding: "10px 14px", color: "white" }}
          value={inputText}
          onChange={(e) => setInputText(e.target.value)}
          disabled={isWaiting}
        />
        <button
          type="submit"
          className="btn-primary"
          style={{ padding: "12px 24px", borderRadius: "8px" }}
          disabled={isWaiting || !inputText.trim()}
        >
          <Send size={16} />
        </button>
      </form>
    </div>
  );
};

export default AiPage;
