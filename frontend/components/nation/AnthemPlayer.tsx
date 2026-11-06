"use client";

import { Pause, Play, VolumeX } from "lucide-react";
import { useAnthem } from "@/hooks/useAnthem";
import { anthemTitle } from "@/lib/cosmetics";

type Props = {
  /** The anthem in effect for this nation (a catalog key). */
  anthemKey: string | null | undefined;
  /** Compact variant for small spaces (the scene header). */
  compact?: boolean;
};

/**
 * Play/stop control for a nation's anthem. Never starts on its own: the AudioContext is created
 * inside the click handler, so browsers allow the sound. Visitors can listen too.
 */
export default function AnthemPlayer({ anthemKey, compact = false }: Props) {
  const { playingKey, toggle, muted, setMuted } = useAnthem();

  if (!anthemKey) {
    return null;
  }

  const playing = playingKey === anthemKey;
  const title = anthemTitle(anthemKey);

  return (
    <span className="inline-flex items-center gap-2">
      <button
        type="button"
        onClick={() => toggle(anthemKey)}
        aria-pressed={playing}
        aria-label={playing ? `Stop the anthem ${title}` : `Play the anthem ${title}`}
        className={`inline-flex items-center gap-1.5 rounded-lg border px-2.5 py-1 text-xs font-semibold transition ${
          playing
            ? "border-platform-accent bg-platform-accentSoft/40 text-platform-accent"
            : "border-platform-border text-gray-300 hover:border-gray-500"
        }`}
      >
        {playing ? <Pause className="h-3.5 w-3.5" aria-hidden="true" /> : <Play className="h-3.5 w-3.5" aria-hidden="true" />}
        {playing ? "Stop anthem" : (compact ? "Anthem" : `Play anthem`)}
      </button>
      {!playing && (
        <button
          type="button"
          onClick={() => setMuted(!muted)}
          aria-label={muted ? "Unmute anthems" : "Mute anthems"}
          title={muted ? "Anthems are muted" : "Anthems will play when you press play"}
          className={`rounded-lg border p-1.5 transition ${muted ? "border-platform-border text-gray-500" : "border-platform-border text-gray-300 hover:border-gray-500"}`}
        >
          <VolumeX className="h-3.5 w-3.5" aria-hidden="true" />
        </button>
      )}
      {playing && <span className="text-xs text-gray-400">{title}</span>}
    </span>
  );
}
