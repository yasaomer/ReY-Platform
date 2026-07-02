import React, { useState, useEffect, useRef } from "react";
import { MapPin, RefreshCw, Navigation, Compass, AlertCircle, Calendar } from "lucide-react";

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

export const LocationPage: React.FC = () => {
  const [location, setLocation] = useState<LocationData | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [refreshMessage, setRefreshMessage] = useState("");
  const mapRef = useRef<any>(null);
  const API_BASE = "http://localhost:8787/api/v1";

  const fetchLocation = async () => {
    const token = localStorage.getItem("token");
    try {
      const response = await fetch(`${API_BASE}/location/current`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      const res = await response.json();
      if (res.success) {
        setLocation(res.data);
      }
    } catch (e) {
      console.error("Failed to load location coordinates", e);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchLocation();
  }, []);

  // Map renderer
  useEffect(() => {
    const L = (window as any).L;
    if (!location || !L) return;

    // Clean up existing map
    if (mapRef.current) {
      mapRef.current.remove();
      mapRef.current = null;
    }

    try {
      const map = L.map("map-container", {
        zoomControl: true,
        attributionControl: false
      }).setView([location.latitude, location.longitude], 15);

      L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        maxZoom: 19
      }).addTo(map);

      // Custom marker icon
      const myIcon = L.divIcon({
        className: "custom-div-icon",
        html: `<div style="
          width: 20px; 
          height: 20px; 
          background: var(--primary); 
          border: 3px solid #fff; 
          border-radius: 50%;
          box-shadow: 0 0 15px var(--primary-glow);
        "></div>`,
        iconSize: [20, 20],
        iconAnchor: [10, 10]
      });

      L.marker([location.latitude, location.longitude], { icon: myIcon })
        .addTo(map)
        .bindPopup(`<b>Accuracy:</b> ${Math.round(location.accuracy)} meters`)
        .openPopup();

      mapRef.current = map;
    } catch (err) {
      console.error("Failed to render Leaflet map", err);
    }

    return () => {
      if (mapRef.current) {
        mapRef.current.remove();
        mapRef.current = null;
      }
    };
  }, [location]);

  // Request refreshed location from APK
  const handleRefresh = async () => {
    setIsRefreshing(true);
    setRefreshMessage("Sending location refresh command to owner's device...");
    const token = localStorage.getItem("token");

    try {
      const response = await fetch(`${API_BASE}/location/refresh`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` }
      });
      const res = await response.json();
      
      if (!response.ok || !res.success) {
        throw new Error(res.message || "Failed to trigger refresh");
      }

      // Start polling for coordinates updates
      let attempts = 0;
      const initialTime = location ? new Date(location.timestamp).getTime() : 0;
      
      const interval = setInterval(async () => {
        attempts++;
        setRefreshMessage(`Waiting for update... (Attempt ${attempts}/10)`);
        
        try {
          const pollRes = await fetch(`${API_BASE}/location/current`, {
            headers: { Authorization: `Bearer ${token}` }
          });
          const pollJson = await pollRes.json();
          
          if (pollJson.success) {
            const newTime = new Date(pollJson.data.timestamp).getTime();
            if (newTime > initialTime) {
              setLocation(pollJson.data);
              setRefreshMessage("Location updated successfully!");
              clearInterval(interval);
              setIsRefreshing(false);
              setTimeout(() => setRefreshMessage(""), 3000);
            }
          }
        } catch (e) {
          console.error("Polling error", e);
        }

        if (attempts >= 10) {
          clearInterval(interval);
          setRefreshMessage("Update request timed out. Device might be offline.");
          setIsRefreshing(false);
          setTimeout(() => setRefreshMessage(""), 5000);
        }
      }, 3000);

    } catch (err: any) {
      setRefreshMessage(err.message);
      setIsRefreshing(false);
      setTimeout(() => setRefreshMessage(""), 5000);
    }
  };

  return (
    <div>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", marginBottom: "32px" }}>
        <div>
          <h2 style={{ fontSize: "28px", fontWeight: 800, fontFamily: "var(--font-display)" }}>Live Location</h2>
          <p style={{ color: "var(--text-secondary)", fontSize: "14px", marginTop: "4px" }}>View the owner's approximate coordinate location</p>
        </div>
        <button
          onClick={handleRefresh}
          className="btn-primary"
          style={{ fontSize: "14px", padding: "10px 20px" }}
          disabled={isRefreshing || isLoading}
        >
          <RefreshCw size={16} className={isRefreshing ? "animate-spin" : ""} />
          {isRefreshing ? "Refreshing..." : "Refresh Location"}
        </button>
      </div>

      {refreshMessage && (
        <div style={{
          background: "rgba(155, 93, 229, 0.1)",
          border: "1px solid rgba(155, 93, 229, 0.2)",
          color: "var(--primary)",
          padding: "12px 16px",
          borderRadius: "var(--radius-md)",
          fontSize: "14px",
          marginBottom: "24px",
          display: "flex",
          alignItems: "center",
          gap: "8px"
        }}>
          <AlertCircle size={18} />
          {refreshMessage}
        </div>
      )}

      {isLoading ? (
        <div style={{ display: "grid", gridTemplateColumns: "1fr", gap: "24px" }}>
          <div className="glass-panel shimmer-placeholder" style={{ height: "450px" }} />
        </div>
      ) : !location ? (
        <div className="glass-panel" style={{ textAlign: "center", padding: "80px 20px" }}>
          <MapPin size={48} color="var(--text-muted)" style={{ marginBottom: "16px" }} />
          <h3 style={{ fontSize: "18px", marginBottom: "8px", fontWeight: 600 }}>No Location Available</h3>
          <p style={{ color: "var(--text-secondary)", fontSize: "14px", maxWidth: "340px", margin: "0 auto" }}>
            We haven't received any location packets. Request location coordinates from the top action button.
          </p>
        </div>
      ) : (
        <div style={{ display: "grid", gridTemplateColumns: "1fr 340px", gap: "24px", alignItems: "start" }} className="location-grid-layout">
          {/* Map display */}
          <div className="glass-panel" style={{ padding: "0", overflow: "hidden", height: "450px" }}>
            <div id="map-container" style={{ width: "100%", height: "100%" }} />
          </div>

          {/* Details list */}
          <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
            <div className="glass-panel">
              <h3 style={{ fontSize: "18px", fontWeight: 600, marginBottom: "20px", display: "flex", alignItems: "center", gap: "10px" }}>
                <Navigation size={20} color="var(--primary)" />
                Coordinate Info
              </h3>
              
              <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
                <div>
                  <span style={{ fontSize: "12px", color: "var(--text-secondary)" }}>Latitude / Longitude</span>
                  <p style={{ fontSize: "15px", fontWeight: 500, fontFamily: "monospace" }}>
                    {location.latitude.toFixed(6)}, {location.longitude.toFixed(6)}
                  </p>
                </div>
                
                <div>
                  <span style={{ fontSize: "12px", color: "var(--text-secondary)" }}>Accuracy Circle</span>
                  <p style={{ fontSize: "15px", fontWeight: 500 }}>
                    ± {Math.round(location.accuracy)} meters
                  </p>
                </div>

                <div>
                  <span style={{ fontSize: "12px", color: "var(--text-secondary)" }}>Altitude</span>
                  <p style={{ fontSize: "15px", fontWeight: 500 }}>
                    {location.altitude ? `${Math.round(location.altitude)} meters` : "Unavailable"}
                  </p>
                </div>
              </div>
            </div>

            <div className="glass-panel">
              <h3 style={{ fontSize: "18px", fontWeight: 600, marginBottom: "20px", display: "flex", alignItems: "center", gap: "10px" }}>
                <Compass size={20} color="var(--secondary)" />
                Telemetry Stats
              </h3>
              
              <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
                <div>
                  <span style={{ fontSize: "12px", color: "var(--text-secondary)" }}>Speed</span>
                  <p style={{ fontSize: "15px", fontWeight: 500 }}>
                    {location.speed ? `${(location.speed * 3.6).toFixed(1)} km/h` : "Stationary"}
                  </p>
                </div>

                <div>
                  <span style={{ fontSize: "12px", color: "var(--text-secondary)" }}>Dispatch Provider</span>
                  <p style={{ fontSize: "15px", fontWeight: 500, textTransform: "capitalize" }}>
                    {location.provider} network
                  </p>
                </div>

                <div>
                  <span style={{ fontSize: "12px", color: "var(--text-secondary)" }}>Last Updated</span>
                  <p style={{ fontSize: "14px", fontWeight: 500, display: "flex", alignItems: "center", gap: "6px" }}>
                    <Calendar size={14} color="var(--text-muted)" />
                    {new Date(location.timestamp).toLocaleString()}
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
export default LocationPage;
