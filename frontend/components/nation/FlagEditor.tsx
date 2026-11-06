"use client";

import { useMemo, useState } from "react";
import { Check } from "lucide-react";
import { FlagGraphic } from "@/components/nation/Flag";
import { getApiErrorMessage, updateNationProfile } from "@/lib/api";
import type { FlagDto } from "@/lib/cosmetics";
import { EMBLEMS_BY_RANK, FLAG_COLOR_HEX, FLAG_COLOR_KEYS, FLAG_PATTERN_KEYS, FLAG_PATTERNS, defaultFlag } from "@/lib/flags";
import { RANK_NAMES, type FlagPayload, type NationDto } from "@/lib/nation";

type Props = {
  nation: NationDto;
  onUpdated: (nation: NationDto) => void;
};

/** Owner-only flag designer. Everything is validated by the server; the UI simply never sends broken flags. */
export default function FlagEditor({ nation, onUpdated }: Props) {
  const current = useMemo(
    () => nation.flag ?? defaultFlag(nation.rankIndex),
    [nation.flag, nation.rankIndex]
  );
  const [pattern, setPattern] = useState(current.pattern);
  const [colors, setColors] = useState<string[]>(current.colors);
  const [emblem, setEmblem] = useState<string | null>(current.emblem);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<{ kind: "ok" | "error"; text: string } | null>(null);

  const preview: FlagDto = { pattern, colors: colors.slice(0, 3), emblem };
  const unchanged = pattern === current.pattern && sameColors(colors, current.colors) && emblem === current.emblem;

  function toggleColor(key: string) {
    setColors((prev) => {
      if (prev.includes(key)) {
        return prev.filter((c) => c !== key);
      }
      if (prev.length >= 3) {
        return [...prev.slice(1), key];          // replace the oldest pick
      }
      return [...prev, key];
    });
  }

  async function save() {
    if (colors.length < 2 || saving) {
      return;
    }
    const payload: FlagPayload = { pattern, colors, emblem: emblem ?? null };
    setSaving(true);
    setMessage(null);
    try {
      const updated = await updateNationProfile({ flag: payload });
      onUpdated(updated);
      setMessage({ kind: "ok", text: "Flag saved." });
    } catch (error) {
      setMessage({ kind: "error", text: getApiErrorMessage(error, "Could not save your flag.") });
    } finally {
      setSaving(false);
    }
  }

  return (
    <div>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h3 className="text-sm font-semibold uppercase tracking-wide text-gray-400">Flag</h3>
        <FlagGraphic flag={preview} className="h-[60px] w-[90px] rounded-sm shadow" />
      </div>

      <div className="mt-3 grid gap-4 md:grid-cols-3">
        <div>
          <p className="mb-1 text-xs font-semibold text-gray-300">Pattern</p>
          <div className="flex flex-wrap gap-1.5">
            {FLAG_PATTERN_KEYS.map((key) => (
              <button
                key={key}
                type="button"
                aria-pressed={pattern === key}
                onClick={() => setPattern(key)}
                className={`rounded-md border px-2 py-1 text-xs transition ${
                  pattern === key ? "border-platform-accent bg-platform-accentSoft/40 text-platform-accent" : "border-platform-border text-gray-300 hover:border-gray-500"
                }`}
              >
                {FLAG_PATTERNS[key].label}
              </button>
            ))}
          </div>
        </div>

        <div>
          <p className="mb-1 text-xs font-semibold text-gray-300">
            Colors <span className="font-normal text-gray-500">(pick 2 or 3)</span>
          </p>
          <div className="flex flex-wrap gap-1.5">
            {FLAG_COLOR_KEYS.map((key) => {
              const index = colors.indexOf(key);
              const selected = index !== -1;
              return (
                <button
                  key={key}
                  type="button"
                  aria-pressed={selected}
                  title={key.replace("color.", "") + (selected ? ` (${index + 1}${index === 0 ? "st" : index === 1 ? "nd" : "rd"})` : "")}
                  onClick={() => toggleColor(key)}
                  className={`relative h-7 w-7 rounded-md border transition ${selected ? "border-platform-accent" : "border-black/40 hover:border-gray-400"}`}
                  style={{ background: FLAG_COLOR_HEX[key] }}
                >
                  {selected && (
                    <Check
                      className="absolute inset-0 m-auto h-4 w-4"
                      style={{ color: key === "color.white" || key === "color.amber" ? "#111827" : "#f3f4f6" }}
                      aria-label={`Position ${index + 1}`}
                    />
                  )}
                </button>
              );
            })}
          </div>
        </div>

        <div>
          <p className="mb-1 text-xs font-semibold text-gray-300">Emblem</p>
          <div className="flex flex-wrap gap-1.5">
            <button
              type="button"
              aria-pressed={emblem === null}
              onClick={() => setEmblem(null)}
              className={`rounded-md border px-2 py-1 text-xs transition ${
                emblem === null ? "border-platform-accent bg-platform-accentSoft/40 text-platform-accent" : "border-platform-border text-gray-300 hover:border-gray-500"
              }`}
            >
              None
            </button>
            {EMBLEMS_BY_RANK.map((entry, index) => {
              const locked = index > nation.rankIndex;
              return (
                <button
                  key={entry.key}
                  type="button"
                  disabled={locked}
                  aria-pressed={emblem === entry.key}
                  title={locked ? `Unlocks at ${rankName(index)}` : entry.label}
                  onClick={() => setEmblem(entry.key)}
                  className={`rounded-md border px-2 py-1 text-xs transition ${
                    emblem === entry.key
                      ? "border-platform-accent bg-platform-accentSoft/40 text-platform-accent"
                      : locked
                        ? "cursor-not-allowed border-platform-border text-gray-600"
                        : "border-platform-border text-gray-300 hover:border-gray-500"
                  }`}
                >
                  {entry.label}
                </button>
              );
            })}
          </div>
        </div>
      </div>

      <div className="mt-4 flex items-center gap-3">
        <button
          type="button"
          className="btn-accent disabled:cursor-not-allowed disabled:opacity-50"
          disabled={saving || unchanged || colors.length < 2}
          onClick={save}
        >
          {saving ? "Saving..." : "Save flag"}
        </button>
        {colors.length < 2 && <span className="text-xs text-gray-500">Pick at least two colors.</span>}
        {message && (
          <span role="status" className={`text-sm ${message.kind === "ok" ? "text-platform-accent" : "text-red-400"}`}>
            {message.text}
          </span>
        )}
      </div>
    </div>
  );
}

function sameColors(a: string[], b: string[]): boolean {
  return a.length === b.length && a.every((color, i) => color === b[i]);
}

function rankName(index: number): string {
  return RANK_NAMES[Math.min(Math.max(index, 0), RANK_NAMES.length - 1)];
}
