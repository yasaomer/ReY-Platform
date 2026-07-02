import React, { useState, useEffect } from "react";
import { Maximize2, X, ChevronLeft, ChevronRight, Eye, Calendar, Sparkles, Heart, Image as ImageIcon } from "lucide-react";

interface GalleryImage {
  image_id: string;
  file_name: string;
  file_hash: string;
  thumbnail_id: string | null;
  width: number | null;
  height: number | null;
  size_bytes: number | null;
  created_time: string;
  view_count: number;
  is_favorite: number;
  category: string | null;
}

export const GalleryPage: React.FC = () => {
  const [images, setImages] = useState<GalleryImage[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [activeIndex, setActiveIndex] = useState<number | null>(null);

  const API_BASE = "http://localhost:8787/api/v1";

  const fetchGallery = async () => {
    setIsLoading(true);
    const token = localStorage.getItem("token");
    try {
      const response = await fetch(`${API_BASE}/gallery/list`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      const res = await response.json();
      if (res.success) {
        setImages(res.data);
      }
    } catch (e) {
      console.error("Failed to fetch gallery metadata", e);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchGallery();
  }, []);

  const getImageUrl = (img: GalleryImage) => {
    // If thumbnail_id is present, load from Google Drive hotlink, otherwise use placeholder
    if (img.thumbnail_id) {
      return `https://lh3.googleusercontent.com/d/${img.thumbnail_id}`;
    }
    return `https://images.unsplash.com/photo-1579546929518-9e396f3cc809?w=800&auto=format&fit=crop&q=60`;
  };

  const getFullImageUrl = (img: GalleryImage) => {
    if (img.image_id) {
      return `https://lh3.googleusercontent.com/d/${img.image_id}`;
    }
    return getImageUrl(img);
  };

  const logImageView = async (imageId: string, eventType: string = "view") => {
    const token = localStorage.getItem("token");
    try {
      await fetch(`${API_BASE}/gallery/log-view`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({ image_id: imageId, event_type: eventType })
      });
    } catch (e) {
      console.error("Failed to log view statistic", e);
    }
  };

  const openLightbox = (index: number) => {
    setActiveIndex(index);
    logImageView(images[index].image_id, "view");
  };

  const closeLightbox = () => {
    setActiveIndex(null);
  };

  const nextImage = (e?: React.MouseEvent) => {
    e?.stopPropagation();
    if (activeIndex === null) return;
    const nextIdx = (activeIndex + 1) % images.length;
    setActiveIndex(nextIdx);
    logImageView(images[nextIdx].image_id, "view");
  };

  const prevImage = (e?: React.MouseEvent) => {
    e?.stopPropagation();
    if (activeIndex === null) return;
    const prevIdx = (activeIndex - 1 + images.length) % images.length;
    setActiveIndex(prevIdx);
    logImageView(images[prevIdx].image_id, "view");
  };

  // Keyboard navigation
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (activeIndex === null) return;
      if (e.key === "ArrowRight") nextImage();
      if (e.key === "ArrowLeft") prevImage();
      if (e.key === "Escape") closeLightbox();
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [activeIndex, images]);

  return (
    <div>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", marginBottom: "32px" }}>
        <div>
          <h2 style={{ fontSize: "28px", fontWeight: 800, fontFamily: "var(--font-display)" }}>Photo Gallery</h2>
          <p style={{ color: "var(--text-secondary)", fontSize: "14px", marginTop: "4px" }}>Synchronized snapshots from owner's device</p>
        </div>
      </div>

      {isLoading ? (
        // Grid skeleton loaders
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(220px, 1fr))", gap: "20px" }}>
          {[1, 2, 3, 4, 5, 6].map(n => (
            <div key={n} className="glass-panel shimmer-placeholder" style={{ aspectRatio: "1", borderRadius: "var(--radius-md)" }} />
          ))}
        </div>
      ) : images.length === 0 ? (
        // Elegant empty state (Section 99)
        <div className="glass-panel" style={{ textAlign: "center", padding: "80px 20px" }}>
          <ImageIcon size={48} color="var(--text-muted)" style={{ marginBottom: "16px" }} />
          <h3 style={{ fontSize: "18px", marginBottom: "8px", fontWeight: 600 }}>No Images Yet</h3>
          <p style={{ color: "var(--text-secondary)", fontSize: "14px", maxWidth: "340px", margin: "0 auto" }}>
            The photo archive is currently empty. Images approved in the Private OML app will appear here.
          </p>
        </div>
      ) : (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(220px, 1fr))", gap: "20px" }}>
          {images.map((img, idx) => (
            <div
              key={img.image_id}
              className="glass-panel"
              onClick={() => openLightbox(idx)}
              style={{
                padding: "0",
                overflow: "hidden",
                cursor: "pointer",
                position: "relative",
                aspectRatio: "1",
                border: "1px solid var(--border-glass)",
                borderRadius: "var(--radius-md)"
              }}
              onMouseEnter={(e) => {
                const overlay = e.currentTarget.querySelector(".hover-overlay") as HTMLElement;
                if (overlay) overlay.style.opacity = "1";
              }}
              onMouseLeave={(e) => {
                const overlay = e.currentTarget.querySelector(".hover-overlay") as HTMLElement;
                if (overlay) overlay.style.opacity = "0";
              }}
            >
              <img
                src={getImageUrl(img)}
                alt={img.file_name}
                loading="lazy"
                style={{ width: "100%", height: "100%", objectFit: "cover", display: "block" }}
              />
              
              {/* Heart favorite badge */}
              {img.is_favorite === 1 && (
                <div style={{
                  position: "absolute",
                  top: "12px",
                  right: "12px",
                  background: "rgba(239, 68, 68, 0.2)",
                  color: "#ef4444",
                  backdropFilter: "blur(4px)",
                  padding: "6px",
                  borderRadius: "50%",
                  display: "flex"
                }}>
                  <Heart size={14} fill="#ef4444" />
                </div>
              )}

              {/* Glass overlay on hover */}
              <div
                className="hover-overlay"
                style={{
                  position: "absolute",
                  top: 0,
                  left: 0,
                  right: 0,
                  bottom: 0,
                  background: "rgba(9, 10, 15, 0.4)",
                  backdropFilter: "blur(4px)",
                  opacity: 0,
                  transition: "var(--transition-fast)",
                  display: "flex",
                  flexDirection: "column",
                  justifyContent: "space-between",
                  padding: "16px",
                  color: "#fff"
                }}
              >
                <div style={{ alignSelf: "flex-end" }}>
                  <Maximize2 size={18} />
                </div>
                <div>
                  <h4 style={{ fontSize: "14px", fontWeight: 600, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                    {img.file_name}
                  </h4>
                  <p style={{ fontSize: "11px", opacity: 0.8, display: "flex", alignItems: "center", gap: "4px", marginTop: "2px" }}>
                    <Calendar size={12} />
                    {new Date(img.created_time).toLocaleDateString()}
                  </p>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* LIGHTBOX MODAL */}
      {activeIndex !== null && (
        <div
          onClick={closeLightbox}
          style={{
            position: "fixed",
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            background: "rgba(9, 10, 15, 0.95)",
            backdropFilter: "blur(20px)",
            zIndex: 9999,
            display: "flex",
            alignItems: "center",
            justifyContent: "center"
          }}
        >
          {/* Close button */}
          <button
            onClick={closeLightbox}
            style={{
              position: "absolute",
              top: "24px",
              right: "24px",
              background: "rgba(255,255,255,0.05)",
              border: "1px solid var(--border-glass)",
              color: "#fff",
              padding: "10px",
              borderRadius: "50%",
              cursor: "pointer",
              display: "flex",
              zIndex: 100
            }}
          >
            <X size={20} />
          </button>

          {/* Left Navigation */}
          <button
            onClick={prevImage}
            style={{
              position: "absolute",
              left: "24px",
              background: "rgba(255,255,255,0.05)",
              border: "1px solid var(--border-glass)",
              color: "#fff",
              padding: "12px",
              borderRadius: "50%",
              cursor: "pointer",
              display: "flex"
            }}
          >
            <ChevronLeft size={24} />
          </button>

          {/* Lightbox image */}
          <div
            onClick={(e) => e.stopPropagation()}
            style={{
              maxWidth: "85%",
              maxHeight: "80%",
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              gap: "20px"
            }}
          >
            <img
              src={getFullImageUrl(images[activeIndex])}
              alt={images[activeIndex].file_name}
              style={{
                maxWidth: "100%",
                maxHeight: "70vh",
                borderRadius: "var(--radius-lg)",
                boxShadow: "0 20px 50px rgba(0,0,0,0.8)",
                border: "1px solid var(--border-glass)"
              }}
            />
            <div style={{ textAlign: "center" }}>
              <h3 style={{ fontSize: "18px", color: "#fff", fontWeight: 600 }}>{images[activeIndex].file_name}</h3>
              <div style={{ display: "flex", gap: "16px", justifyContent: "center", marginTop: "8px", color: "var(--text-secondary)", fontSize: "13px" }}>
                <span style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                  <Calendar size={14} />
                  {new Date(images[activeIndex].created_time).toLocaleDateString()}
                </span>
                <span style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                  <Eye size={14} />
                  {images[activeIndex].view_count} views
                </span>
                {images[activeIndex].category && (
                  <span style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                    <Sparkles size={14} />
                    {images[activeIndex].category}
                  </span>
                )}
              </div>
            </div>
          </div>

          {/* Right Navigation */}
          <button
            onClick={nextImage}
            style={{
              position: "absolute",
              right: "24px",
              background: "rgba(255,255,255,0.05)",
              border: "1px solid var(--border-glass)",
              color: "#fff",
              padding: "12px",
              borderRadius: "50%",
              cursor: "pointer",
              display: "flex"
            }}
          >
            <ChevronRight size={24} />
          </button>
        </div>
      )}
    </div>
  );
};
export default GalleryPage;
