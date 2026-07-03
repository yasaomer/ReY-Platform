import { API_BASE } from "../config";

// Retrieve or initialize session ID
const getSessionId = (): string => {
  let sessionId = sessionStorage.getItem("rey_session_id");
  if (!sessionId) {
    sessionId = "sess-" + Math.random().toString(36).substring(2, 12);
    sessionStorage.setItem("rey_session_id", sessionId);
  }
  return sessionId;
};

// Generic event sender
export const trackEvent = async (
  eventType: string,
  pageName: string,
  targetItem?: string,
  durationSeconds?: number
) => {
  const sessionId = getSessionId();
  try {
    await fetch(`${API_BASE}/analytics/event`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        session_id: sessionId,
        event_type: eventType,
        page_name: pageName,
        target_item: targetItem || null,
        duration_seconds: durationSeconds || null
      })
    });
  } catch (e) {
    console.error("Failed to post visitor analytics event", e);
  }
};

// Auto track duration on pages
export const usePageTracker = (pageName: string) => {
  const startTimeRef = React.useRef(Date.now());

  React.useEffect(() => {
    // 1. Log entrance
    trackEvent("page_view", pageName);
    startTimeRef.current = Date.now();

    // 2. Log exit duration on unmount
    return () => {
      const durationSeconds = (Date.now() - startTimeRef.current) / 1000;
      trackEvent("page_exit", pageName, undefined, durationSeconds);
    };
  }, [pageName]);
};

import React from "react";
