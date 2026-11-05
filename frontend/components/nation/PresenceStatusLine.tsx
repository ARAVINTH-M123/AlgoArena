import { presenceLabel, type PresenceView } from "@/lib/presence";

type Props = {
  presence: PresenceView;
  nowMs: number;
  /** Owner view: shown when "Appear offline" is on, because visitors then see something different. */
  hiddenFromVisitors?: boolean;
};

const DOT_COLOR = { ONLINE: "#22c55e", IDLE: "#f59e0b", OFFLINE: "#6b7280" } as const;

/** Always-visible plain-text status (the figure is decoration; this is the accessible truth). */
export default function PresenceStatusLine({ presence, nowMs, hiddenFromVisitors }: Props) {
  return (
    <p role="status" aria-live="polite" className="flex flex-wrap items-center gap-2 text-sm text-gray-300">
      <span className="inline-block h-2.5 w-2.5 rounded-full" style={{ backgroundColor: DOT_COLOR[presence.status] }} aria-hidden="true" />
      <span>{presenceLabel(presence, nowMs)}</span>
      {presence.justSolved && <span className="text-platform-accent">· just solved a problem</span>}
      {hiddenFromVisitors && <span className="text-xs text-gray-500">(visitors see you as offline)</span>}
    </p>
  );
}
