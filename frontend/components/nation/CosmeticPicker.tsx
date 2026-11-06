"use client";

import { useEffect, useState } from "react";
import { Check, Lock, Music2 } from "lucide-react";
import NationFrame from "@/components/nation/NationFrame";
import { equipCosmetic, getApiErrorMessage, getMyCosmetics } from "@/lib/api";
import { anthemController } from "@/lib/anthemPlayer";
import {
  ambientLook,
  auraLook,
  skyLook,
  themeLook,
  type CosmeticItemDto,
  type CosmeticsDto,
  type EquippableCategory
} from "@/lib/cosmetics";

/** What the owner pinned in this category (null = automatic or none). */
function pinnedKeyFor(cosmetics: CosmeticsDto, category: EquippableCategory): string | null {
  switch (category) {
    case "THEME": return cosmetics.pinnedThemeKey;
    case "BORDER": return cosmetics.pinnedBorderKey;
    case "ANTHEM": return cosmetics.pinnedAnthemKey;
    case "SKY": return cosmetics.pinnedSkyKey;
    case "AURA": return cosmetics.pinnedAuraKey;
    case "AMBIENT": return cosmetics.pinnedAmbientKey;
  }
}

const SECTIONS: { category: EquippableCategory; title: string; help: string }[] = [
  { category: "THEME", title: "Editor theme", help: "The look of your code editor. Each rank unlocks one." },
  { category: "BORDER", title: "Nation frame", help: "The frame around your nation scene, visible to every visitor." },
  { category: "ANTHEM", title: "Anthem", help: "A little generative song for your nation. Preview any of them; equip the ones you have unlocked." },
  { category: "SKY", title: "Sky", help: "The sky of your nation scene. Mystery Caches and shard crafting are the only way to get these." },
  { category: "AURA", title: "Aura", help: "A glowing ring around your presence figure. The Ascended Aura is granted when the Teleport Gate completes." },
  { category: "AMBIENT", title: "Ambient", help: "Little motes drifting over your nation scene. Duplicates become shards." }
];

type ViewProps = {
  cosmetics: CosmeticsDto;
  busy: boolean;
  onPick: (category: EquippableCategory, key: string | null) => void;
};

function Preview({ item }: { item: CosmeticItemDto }) {
  if (item.category === "BORDER") {
    return (
      <NationFrame borderKey={item.key}>
        <div style={{ height: 34, background: "#05070a" }} />
      </NationFrame>
    );
  }
  if (item.category === "ANTHEM") {
    // a static little waveform so every anthem card has a face (no nested buttons here: the card is a div)
    const heights = [8, 14, 20, 12, 24, 16, 9, 18, 13, 22, 10, 15];
    return (
      <div className="flex h-[50px] items-end justify-center gap-[3px] rounded-md border border-platform-border bg-black/30 px-2 py-1.5" aria-hidden="true">
        {heights.map((height, index) => (
          <span key={index} className="w-[3px] rounded-sm bg-platform-accent" style={{ height, opacity: 0.35 + (index % 5) * 0.13 }} />
        ))}
      </div>
    );
  }
  if (item.category === "SKY") {
    const look = skyLook(item.key);
    return (
      <div className="h-[50px] rounded-md border border-platform-border" aria-hidden="true"
        style={{ background: look ? `linear-gradient(180deg, ${look.top}, ${look.bottom})` : "#05070a" }} />
    );
  }
  if (item.category === "AURA") {
    const look = auraLook(item.key);
    return (
      <div className="flex h-[50px] items-center justify-center rounded-md border border-platform-border bg-black/30" aria-hidden="true">
        <span className="h-7 w-7 rounded-full" style={{
          boxShadow: look ? `0 0 10px ${look.color}, inset 0 0 6px ${look.color}` : undefined,
          border: look ? `2px solid ${look.color}` : undefined
        }} />
      </div>
    );
  }
  if (item.category === "AMBIENT") {
    const look = ambientLook(item.key);
    const dots = [6, 18, 30, 42, 12, 24, 36];
    return (
      <div className="relative h-[50px] rounded-md border border-platform-border bg-black/30" aria-hidden="true">
        {dots.map((left, index) => (
          <span key={index} className="absolute rounded-full" style={{
            left: `${left}%`,
            top: `${15 + ((index * 23) % 60)}%`,
            width: 3 + (index % 3), height: 3 + (index % 3),
            background: look ? look.color : "#3a4655",
            opacity: 0.35 + (index % 4) * 0.15
          }} />
        ))}
      </div>
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
        const pinned = pinnedKeyFor(cosmetics, section.category);
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
                <span className="font-semibold text-gray-100">
                  {section.category === "SKY" || section.category === "AURA" || section.category === "AMBIENT" ? "None" : "Automatic"}
                </span>
                <span className="mt-1 block text-gray-400">
                  {section.category === "SKY" || section.category === "AURA" || section.category === "AMBIENT"
                    ? "The rank's plain look"
                    : "Follows your current rank"}
                </span>
                {pinned === null && <Check className="mt-1 h-4 w-4 text-platform-accent" aria-label="Selected" />}
              </button>

              {items.map((item) => {
                const selected = pinned === item.key;
                if (section.category === "ANTHEM") {
                  // a div card with two buttons (select + preview): no button-inside-button
                  const sounding = anthemController.getSnapshot() === item.key;
                  return (
                    <div
                      key={item.key}
                      className={`rounded-lg border p-2 text-left text-xs transition ${
                        selected ? "border-platform-accent bg-platform-accentSoft/40" : "border-platform-border"
                      }`}
                    >
                      <Preview item={item} />
                      <span className="mt-2 block font-semibold text-gray-100">{item.displayName}</span>
                      {item.owned ? (
                        <span className="block text-gray-400">{selected ? "In use" : item.rarity.charAt(0) + item.rarity.slice(1).toLowerCase()}</span>
                      ) : (
                        <span className="flex items-center gap-1 text-gray-500">
                          <Lock className="h-3 w-3" aria-hidden="true" />
                          {item.unlockRankName ? `Unlocks at ${item.unlockRankName}` : "From caches or crafting"}
                        </span>
                      )}
                      <span className="mt-2 flex items-center gap-1.5">
                        <button
                          type="button"
                          disabled={busy || !item.owned}
                          onClick={() => onPick(section.category, item.key)}
                          aria-pressed={selected}
                          className={`flex-1 rounded-md border px-2 py-1 font-semibold transition ${
                            selected
                              ? "border-platform-accent text-platform-accent"
                              : item.owned
                                ? "border-platform-border text-gray-200 hover:border-gray-500"
                                : "cursor-not-allowed border-platform-border text-gray-600"
                          }`}
                        >
                          {selected ? "Selected" : item.owned ? "Select" : "Locked"}
                        </button>
                        <button
                          type="button"
                          onClick={() => anthemController.toggle(item.key)}
                          aria-label={sounding ? `Stop preview of ${item.displayName}` : `Preview ${item.displayName}`}
                          aria-pressed={sounding}
                          className={`rounded-md border px-2 py-1 transition ${
                            sounding ? "border-platform-accent text-platform-accent" : "border-platform-border text-gray-300 hover:border-gray-500"
                          }`}
                        >
                          <Music2 className="h-3.5 w-3.5" aria-hidden="true" />
                        </button>
                      </span>
                    </div>
                  );
                }
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
                        {item.unlockRankName ? `Unlocks at ${item.unlockRankName}` : "From caches or crafting"}
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
