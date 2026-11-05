"use client";

import Link from "next/link";
import { CATEGORY_LABELS } from "@/lib/cosmetics";
import {
  RANK_NAMES,
  STAGE_LABELS,
  TRACK_LABELS,
  formatGain,
  itemGlyph,
  paletteFor,
  rankTitle,
  type NationResultDto
} from "@/lib/nation";

type Props = {
  result: NationResultDto;
  /** Used for the "View your nation" link. */
  username?: string;
};

/** What an accepted solve did to the nation: XP, track points, finished items, what is being built next. */
export default function NationResultPanel({ result, username }: Props) {
  if (result.mode === "NO_OP") {
    // awards switched off in this deployment: say nothing; already counted: explain gently
    if (result.reason !== "ALREADY_COUNTED") return null;
    return (
      <p className="text-xs text-gray-400">
        Already counted for your nation. Solving it again gives a tiny bonus after 24 hours.
      </p>
    );
  }

  const rankIndex = Math.max(0, RANK_NAMES.indexOf(result.rankName ?? ""));
  const accent = paletteFor(rankIndex).accent;
  const gains = [
    { label: TRACK_LABELS.BASICS, value: result.basicsPoints },
    { label: TRACK_LABELS.STRUCTURES, value: result.structuresPoints },
    { label: TRACK_LABELS.LANDMARKS, value: result.landmarksPoints }
  ];

  return (
    <div className="rounded-lg border p-3" style={{ borderColor: accent + "88" }} aria-label="Nation progress">
      <div className="flex items-center justify-between gap-2">
        <h4 className="text-sm font-semibold uppercase tracking-wide" style={{ color: accent }}>Nation progress</h4>
        <span className="rounded-full border border-platform-border px-2 py-0.5 text-xs text-gray-300">
          {result.mode === "FIRST" ? "First solve" : "Re-solve bonus"}
        </span>
      </div>

      <p className="mt-2 text-2xl font-bold text-platform-text">
        +{formatGain(result.xpGained)} <span className="text-base font-semibold text-gray-300">XP</span>
      </p>
      <p className="mt-1 flex flex-wrap gap-x-3 text-xs text-gray-300">
        {gains.map((gain) => (
          <span key={gain.label}>{gain.label} +{formatGain(gain.value)}</span>
        ))}
      </p>

      {result.itemsCompleted.length > 0 && (
        <ul className="mt-3 space-y-1">
          {result.itemsCompleted.map((item, index) => (
            <li key={`${item.key}-${index}`} className="flex items-center gap-2 text-sm text-gray-100">
              <span aria-hidden="true" className="text-lg">{itemGlyph(item.key, item.track)}</span>
              <span>{item.name} completed</span>
              <span className="text-xs text-gray-500">({TRACK_LABELS[item.track]})</span>
            </li>
          ))}
        </ul>
      )}

      <div className="mt-3 space-y-2">
        {result.tracks.map((track) => {
          const percent = track.cost > 0 ? Math.min(100, Math.floor((track.progress / track.cost) * 100)) : 0;
          return (
            <div key={track.track}>
              <div className="flex justify-between text-xs text-gray-300">
                <span>{TRACK_LABELS[track.track]}: {track.itemName}</span>
                <span className="text-gray-400">{STAGE_LABELS[track.stage]}</span>
              </div>
              <div className="mt-0.5 h-1.5 w-full overflow-hidden rounded-full border border-platform-border bg-black">
                <div className="h-full rounded-full" style={{ width: `${percent}%`, backgroundColor: accent }} />
              </div>
            </div>
          );
        })}
      </div>

      {result.unlocked.length > 0 && (
        <p className="mt-3 text-xs text-gray-200">
          Unlocked: {result.unlocked.map((item) => `${item.name} (${CATEGORY_LABELS[item.category] ?? item.category.toLowerCase()})`).join(", ")}
        </p>
      )}

      <div className="mt-3 flex flex-wrap items-center justify-between gap-2 text-xs text-gray-300">
        <span>
          Nation rank: <span className="font-semibold" style={{ color: accent }}>{rankTitle(result.rankName ?? "Null", result.tier)}</span>
        </span>
        {username && (
          <Link href={`/profile/${encodeURIComponent(username)}`} className="text-platform-accent underline">
            View your nation
          </Link>
        )}
      </div>
    </div>
  );
}
