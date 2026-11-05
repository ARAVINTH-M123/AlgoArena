"use client";

import { useEffect } from "react";
import { sendHeartbeat, sendOfflineBeacon } from "@/lib/api";
import { HEARTBEAT_INTERVAL_MS, IDLE_AFTER_MS } from "@/lib/presence";

const SESSION_KEY = "presenceSessionId";

/** One random id per browser tab (kept across reloads of the same tab). */
function getSessionId(): string {
  const existing = sessionStorage.getItem(SESSION_KEY);
  if (existing) return existing;
  const fresh =
    typeof crypto !== "undefined" && typeof crypto.randomUUID === "function"
      ? crypto.randomUUID()
      : `${Math.random().toString(36).slice(2)}${Date.now().toString(36)}`.padEnd(12, "0");
  sessionStorage.setItem(SESSION_KEY, fresh);
  return fresh;
}

const ACTIVITY_EVENTS = ["pointerdown", "pointermove", "keydown", "wheel", "touchstart", "scroll"] as const;

/**
 * While enabled (user logged in): tells the server every 30 s that this tab is open and whether the user has
 * touched it in the last 5 minutes. Pauses while the tab is hidden and says goodbye when the page is closed.
 */
export function useHeartbeat(enabled: boolean) {
  useEffect(() => {
    if (!enabled) return;

    const sessionId = getSessionId();
    let lastInteraction = Date.now();
    const markActive = () => {
      lastInteraction = Date.now();
    };
    ACTIVITY_EVENTS.forEach((name) => window.addEventListener(name, markActive, { passive: true }));

    const beat = () => {
      if (document.visibilityState !== "visible") return;
      const active = Date.now() - lastInteraction < IDLE_AFTER_MS;
      // 429 (too soon) and network errors are harmless: the next beat fixes things
      sendHeartbeat(sessionId, active).catch(() => undefined);
    };

    const onVisibility = () => {
      if (document.visibilityState === "visible") beat();
    };
    const onPageHide = () => sendOfflineBeacon(sessionId);

    beat();
    const timer = window.setInterval(beat, HEARTBEAT_INTERVAL_MS);
    document.addEventListener("visibilitychange", onVisibility);
    window.addEventListener("pagehide", onPageHide);

    return () => {
      window.clearInterval(timer);
      document.removeEventListener("visibilitychange", onVisibility);
      window.removeEventListener("pagehide", onPageHide);
      ACTIVITY_EVENTS.forEach((name) => window.removeEventListener(name, markActive));
    };
  }, [enabled]);
}
