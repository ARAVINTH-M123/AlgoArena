"use client";

import { useEffect, useState } from "react";
import { X } from "lucide-react";
import { rarityColor, type CacheOpenResultDto } from "@/lib/cosmetics";
import { CATEGORY_LABELS } from "@/lib/cosmetics";

/**
 * The reveal moment: a rarity-colored card naming what the cache contained.
 * Duplicates say so honestly and show the shards they turned into.
 */
export default function CacheReveal({
  result,
  hasMore,
  onOpenAnother,
  onClose
}: {
  result: CacheOpenResultDto;
  /** True when another cache is still waiting, so the button offers to continue. */
  hasMore: boolean;
  onOpenAnother: () => void;
  onClose: () => void;
}) {
  const [shown, setShown] = useState(false);
  useEffect(() => {
    const timer = setTimeout(() => setShown(true), 30);   // let the card pop in
    return () => clearTimeout(timer);
  }, [result.key]);

  const color = rarityColor(result.rarity);

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4"
      role="dialog"
      aria-modal="true"
      aria-label="Mystery Cache opened"
      onClick={onClose}
    >
      <div
        className={`relative w-full max-w-sm rounded-2xl border-2 bg-platform-panel p-6 text-center shadow-2xl transition-all duration-300 ${shown ? "scale-100 opacity-100" : "scale-90 opacity-0"}`}
        style={{ borderColor: color, boxShadow: `0 0 34px ${color}44` }}
        onClick={(event) => event.stopPropagation()}
      >
        <button
          type="button"
          onClick={onClose}
          aria-label="Close"
          className="absolute right-3 top-3 text-gray-500 hover:text-gray-200"
        >
          <X className="h-5 w-5" aria-hidden="true" />
        </button>

        <p className="text-xs font-semibold uppercase tracking-widest" style={{ color }}>
          {result.rarity}
        </p>
        <div
          className="mx-auto mt-4 flex h-24 w-24 items-center justify-center rounded-full border-2 text-4xl"
          style={{ borderColor: color, boxShadow: `0 0 22px ${color}55`, background: `${color}14` }}
          aria-hidden="true"
        >
          {result.wasDuplicate ? "✦" : "✧"}
        </div>

        <h3 className="mt-4 text-lg font-bold text-platform-text">{result.name}</h3>
        <p className="text-sm text-gray-400">
          {CATEGORY_LABELS[result.category] ?? result.category.toLowerCase()}
        </p>

        {result.wasDuplicate ? (
          <p className="mt-3 text-sm" style={{ color }}>
            Already in your collection - it turned into <strong>{result.shardsGained} shards</strong>.
          </p>
        ) : (
          <p className="mt-3 text-sm text-platform-accent">Added to your collection. Equip it on your nation page.</p>
        )}

        <div className="mt-5 flex flex-col gap-2">
          {hasMore ? (
            <button
              type="button"
              onClick={onOpenAnother}
              className="w-full rounded-lg border border-platform-border px-4 py-2 text-sm font-semibold text-platform-text transition hover:border-platform-accent"
            >
              Open the next cache ({result.unopenedCaches} left)
            </button>
          ) : (
            <p className="text-xs text-gray-500">
              Solve problems to earn more caches. {result.shards.toLocaleString("en-US")} shards in the vault.
            </p>
          )}
          <button
            type="button"
            onClick={onClose}
            className="w-full rounded-lg px-4 py-2 text-sm text-gray-400 transition hover:text-gray-200"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
}
