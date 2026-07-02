import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  MapPin, RefreshCw, Navigation, Compass, AlertCircle,
  Wifi, WifiOff, CheckCircle, Clock, Loader,
  Copy, Check, ExternalLink
} from "lucide-react";
import { API_BASE } from "../config";

// ──────────────────────────────────────────────────────────
//  TYPES
// ──────────────────────────────────────────────────────────
interface LocationData {
  latitude: number;
  longitude: number;
  accuracy: number;
  altitude: number | null;
  speed: number | null;
  heading: number | null;
  address: string | null;
  provider: string;
  timestamp: string;
}

type RefreshPhase =
  | "idle" | "sending" | "waiting" | "getting_gps"
  | "validating" | "uploading" | "done" | "timeout" | "offline";

const PHASE_LABELS: Record<RefreshPhase, string> = {
  idle:         "Ready to refresh",
  sending:      "Sending request to device…",
  waiting:      "Waiting for device response…",
  getting_gps:  "Device acquiring GPS fix…",
  validating:   "Validating coordinates…",
  uploading:    "Uploading to server…",
  done:         "Location updated successfully!",
  timeout:      "Request timed out — device may be offline",
  offline:      "Backend unreachable. Check connection.",
};

const PHASE_COLORS: Record<RefreshPhase, string> = {
  idle:         "var(--text-secondary)",
  sending:      "var(--primary)",
  waiting:      "var(--primary)",
  getting_gps:  "var(--secondary)",
  validating:   "var(--secondary)",
  uploading:    "var(--secondary)",
  done:         "#00E676",
  timeout:      "#FFD166",
  offline:      "#FF5252",
};

// ──────────────────────────────────────────────────────────
//  COMPONENT
// ──────────────────────────────────────────────────────────
export const LocationPage: React.FC = () => {
  const [location, setLocation]       = useState<LocationData | null>(null);
  const [isLoading, setIsLoading]     = useState(true);
  const [phase, setPhase]             = useState<RefreshPhase>("idle");
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [copied, setCopied]           = useState(false);
  const [mapLayer, setMapLayer]       = useState<"normal" | "satellite" | "terrain">("normal");
  const [online, setOnline]           = useState(navigator.onLine);
  const mapRef                        = useRef<any>(null);
  const markerRef                     = useRef<any>(null);
  const pollRef                       = useRef<ReturnType<typeof setInterval> | null>(null);

  // ── Network status listener ────────────────────────────
  useEffect(() => {
    const onOnline  = () => setOnline(true);
    const onOffline = () => setOnline(false);
    window.addEventListener("online",  onOnline);
    window.addEventListener("offline", onOffline);
    return () => {
      window.removeEventListener("online",  onOnline);
      window.removeEventListener("offline", onOffline);
    };
  }, []);

  // ── Initial fetch ──────────────────────────────────────
  const fetchLocation = useCallback(async () => {
    const token = localStorage.getItem("token");
    try {
      const res  = await fetch(`${API_BASE}/location/current`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const data = await res.json();
      if (data.success && data.data) setLocation(data.data);
    } catch { /* offline */ }
    finally   { setIsLoading(false); }
  }, []);

  useEffect(() => { fetchLocation(); }, [fetchLocation]);

  // ── Map renderer ───────────────────────────────────────
  const TILE_URLS: Record<string, string> = {
    normal:    "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
    satellite: "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}",
    terrain:   "https://stamen-tiles.a.ssl.fastly.net/terrain/{z}/{x}/{y}.jpg",
  };

  useEffect(() => {
    const L = (window as any).L;
    if (!location || !L) return;

    if (mapRef.current) {
      mapRef.current.remove();
      mapRef.current = null;
      markerRef.current = null;
    }

    try {
      const map = L.map("loc-map-container", {
        zoomControl: true,
        attributionControl: false,
      }).setView([location.latitude, location.longitude], 15);

      L.tileLayer(TILE_URLS[mapLayer], { maxZoom: 19 }).addTo(map);

      // Accuracy circle
      L.circle([location.latitude, location.longitude], {
        radius: location.accuracy,
        color: "var(--primary)",
        fillColor: "rgba(155,93,229,0.08)",
        fillOpacity: 1,
        weight: 1.5,
      }).addTo(map);

      // Pulsing marker
      const icon = L.divIcon({
        className: "",
        html: `
          <div style="position:relative;width:24px;height:24px;">
            <div style="
              position:absolute;inset:0;border-radius:50%;
              background:var(--primary);opacity:0.25;
              animation:loc-pulse 1.8s ease-out infinite;
            "></div>
            <div style="
              position:absolute;inset:4px;border-radius:50%;
              background:var(--primary);border:2.5px solid #fff;
              box-shadow:0 0 14px var(--primary);
            "></div>
          </div>`,
        iconSize: [24, 24],
        iconAnchor: [12, 12],
      });

      const marker = L.marker([location.latitude, location.longitude], { icon })
        .addTo(map)
        .bindPopup(`<b>Accuracy:</b> ±${Math.round(location.accuracy)} m<br/><b>Provider:</b> ${location.provider}`);

      mapRef.current   = map;
      markerRef.current = marker;
    } catch (err) {
      console.error("Map render error", err);
    }

    return () => {
      if (mapRef.current) { mapRef.current.remove(); mapRef.current = null; }
    };
  }, [location, mapLayer]);

  // ── Refresh handler ────────────────────────────────────
  const handleRefresh = async () => {
    if (isRefreshing) return;
    if (pollRef.current) clearInterval(pollRef.current);

    setIsRefreshing(true);
    setPhase("sending");
    const token = localStorage.getItem("token");

    try {
      const res = await fetch(`${API_BASE}/location/refresh`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) throw new Error("Backend offline");
      const data = await res.json();
      if (!data.success) throw new Error(data.message || "Failed to queue refresh");
    } catch {
      setPhase("offline");
      setIsRefreshing(false);
      setTimeout(() => setPhase("idle"), 5000);
      return;
    }

    setPhase("waiting");
    const initialTs = location ? new Date(location.timestamp).getTime() : 0;
    let attempts    = 0;

    // Simulate phase progression
    const phases: RefreshPhase[] = ["waiting", "getting_gps", "validating", "uploading"];
    let pi = 0;

    pollRef.current = setInterval(async () => {
      attempts++;

      // Advance phase visually
      if (pi < phases.length - 1 && attempts % 3 === 0) pi++;
      setPhase(phases[Math.min(pi, phases.length - 1)]);

      try {
        const r    = await fetch(`${API_BASE}/location/current`, {
          headers: { Authorization: `Bearer ${token}` },
        });
        const json = await r.json();
        if (json.success && json.data) {
          const newTs = new Date(json.data.timestamp).getTime();
          if (newTs > initialTs) {
            setLocation(json.data);
            setPhase("done");
            clearInterval(pollRef.current!);
            setIsRefreshing(false);
            setTimeout(() => setPhase("idle"), 3000);
            return;
          }
        }
      } catch { /* ignore poll errors */ }

      if (attempts >= 10) {
        clearInterval(pollRef.current!);
        setPhase("timeout");
        setIsRefreshing(false);
        setTimeout(() => setPhase("idle"), 5000);
      }
    }, 3000);
  };

  // Cleanup interval on unmount
  useEffect(() => () => { if (pollRef.current) clearInterval(pollRef.current); }, []);

  // ── Copy coords ────────────────────────────────────────
  const handleCopy = () => {
    if (!location) return;
    navigator.clipboard.writeText(`${location.latitude.toFixed(6)}, ${location.longitude.toFixed(6)}`);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  // ── Render ─────────────────────────────────────────────
  return (
    <div>
      {/* ── HEADER ────────────────────────────────────── */}
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: "32px", flexWrap: "wrap", gap: "16px" }}>
        <div>
          <div style={{ display: "flex", alignItems: "center", gap: "12px", marginBottom: "6px" }}>
            <MapPin size={28} color="var(--primary)" />
            <h1 style={{ fontSize: "32px", fontWeight: 800, fontFamily: "var(--font-display)", margin: 0 }}>
              Live Location
            </h1>
          </div>
          <p style={{ color: "var(--text-secondary)", fontSize: "14px", maxWidth: "480px" }}>
            View the owner's GPS position. Press refresh to request an updated fix from their device.
          </p>
        </div>

        <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
          {/* Online indicator */}
          <div style={{
            display: "flex", alignItems: "center", gap: "6px",
            background: online ? "rgba(0,230,118,0.1)" : "rgba(255,82,82,0.1)",
            border: `1px solid ${online ? "rgba(0,230,118,0.3)" : "rgba(255,82,82,0.3)"}`,
            borderRadius: "var(--radius-md)", padding: "8px 12px",
            fontSize: "12px", fontWeight: 700,
            color: online ? "#00E676" : "#FF5252"
          }}>
            {online ? <Wifi size={13}/> : <WifiOff size={13}/>}
            {online ? "Connected" : "Offline"}
          </div>

          {/* Refresh button */}
          <button
            onClick={handleRefresh}
            disabled={isRefreshing || isLoading}
            className="btn-primary"
            style={{ display: "flex", alignItems: "center", gap: "8px", padding: "10px 20px", fontSize: "14px" }}
          >
            <RefreshCw size={16} style={{
              animation: isRefreshing ? "loc-spin 0.9s linear infinite" : "none"
            }}/>
            {isRefreshing ? "Refreshing…" : "Refresh Location"}
          </button>
        </div>
      </div>

      {/* ── PHASE STATUS BAR ──────────────────────────── */}
      {phase !== "idle" && (
        <div
          className="glass-panel"
          style={{
            marginBottom: "24px",
            padding: "14px 20px",
            display: "flex", alignItems: "center", gap: "12px",
            borderLeft: `3px solid ${PHASE_COLORS[phase]}`,
          }}
        >
          {phase === "done"
            ? <CheckCircle size={18} color="#00E676"/>
            : phase === "timeout" || phase === "offline"
              ? <AlertCircle size={18} color={PHASE_COLORS[phase]}/>
              : <Loader size={18} color={PHASE_COLORS[phase]} style={{ animation: "loc-spin 1s linear infinite" }}/>
          }
          <span style={{ color: PHASE_COLORS[phase], fontWeight: 600, fontSize: "14px" }}>
            {PHASE_LABELS[phase]}
          </span>

          {/* Progress steps */}
          {isRefreshing && (
            <div style={{ marginLeft: "auto", display: "flex", gap: "6px" }}>
              {(["sending","waiting","getting_gps","validating","uploading","done"] as RefreshPhase[]).map(p => (
                <div key={p} style={{
                  width: "6px", height: "6px", borderRadius: "50%",
                  background: p === phase ? "var(--primary)" : "rgba(255,255,255,0.15)",
                  transition: "background 0.3s"
                }}/>
              ))}
            </div>
          )}
        </div>
      )}

      {/* ── LOADING ───────────────────────────────────── */}
      {isLoading ? (
        <div style={{ display: "grid", gridTemplateColumns: "1fr", gap: "24px" }}>
          <div className="glass-panel shimmer-placeholder" style={{ height: "480px", borderRadius: "var(--radius-lg)" }} />
        </div>
      ) : !location ? (
        <div className="glass-panel" style={{ textAlign: "center", padding: "100px 24px" }}>
          <MapPin size={52} color="var(--text-muted)" style={{ marginBottom: "20px" }} />
          <h3 style={{ fontSize: "20px", marginBottom: "10px", fontWeight: 700 }}>No Location Available</h3>
          <p style={{ color: "var(--text-secondary)", fontSize: "14px", maxWidth: "360px", margin: "0 auto 24px" }}>
            No GPS data has been received yet. Press "Refresh Location" to request a fix from the owner's device.
          </p>
          <button className="btn-primary" onClick={handleRefresh} disabled={isRefreshing}
            style={{ display: "inline-flex", alignItems: "center", gap: "8px" }}>
            <RefreshCw size={15}/> Request Location
          </button>
        </div>
      ) : (
        <div style={{ display: "grid", gridTemplateColumns: "1fr 340px", gap: "24px", alignItems: "start" }} className="location-grid-layout">

          {/* ── MAP ───────────────────────────────────── */}
          <div className="glass-panel" style={{ padding: 0, overflow: "hidden", borderRadius: "var(--radius-lg)" }}>
            {/* Layer switcher */}
            <div style={{
              display: "flex", gap: "8px", padding: "12px 16px",
              borderBottom: "1px solid var(--border-glass)",
              background: "rgba(255,255,255,0.02)"
            }}>
              {(["normal","satellite","terrain"] as const).map(layer => (
                <button key={layer} onClick={() => setMapLayer(layer)} style={{
                  background: mapLayer === layer ? "var(--primary)" : "rgba(255,255,255,0.06)",
                  border: "none", borderRadius: "6px",
                  color: mapLayer === layer ? "#000" : "var(--text-secondary)",
                  padding: "5px 12px", fontSize: "11px", fontWeight: 700,
                  cursor: "pointer", textTransform: "capitalize", transition: "all 0.2s"
                }}>
                  {layer}
                </button>
              ))}
            </div>

            {/* Map canvas */}
            <div id="loc-map-container" style={{ width: "100%", height: "420px" }} />

            {/* Bottom bar */}
            <div style={{
              padding: "10px 16px",
              display: "flex", justifyContent: "space-between", alignItems: "center",
              borderTop: "1px solid var(--border-glass)",
              background: "rgba(255,255,255,0.02)"
            }}>
              <span style={{ fontSize: "11px", color: "var(--text-muted)" }}>
                ± {Math.round(location.accuracy)} m accuracy circle shown
              </span>
              <a
                href={`https://maps.google.com/maps?q=${location.latitude},${location.longitude}`}
                target="_blank"
                rel="noopener noreferrer"
                style={{
                  display: "flex", alignItems: "center", gap: "5px",
                  color: "var(--primary)", fontSize: "12px",
                  textDecoration: "none", fontWeight: 600
                }}
              >
                <ExternalLink size={12}/> Open in Google Maps
              </a>
            </div>
          </div>

          {/* ── SIDEBAR ───────────────────────────────── */}
          <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>

            {/* Coordinates card */}
            <div className="glass-panel">
              <h3 style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "16px", fontWeight: 700, marginBottom: "18px" }}>
                <Navigation size={18} color="var(--primary)"/>
                Coordinates
              </h3>
              <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
                <InfoRow label="Latitude"  value={location.latitude.toFixed(6)}/>
                <InfoRow label="Longitude" value={location.longitude.toFixed(6)}/>
                <InfoRow label="Accuracy"  value={`± ${Math.round(location.accuracy)} m`}/>
                {location.altitude !== null && (
                  <InfoRow label="Altitude" value={`${Math.round(location.altitude)} m`}/>
                )}
              </div>
              <button onClick={handleCopy} style={{
                marginTop: "16px", width: "100%",
                display: "flex", alignItems: "center", justifyContent: "center", gap: "6px",
                background: copied ? "rgba(0,230,118,0.1)" : "rgba(255,255,255,0.04)",
                border: `1px solid ${copied ? "rgba(0,230,118,0.4)" : "var(--border-glass)"}`,
                borderRadius: "var(--radius-md)", padding: "9px",
                color: copied ? "#00E676" : "var(--text-secondary)",
                cursor: "pointer", fontSize: "13px", fontWeight: 600,
                transition: "all 0.2s"
              }}>
                {copied ? <Check size={14}/> : <Copy size={14}/>}
                {copied ? "Copied!" : "Copy Coordinates"}
              </button>
            </div>

            {/* Telemetry card */}
            <div className="glass-panel">
              <h3 style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "16px", fontWeight: 700, marginBottom: "18px" }}>
                <Compass size={18} color="var(--secondary)"/>
                Telemetry
              </h3>
              <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
                <InfoRow label="Speed"
                  value={location.speed ? `${(location.speed * 3.6).toFixed(1)} km/h` : "Stationary"}/>
                <InfoRow label="Heading"
                  value={location.heading ? `${Math.round(location.heading)}°` : "—"}/>
                <InfoRow label="Provider"  value={location.provider.toUpperCase()}/>
                {location.address && (
                  <InfoRow label="Address" value={location.address}/>
                )}
              </div>
            </div>

            {/* Updated card */}
            <div className="glass-panel">
              <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "12px" }}>
                <Clock size={16} color="var(--text-muted)"/>
                <span style={{ fontSize: "12px", color: "var(--text-muted)", fontWeight: 700, textTransform: "uppercase", letterSpacing: "0.08em" }}>
                  Last Updated
                </span>
              </div>
              <p style={{ fontWeight: 600, fontSize: "14px" }}>
                {new Date(location.timestamp).toLocaleString()}
              </p>
              <p style={{ color: "var(--text-secondary)", fontSize: "12px", marginTop: "4px" }}>
                {timeSince(location.timestamp)} ago
              </p>
            </div>

          </div>
        </div>
      )}

      {/* Inject CSS keyframes */}
      <style>{`
        @keyframes loc-pulse {
          0%   { transform: scale(1);   opacity: 0.6; }
          100% { transform: scale(3.5); opacity: 0; }
        }
        @keyframes loc-spin {
          to { transform: rotate(360deg); }
        }
        @media (max-width: 860px) {
          .location-grid-layout {
            grid-template-columns: 1fr !important;
          }
        }
      `}</style>
    </div>
  );
};

// ──────────────────────────────────────────────────────────
//  HELPER COMPONENTS
// ──────────────────────────────────────────────────────────

const InfoRow: React.FC<{ label: string; value: string }> = ({ label, value }) => (
  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: "8px" }}>
    <span style={{ fontSize: "12px", color: "var(--text-secondary)", flexShrink: 0, width: "72px" }}>{label}</span>
    <span style={{ fontSize: "13px", fontWeight: 600, fontFamily: "monospace", textAlign: "right" }}>{value}</span>
  </div>
);

function timeSince(isoString: string): string {
  const diff = Date.now() - new Date(isoString).getTime();
  const m    = Math.floor(diff / 60000);
  if (m < 1)  return "less than a minute";
  if (m < 60) return `${m} min`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h} hr`;
  return `${Math.floor(h / 24)} d`;
}

export default LocationPage;
