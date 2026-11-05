"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useReducedMotion } from "@/hooks/useReducedMotion";
import { CATEGORY_LABELS } from "@/lib/cosmetics";
import { RANK_NAMES, paletteFor } from "@/lib/nation";

type Props = {
  /** Names of the ranks just reached, in order, for example ["Boot"]. */
  rankUps: string[];
  /** Cosmetics unlocked by this rank-up, shown once the boot text has finished. */
  unlocked?: { category: string; key: string; name: string }[];
  onClose: () => void;
};

const LINE_DELAY_MS = 650;

/** Boot-style lines: "> Init complete." "> Booting Boot..." "> Boot loaded." for every rank reached. */
export function bootLines(rankUps: string[]): string[] {
  const lines: string[] = [];
  for (const name of rankUps) {
    const index = RANK_NAMES.indexOf(name);
    const previous = index > 0 ? RANK_NAMES[index - 1] : "System";
    lines.push(`> ${previous} complete.`, `> Booting ${name}...`, `> ${name} loaded.`);
  }
  return lines;
}

function skyGradient(rankIndex: number): string {
  const palette = paletteFor(rankIndex);
  return `linear-gradient(to bottom, ${palette.skyTop}, ${palette.skyBottom})`;
}

/** Full-screen "your nation transforms" moment: the old look fades into the new one while the boot text types. */
export default function RankUpOverlay({ rankUps, unlocked = [], onClose }: Props) {
  const reduced = useReducedMotion();
  const lines = useMemo(() => bootLines(rankUps), [rankUps]);
  const [shown, setShown] = useState(1);
  const [transformed, setTransformed] = useState(false);
  const closeRef = useRef<HTMLButtonElement>(null);

  const finalName = rankUps[rankUps.length - 1] ?? "Null";
  const finalIndex = Math.max(0, RANK_NAMES.indexOf(finalName));
  const fromIndex = Math.max(0, RANK_NAMES.indexOf(rankUps[0] ?? "Null") - 1);
  const accent = paletteFor(finalIndex).accent;
  const done = shown >= lines.length;

  // type the lines one by one (all at once for reduced motion)
  useEffect(() => {
    if (reduced) {
      setShown(lines.length);
      return;
    }
    if (shown >= lines.length) return;
    const timer = window.setTimeout(() => setShown((count) => count + 1), LINE_DELAY_MS);
    return () => window.clearTimeout(timer);
  }, [shown, lines, reduced]);

  // the new look fades in over the old one
  useEffect(() => {
    const frame = window.requestAnimationFrame(() => setTransformed(true));
    return () => window.cancelAnimationFrame(frame);
  }, []);

  useEffect(() => {
    closeRef.current?.focus();
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  return (
    <div role="dialog" aria-modal="true" aria-labelledby="rank-up-title" className="fixed inset-0 z-[60] flex items-center justify-center p-4">
      <div className="absolute inset-0" style={{ background: skyGradient(fromIndex) }} />
      <div
        className="absolute inset-0"
        style={{ background: skyGradient(finalIndex), opacity: transformed ? 1 : 0, transition: reduced ? "none" : "opacity 2.8s ease-in-out" }}
      />

      <div className="relative w-full max-w-md rounded-xl border bg-black/70 p-6 text-center" style={{ borderColor: accent + "aa" }}>
        <p className="text-xs uppercase tracking-[0.3em] text-gray-300">Nation rank up</p>
        <h2 id="rank-up-title" className="mt-2 text-3xl font-bold" style={{ color: accent }}>
          {RANK_NAMES[fromIndex]} &rarr; {finalName}
        </h2>

        <div className="mt-5 min-h-[5.5rem] rounded-md border border-platform-border bg-black/60 p-3 text-left font-mono text-sm" aria-live="polite">
          {lines.slice(0, shown).map((line, index) => (
            <p key={`${line}-${index}`} style={{ color: index === lines.length - 1 ? accent : "#d1d5db" }}>{line}</p>
          ))}
          {!done && <span className="inline-block animate-pulse text-gray-300">_</span>}
        </div>

        {done && unlocked.length > 0 && (
          <div className="mt-4 text-left text-sm text-gray-200">
            <p className="text-xs uppercase tracking-wide text-gray-400">Unlocked</p>
            <ul className="mt-1 space-y-0.5">
              {unlocked.map((item) => (
                <li key={item.key}>
                  <span className="font-semibold" style={{ color: accent }}>{item.name}</span>{" "}
                  <span className="text-gray-400">{CATEGORY_LABELS[item.category] ?? item.category.toLowerCase()}</span>
                </li>
              ))}
            </ul>
            <p className="mt-1 text-xs text-gray-500">Choose them on your profile page.</p>
          </div>
        )}

        <button ref={closeRef} type="button" onClick={onClose} className="btn-accent mt-5">
          {done ? "Continue" : "Skip"}
        </button>
      </div>
    </div>
  );
}
