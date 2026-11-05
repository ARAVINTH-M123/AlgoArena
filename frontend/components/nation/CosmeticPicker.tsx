"use client";

import { useEffect, useState } from "react";
import { Check, Lock } from "lucide-react";
import NationFrame from "@/components/nation/NationFrame";
import { equipCosmetic, getApiErrorMessage, getMyCosmetics } from "@/lib/api";
import { themeLook, type CosmeticItemDto, type CosmeticsDto, type EquippableCategory } from "@/lib/cosmetics";

type ViewProps = {
  cosmetics: CosmeticsDto;
  busy: boolean;
  onPick: (category: EquippableCategory, key: string | null) => void;
};

const SECTIONS: { category: EquippableCategory; title: string; help: string }[] = [
  { category: "THEME", title: "Editor theme", help: "The look of your code editor. Each rank unlocks one." },
  { category: "BORDER", title: "Nation frame", help: "The frame around your nation scene, visible to every visitor." }
];

function Preview({ item }: { item: CosmeticItemDto }) {
  if (item.category === "BORDER") {
    return (
      <NationFrame borderKey={item.key}>
        <div style={{ height: 34, background: "#05070a" }} />
      </NationFrame>
    );
  }
  const look = themeLook(item.key);
  return (
    <div className="space-y-1 rounded-md border border-platform-border p-2" style={{ background: look.background }} aria-hidden="true">
      <div className="h-1.5 w-3/4 rounded" style={{ background: look.accent }} />
      <div className="h-1.5 w-1/2 rounded" style={{ background: look.accent, opacity: 0.55 }} />
      <div className="h-1.5 w-2/3 rounded" style={{ background: "#d6deeb", opacity: 0.35 }} />
    </div>
  );
}

/** Presentational part (no network): easy to render and test. */
export function CosmeticPickerView({ cosmetics, busy, onPick }: ViewProps) {
  return (
    <div className="space-y-5">
      {SECTIONS.map((section) => {
        const items = cosmetics.items.filter((item) => item.category === section.category);
        const pinned = section.category === "THEME" ? cosmetics.pinnedThemeKey : cosmetics.pinnedBorderKey;
        return (
          <div key={section.category}>
            <h4 className="text-sm font-semibold text-gray-200">{section.title}</h4>
            <p className="text-xs text-gray-500">{section.help}</p>
            <div className="mt-2 grid grid-cols-2 gap-3 md:grid-cols-5">
              <button
                type="button"
                disabled={busy}
                onClick={() => onPick(section.category, null)}
                aria-pressed={pinned === null}
                className={`rounded-lg border p-2 text-left text-xs transition ${pinned === null ? "border-platform-accent bg-platform-accentSoft/40" : "border-platform-border hover:border-gray-500"}`}
              >
                <span className="font-semibold text-gray-100">Automatic</span>
                <span className="mt-1 block text-gray-400">Follows your current rank</span>
                {pinned === null && <Check className="mt-1 h-4 w-4 text-platform-accent" aria-label="Selected" />}
              </button>

              {items.map((item) => {
                const selected = pinned === item.key;
                return (
                  <button
                    key={item.key}
                    type="button"
                    disabled={busy || !item.owned}
                    onClick={() => onPick(section.category, item.key)}
                    aria-pressed={selected}
                    className={`rounded-lg border p-2 text-left text-xs transition ${
                      selected ? "border-platform-accent bg-platform-accentSoft/40" : "border-platform-border"
                    } ${item.owned ? "hover:border-gray-500" : "cursor-not-allowed opacity-50"}`}
                  >
                    <Preview item={item} />
                    <span className="mt-2 block font-semibold text-gray-100">{item.displayName}</span>
                    {item.owned ? (
                      <span className="block text-gray-400">
                        {item.equipped ? "In use" : item.rarity.charAt(0) + item.rarity.slice(1).toLowerCase()}
                      </span>
                    ) : (
                      <span className="flex items-center gap-1 text-gray-500">
                        <Lock className="h-3 w-3" aria-hidden="true" />
                        Unlocks at {item.unlockRankName}
                      </span>
                    )}
                  </button>
                );
              })}
            </div>
          </div>
        );
      })}
    </div>
  );
}

type Props = {
  /** Called after every successful change so the page can update the nation's frame immediately. */
  onChanged: (cosmetics: CosmeticsDto) => void;
};

/** Owner-only: choose the editor theme and the nation frame among the unlocked ones. */
export default function CosmeticPicker({ onChanged }: Props) {
  const [cosmetics, setCosmetics] = useState<CosmeticsDto | null>(null);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<{ kind: "ok" | "error"; text: string } | null>(null);

  useEffect(() => {
    let cancelled = false;
    getMyCosmetics()
      .then((data) => {
        if (!cancelled) setCosmetics(data);
      })
      .catch((error) => {
        if (!cancelled) setMessage({ kind: "error", text: getApiErrorMessage(error, "Could not load your cosmetics.") });
      });
    return () => {
      cancelled = true;
    };
  }, []);

  async function pick(category: EquippableCategory, key: string | null) {
    setBusy(true);
    setMessage(null);
    try {
      const updated = await equipCosmetic(category, key);
      setCosmetics(updated);
      onChanged(updated);
      setMessage({ kind: "ok", text: "Saved." });
    } catch (error) {
      setMessage({ kind: "error", text: getApiErrorMessage(error, "Could not change that.") });
    } finally {
      setBusy(false);
    }
  }

  return (
    <div>
      <h3 className="text-sm font-semibold uppercase tracking-wide text-gray-400">Cosmetics</h3>
      <div className="mt-3">
        {cosmetics ? <CosmeticPickerView cosmetics={cosmetics} busy={busy} onPick={pick} /> : !message && <p className="text-sm text-gray-400">Loading...</p>}
      </div>
      {message && (
        <p role="status" className={`mt-3 text-sm ${message.kind === "ok" ? "text-platform-accent" : "text-red-400"}`}>{message.text}</p>
      )}
    </div>
  );
}
