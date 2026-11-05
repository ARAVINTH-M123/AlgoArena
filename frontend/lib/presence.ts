// Presence: shared types and text helpers.

export type PresenceStatus = "ONLINE" | "IDLE" | "OFFLINE";

/** Wire format of the API and of the live stream. */
export type PresenceDto = {
  status: PresenceStatus;
  /** ISO-8601 (UTC); only present when OFFLINE and visible to the viewer. */
  lastActiveAt: string | null;
  /** Counts down from 60 after an accepted first-solve; 0 = not glowing. */
  justSolvedSecondsLeft: number;
};

/** What the UI renders right now (the countdown already applied). */
export type PresenceView = {
  status: PresenceStatus;
  lastActiveAt: string | null;
  justSolved: boolean;
};

export const HEARTBEAT_INTERVAL_MS = 30_000;
export const IDLE_AFTER_MS = 5 * 60 * 1000;

export function timeAgo(iso: string, nowMs: number = Date.now()): string {
  const seconds = Math.max(0, Math.floor((nowMs - new Date(iso).getTime()) / 1000));
  if (seconds < 60) return "just now";
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes} minute${minutes === 1 ? "" : "s"} ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} hour${hours === 1 ? "" : "s"} ago`;
  const days = Math.floor(hours / 24);
  return `${days} day${days === 1 ? "" : "s"} ago`;
}

/** Plain-text status shown next to the scene (accessible, always visible). */
export function presenceLabel(view: PresenceView, nowMs: number = Date.now()): string {
  if (view.status === "ONLINE") return "Online";
  if (view.status === "IDLE") return "Idle";
  return view.lastActiveAt ? `Offline · Last active ${timeAgo(view.lastActiveAt, nowMs)}` : "Offline";
}
