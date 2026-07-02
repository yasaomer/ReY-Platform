import React, { useState, useEffect } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { Home, Image, MessageSquare, MapPin, Share2, Bot, LogOut, RefreshCw } from "lucide-react";
import { API_BASE } from "../config";

interface AppLayoutProps {
  children: React.ReactNode;
}

export const AppLayout: React.FC<AppLayoutProps> = ({ children }) => {
  const location = useLocation();
  const navigate = useNavigate();
  const [isMobile, setIsMobile] = useState(window.innerWidth < 768);

  useEffect(() => {
    const handleResize = () => {
      setIsMobile(window.innerWidth < 768);
    };
    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, []);

  const navItems = [
    { path: "/", label: "Home", icon: Home },
    { path: "/gallery", label: "Gallery", icon: Image },
    { path: "/last-message", label: "Last Message", icon: MessageSquare },
    { path: "/location", label: "Location", icon: MapPin },
    { path: "/social", label: "Social Media", icon: Share2 },
    { path: "/ai", label: "AI Assistant", icon: Bot },
    { path: "/sync", label: "Sync Engine", icon: RefreshCw },
  ];

  const handleLogout = async () => {
    const token = localStorage.getItem("token");
    if (token) {
      try {
        await fetch(`${API_BASE}/auth/logout`, {
          method: "POST",
          headers: {
            Authorization: `Bearer ${token}`
          }
        });
      } catch (e) {
        console.error("Logout failed", e);
      }
    }
    localStorage.removeItem("token");
    localStorage.removeItem("username");
    navigate("/login");
  };

  const isActive = (path: string) => {
    if (path === "/") {
      return location.pathname === "/";
    }
    return location.pathname.startsWith(path);
  };

  return (
    <div style={{ display: "flex", minHeight: "100vh", background: "var(--bg-base)" }}>
      {/* 1. Desktop Sidebar */}
      {!isMobile && (
        <aside style={{
          width: "260px",
          borderRight: "1px solid var(--border-glass)",
          background: "rgba(18, 20, 32, 0.4)",
          backdropFilter: "blur(20px)",
          display: "flex",
          flexDirection: "column",
          padding: "24px 16px",
          position: "sticky",
          top: 0,
          height: "100vh"
        }}>
          {/* Logo */}
          <div style={{ display: "flex", alignItems: "center", gap: "12px", marginBottom: "40px", padding: "0 8px" }}>
            <div style={{
              width: "36px",
              height: "36px",
              background: "var(--primary-gradient)",
              borderRadius: "50%",
              boxShadow: "0 0 15px var(--primary-glow)"
            }} />
            <div>
              <h1 style={{ fontSize: "20px", fontWeight: 700, fontFamily: "var(--font-display)" }}>ReY</h1>
              <p style={{ fontSize: "11px", color: "var(--text-muted)", marginTop: "-2px" }}>Memory Vault</p>
            </div>
          </div>

          {/* Nav list */}
          <nav style={{ display: "flex", flexDirection: "column", gap: "8px", flex: 1 }}>
            {navItems.map((item) => {
              const Icon = item.icon;
              const active = isActive(item.path);
              return (
                <Link
                  key={item.path}
                  to={item.path}
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "16px",
                    padding: "12px 16px",
                    borderRadius: "var(--radius-md)",
                    textDecoration: "none",
                    color: active ? "var(--text-primary)" : "var(--text-secondary)",
                    background: active ? "rgba(155, 93, 229, 0.15)" : "transparent",
                    border: active ? "1px solid rgba(155, 93, 229, 0.3)" : "1px solid transparent",
                    fontFamily: "var(--font-display)",
                    fontWeight: active ? 500 : 400,
                    transition: "var(--transition-fast)"
                  }}
                >
                  <Icon size={20} style={{ color: active ? "var(--primary)" : "inherit" }} />
                  {item.label}
                </Link>
              );
            })}
          </nav>

          {/* User profile footer */}
          <div style={{
            borderTop: "1px solid var(--border-glass)",
            paddingTop: "16px",
            marginTop: "auto"
          }}>
            <button
              onClick={handleLogout}
              style={{
                width: "100%",
                display: "flex",
                alignItems: "center",
                gap: "16px",
                padding: "12px 16px",
                borderRadius: "var(--radius-md)",
                border: "none",
                background: "transparent",
                color: "var(--text-secondary)",
                fontFamily: "var(--font-display)",
                textAlign: "left",
                cursor: "pointer",
                transition: "var(--transition-fast)"
              }}
              onMouseEnter={(e) => (e.currentTarget.style.background = "rgba(255, 255, 255, 0.05)")}
              onMouseLeave={(e) => (e.currentTarget.style.background = "transparent")}
            >
              <LogOut size={20} />
              Log Out
            </button>
          </div>
        </aside>
      )}

      {/* 2. Mobile Bottom Bar / Top Header */}
      {isMobile && (
        <div style={{ width: "100%", display: "flex", flexDirection: "column" }}>
          {/* Header */}
          <header style={{
            height: "64px",
            borderBottom: "1px solid var(--border-glass)",
            background: "rgba(18, 20, 32, 0.8)",
            backdropFilter: "blur(20px)",
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            padding: "0 20px",
            position: "sticky",
            top: 0,
            zIndex: 1000
          }}>
            <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
              <div style={{ width: "28px", height: "28px", background: "var(--primary-gradient)", borderRadius: "50%" }} />
              <h1 style={{ fontSize: "18px", fontWeight: 700 }}>ReY</h1>
            </div>
            
            <button
              onClick={handleLogout}
              style={{
                border: "none",
                background: "transparent",
                color: "var(--text-secondary)",
                cursor: "pointer"
              }}
            >
              <LogOut size={22} />
            </button>
          </header>
        </div>
      )}

      {/* Main Content Area */}
      <main style={{
        flex: 1,
        padding: isMobile ? "24px 16px 80px 16px" : "40px",
        overflowY: "auto",
        maxHeight: "100vh"
      }}>
        <div style={{ maxWidth: "1100px", margin: "0 auto" }}>
          {children}
        </div>
      </main>

      {/* Mobile Navigation bar at the bottom */}
      {isMobile && (
        <nav style={{
          position: "fixed",
          bottom: 0,
          left: 0,
          right: 0,
          height: "64px",
          background: "rgba(18, 20, 32, 0.95)",
          backdropFilter: "blur(20px)",
          borderTop: "1px solid var(--border-glass)",
          display: "flex",
          justifyContent: "space-around",
          alignItems: "center",
          zIndex: 1000
        }}>
          {navItems.map((item) => {
            const Icon = item.icon;
            const active = isActive(item.path);
            return (
              <Link
                key={item.path}
                to={item.path}
                style={{
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center",
                  gap: "4px",
                  color: active ? "var(--primary)" : "var(--text-secondary)",
                  textDecoration: "none",
                  fontSize: "10px",
                  fontWeight: active ? 600 : 400
                }}
              >
                <Icon size={20} />
                <span>{item.label.split(" ")[0]}</span>
              </Link>
            );
          })}
        </nav>
      )}
    </div>
  );
};
export default AppLayout;
