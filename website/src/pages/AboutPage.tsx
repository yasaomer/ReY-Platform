import React from "react";
import { Info, User, Shield, Cpu, ExternalLink, Heart } from "lucide-react";

export const AboutPage: React.FC = () => {
  return (
    <div style={{ maxWidth: "800px", margin: "0 auto", paddingBottom: "40px" }}>
      {/* Page Header */}
      <div style={{ marginBottom: "40px", textAlign: "center" }}>
        <div style={{
          width: "72px", height: "72px",
          background: "var(--primary-gradient)",
          borderRadius: "50%",
          display: "flex", alignItems: "center", justifyContent: "center",
          margin: "0 auto 16px",
          boxShadow: "0 0 30px var(--primary-glow)"
        }}>
          <Info size={32} color="#fff" />
        </div>
        <h1 style={{ fontSize: "36px", fontWeight: 800, fontFamily: "var(--font-display)", marginBottom: "8px" }}>
          About ReY
        </h1>
        <p style={{ color: "var(--text-secondary)", fontSize: "16px" }}>
          A premium personal digital space and secure memory archive.
        </p>
      </div>

      {/* Purpose Section */}
      <section className="glass-panel" style={{ marginBottom: "32px", padding: "32px" }}>
        <h2 style={{ fontSize: "20px", fontWeight: 700, marginBottom: "16px", display: "flex", alignItems: "center", gap: "10px", color: "var(--primary)" }}>
          <Shield size={20} />
          Ecosystem Purpose
        </h2>
        <p style={{ color: "var(--text-primary)", lineHeight: 1.8, fontSize: "15px" }}>
          ReY is a custom, end-to-end synchronized ecosystem designed to serve as a secure personal memory vault, live positioning module, and administrative controller. Every detail—from local folder monitoring on the Android APK to Cloudflare serverless handlers and real-time database synchronizations—is engineered to preserve, process, and present personal knowledge under complete user ownership.
        </p>
      </section>

      {/* Owner Profile Grid */}
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "24px", marginBottom: "32px" }} className="location-grid-layout">
        <section className="glass-panel" style={{ padding: "32px" }}>
          <h2 style={{ fontSize: "20px", fontWeight: 700, marginBottom: "16px", display: "flex", alignItems: "center", gap: "10px", color: "var(--secondary)" }}>
            <User size={20} />
            The Owner
          </h2>
          <div style={{ display: "flex", flexDirection: "column", gap: "12px", fontSize: "14px" }}>
            <div>
              <span style={{ color: "var(--text-secondary)", display: "block", fontSize: "11px", textTransform: "uppercase", fontWeight: 700 }}>Name</span>
              <span style={{ fontSize: "15px", fontWeight: 600 }}>Yasa Omer (ReY)</span>
            </div>
            <div>
              <span style={{ color: "var(--text-secondary)", display: "block", fontSize: "11px", textTransform: "uppercase", fontWeight: 700 }}>Role</span>
              <span style={{ fontSize: "15px", fontWeight: 600 }}>Software Engineer & Creator</span>
            </div>
            <div>
              <span style={{ color: "var(--text-secondary)", display: "block", fontSize: "11px", textTransform: "uppercase", fontWeight: 700 }}>Focus</span>
              <span style={{ fontSize: "15px", fontWeight: 600 }}>Advanced Agentic AI, Secure Systems</span>
            </div>
          </div>
        </section>

        <section className="glass-panel" style={{ padding: "32px" }}>
          <h2 style={{ fontSize: "20px", fontWeight: 700, marginBottom: "16px", display: "flex", alignItems: "center", gap: "10px", color: "#00bbf9" }}>
            <Cpu size={20} />
            Ecosystem Core
          </h2>
          <div style={{ display: "flex", flexDirection: "column", gap: "12px", fontSize: "14px" }}>
            <div>
              <span style={{ color: "var(--text-secondary)", display: "block", fontSize: "11px", textTransform: "uppercase", fontWeight: 700 }}>Platform</span>
              <span style={{ fontSize: "15px", fontWeight: 600 }}>React, Hono, SQLite D1, Cloudflare KV</span>
            </div>
            <div>
              <span style={{ color: "var(--text-secondary)", display: "block", fontSize: "11px", textTransform: "uppercase", fontWeight: 700 }}>Android Client</span>
              <span style={{ fontSize: "15px", fontWeight: 600 }}>Kotlin Jetpack Compose Private OML Admin</span>
            </div>
            <div>
              <span style={{ color: "var(--text-secondary)", display: "block", fontSize: "11px", textTransform: "uppercase", fontWeight: 700 }}>Intelligence</span>
              <span style={{ fontSize: "15px", fontWeight: 600 }}>Gemini AI Vector-Embedded RAG</span>
            </div>
          </div>
        </section>
      </div>

      {/* Tech Specifications */}
      <section className="glass-panel" style={{ marginBottom: "32px", padding: "32px" }}>
        <h2 style={{ fontSize: "20px", fontWeight: 700, marginBottom: "16px", display: "flex", alignItems: "center", gap: "10px" }}>
          System Features
        </h2>
        <ul style={{ color: "var(--text-primary)", lineHeight: 1.8, fontSize: "15px", paddingLeft: "20px", display: "flex", flexDirection: "column", gap: "10px" }}>
          <li><strong>Live Tracking</strong>: Dynamic geolocation updates, fetched securely on-demand from the Android client with zero constant background drainage.</li>
          <li><strong>Ecosystem Synchronization</strong>: Fault-tolerant SQLite sync queue engine supporting priorities, retries, and offline queuing.</li>
          <li><strong>Intelligent Assistant</strong>: Vector-embedded knowledge base documents powering custom Gemini conversations matching the owner's tone and style.</li>
          <li><strong>Media Gallery</strong>: Encrypted background Google Drive triggers with Cloudflare R2 thumbnail rendering.</li>
        </ul>
      </section>

      {/* Footer info */}
      <div style={{ textAlign: "center", marginTop: "48px", display: "flex", flexDirection: "column", alignItems: "center", gap: "8px" }}>
        <p style={{ color: "var(--text-secondary)", fontSize: "13px", display: "flex", alignItems: "center", gap: "6px" }}>
          Built with <Heart size={13} color="#FF5252" /> by Yasa Omer • © 2026 ReY Platform
        </p>
        <a href="https://github.com/yasaomer/ReY-Platform" target="_blank" rel="noopener noreferrer" style={{
          color: "var(--primary)", fontSize: "12px", display: "flex", alignItems: "center", gap: "4px", textDecoration: "none", fontWeight: 600
        }}>
          Repository Source <ExternalLink size={12} />
        </a>
      </div>
    </div>
  );
};

export default AboutPage;
