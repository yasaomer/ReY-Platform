import React, { useState, useEffect, useRef } from "react";
import { Bot, Send, User, AlertCircle, RefreshCw, XCircle } from "lucide-react";

interface Message {
  role: "user" | "model";
  text: string;
}

export const AiPage: React.FC = () => {
  const [messages, setMessages] = useState<Message[]>([
    { role: "model", text: "Hello! I am ReY, your personal AI assistant. I have access to the owner's authorized documents and memories. Ask me anything!" }
  ]);
  const [inputText, setInputText] = useState("");
  const [statusMessage, setStatusMessage] = useState("");
  const [isWaiting, setIsWaiting] = useState(false);
  const [remainingMessages, setRemainingMessages] = useState(85); // Dummy budget indicator (Section 95)
  const [errorText, setErrorText] = useState("");
  
  const scrollRef = useRef<HTMLDivElement>(null);
  const API_BASE = "http://localhost:8787/api/v1";

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollIntoView({ behavior: "smooth" });
    }
  }, [messages, statusMessage]);

  // Handle Cancel Waiting (Section 98)
  const handleCancelWaiting = () => {
    setIsWaiting(false);
    setStatusMessage("");
    setErrorText("Request cancelled by viewer.");
  };

  const handleSend = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputText.trim() || isWaiting) return;

    const userQuery = inputText.trim();
    setInputText("");
    setMessages(prev => [...prev, { role: "user", text: userQuery }]);
    setIsWaiting(true);
    setErrorText("");

    // Cycle through AI Status Messages (Section 97)
    const statusCycle = [
      "Preparing response...",
      "Searching knowledge base...",
      "Contacting AI provider...",
      "Processing model results..."
    ];
    let statusIdx = 0;
    setStatusMessage(statusCycle[0]);
    
    const statusInterval = setInterval(() => {
      statusIdx++;
      if (statusIdx < statusCycle.length) {
        setStatusMessage(statusCycle[statusIdx]);
      }
    }, 2000);

    const token = localStorage.getItem("token");
    try {
      // Build conversation history payload
      const conversationHistory = messages.map(m => ({
        role: m.role,
        text: m.text
      }));

      const response = await fetch(`${API_BASE}/ai/ask`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({ message: userQuery, conversationHistory })
      });

      clearInterval(statusInterval);
      const res = await response.json();

      if (!response.ok || !res.success) {
        throw new Error(res.message || "Failed to retrieve response from AI");
      }

      setMessages(prev => [...prev, { role: "model", text: res.data.answer }]);
      setRemainingMessages(prev => Math.max(0, prev - 1));
    } catch (err: any) {
      clearInterval(statusInterval);
      setErrorText(err.message || "Temporary AI provider connection loss.");
    } finally {
      setIsWaiting(false);
      setStatusMessage("");
    }
  };

  // Helper to format bot markdown content (renders basic bold, lists, paragraphs)
  const renderMessageContent = (text: string) => {
    // Escape standard tags
    const lines = text.split("\n");
    return lines.map((line, index) => {
      let content = line;
      
      // Check for bullet list
      if (content.startsWith("- ") || content.startsWith("* ")) {
        return (
          <li key={index} style={{ marginLeft: "20px", marginBottom: "4px" }}>
            {parseFormatting(content.substring(2))}
          </li>
        );
      }

      // Check for code blocks
      if (content.startsWith("```")) {
        return null; // Simple parser overrides block bounds
      }

      return (
        <p key={index} style={{ marginBottom: "8px", minHeight: "1em" }}>
          {parseFormatting(content)}
        </p>
      );
    });
  };

  const parseFormatting = (text: string) => {
    // Replace **bold**
    const parts = text.split(/(\*\*.*?\*\*)/g);
    return parts.map((part, i) => {
      if (part.startsWith("**") && part.endsWith("**")) {
        return <strong key={i}>{part.slice(2, -2)}</strong>;
      }
      return part;
    });
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", height: "calc(100vh - 120px)" }}>
      {/* Top Banner stats */}
      <div style={{
        display: "flex",
        justifyContent: "space-between",
        alignItems: "center",
        paddingBottom: "16px",
        borderBottom: "1px solid var(--border-glass)",
        marginBottom: "20px"
      }}>
        <div>
          <h2 style={{ fontSize: "24px", fontWeight: 800, fontFamily: "var(--font-display)" }}>AI Companion</h2>
          <p style={{ color: "var(--text-secondary)", fontSize: "13px" }}>RAG Knowledge Processing Engine active</p>
        </div>
        
        {/* Limit tracker indicator */}
        <div style={{ display: "flex", gap: "16px", alignItems: "center" }}>
          <div style={{
            background: "rgba(0, 245, 212, 0.1)",
            border: "1px solid rgba(0, 245, 212, 0.2)",
            borderRadius: "var(--radius-sm)",
            padding: "6px 12px",
            fontSize: "12px",
            color: "var(--secondary)"
          }}>
            Daily limit: <b>{remainingMessages}</b> left
          </div>
        </div>
      </div>

      {/* Main chat log */}
      <div style={{
        flex: 1,
        overflowY: "auto",
        padding: "10px",
        display: "flex",
        flexDirection: "column",
        gap: "20px",
        marginBottom: "20px"
      }}>
        {messages.map((msg, index) => {
          const isModel = msg.role === "model";
          return (
            <div
              key={index}
              style={{
                display: "flex",
                gap: "12px",
                alignItems: "flex-start",
                justifyContent: isModel ? "flex-start" : "flex-end"
              }}
            >
              {isModel && (
                <div style={{
                  background: "var(--primary-gradient)",
                  padding: "8px",
                  borderRadius: "50%",
                  color: "#fff",
                  display: "flex",
                  boxShadow: "0 0 10px var(--primary-glow)"
                }}>
                  <Bot size={18} />
                </div>
              )}
              
              <div className="glass-panel" style={{
                maxWidth: "70%",
                padding: "14px 18px",
                borderRadius: isModel ? "0px 18px 18px 18px" : "18px 0px 18px 18px",
                background: isModel ? "rgba(18, 20, 32, 0.6)" : "rgba(155, 93, 229, 0.1)",
                borderColor: isModel ? "var(--border-glass)" : "rgba(155, 93, 229, 0.3)"
              }}>
                {renderMessageContent(msg.text)}
              </div>

              {!isModel && (
                <div style={{
                  background: "rgba(255,255,255,0.05)",
                  padding: "8px",
                  borderRadius: "50%",
                  color: "var(--text-secondary)",
                  display: "flex",
                  border: "1px solid var(--border-glass)"
                }}>
                  <User size={18} />
                </div>
              )}
            </div>
          );
        })}

        {/* AI Status / Loading indicators */}
        {isWaiting && statusMessage && (
          <div style={{ display: "flex", gap: "12px", alignItems: "center" }}>
            <div style={{
              background: "var(--primary-gradient)",
              padding: "8px",
              borderRadius: "50%",
              color: "#fff",
              display: "flex"
            }}>
              <RefreshCw size={18} className="animate-spin" />
            </div>
            
            <div className="glass-panel" style={{
              padding: "12px 18px",
              borderRadius: "0px 18px 18px 18px",
              fontSize: "14px",
              color: "var(--text-secondary)",
              display: "flex",
              alignItems: "center",
              gap: "12px"
            }}>
              <span>{statusMessage}</span>
              
              {/* Cancel Waiting option (Section 98) */}
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
                  fontWeight: 500
                }}
              >
                <XCircle size={14} />
                Cancel
              </button>
            </div>
          </div>
        )}

        {/* Error response with retry prompt */}
        {errorText && (
          <div style={{ display: "flex", gap: "12px", alignItems: "center", alignSelf: "center" }}>
            <div style={{
              background: "rgba(239, 68, 68, 0.1)",
              border: "1px solid rgba(239, 68, 68, 0.2)",
              borderRadius: "var(--radius-md)",
              padding: "10px 16px",
              fontSize: "13px",
              color: "#f87171",
              display: "flex",
              alignItems: "center",
              gap: "8px"
            }}>
              <AlertCircle size={16} />
              <span>{errorText}</span>
            </div>
          </div>
        )}

        <div ref={scrollRef} />
      </div>

      {/* Input container footer */}
      <form onSubmit={handleSend} style={{ display: "flex", gap: "12px" }}>
        <input
          type="text"
          placeholder="Ask a question about the owner's profile..."
          className="form-input"
          style={{ flex: 1, borderRadius: "var(--radius-md)" }}
          value={inputText}
          onChange={(e) => setInputText(e.target.value)}
          disabled={isWaiting}
        />
        <button
          type="submit"
          className="btn-primary"
          style={{ padding: "14px 20px" }}
          disabled={isWaiting || !inputText.trim()}
        >
          <Send size={18} />
        </button>
      </form>
    </div>
  );
};
export default AiPage;
