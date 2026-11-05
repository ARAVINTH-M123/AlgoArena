"use client";

import { useEffect, useState } from "react";
import type { PresenceDto, PresenceView } from "@/lib/presence";

/**
 * Turns the latest server presence into what to draw right now: applies the "just solved" countdown and keeps
 * relative times ("5 minutes ago") fresh.
 */
export function usePresenceView(presence: PresenceDto, receivedAt: number): { view: PresenceView; nowMs: number } {
  const [nowMs, setNowMs] = useState(() => Date.now());

  const glowing = presence.justSolvedSecondsLeft > 0;
  useEffect(() => {
    setNowMs(Date.now());
    const timer = window.setInterval(() => setNowMs(Date.now()), glowing ? 1000 : 30_000);
    return () => window.clearInterval(timer);
  }, [glowing, receivedAt]);

  const secondsLeft = presence.justSolvedSecondsLeft - (nowMs - receivedAt) / 1000;
  return {
    view: {
      status: presence.status,
      lastActiveAt: presence.lastActiveAt,
      justSolved: presence.status !== "OFFLINE" && secondsLeft > 0
    },
    nowMs
  };
}
