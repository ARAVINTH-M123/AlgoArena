"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import CacheReveal from "@/components/nation/CacheReveal";
import {
  craftCosmetic,
  equipCosmetic,
  getApiErrorMessage,
  getCacheState,
  getMyCosmetics,
  openCache
} from "@/lib/api";
import {
  CATEGORY_LABELS,
  RARITY_ORDER,
  rarityColor,
  type CacheOpenResultDto,
  type CacheStateDto,
  type CosmeticsDto,
  type EquippableCategory,
  type Rarity
} from "@/lib/cosmetics";

const SCENE_CATEGORIES: EquippableCategory[] = ["SKY", "AURA", "AMBIENT"];

/** Rarity chip used across the page. */
function RarityChip({ rarity }: { rarity: string }) {
  const color = rarityColor(rarity);
  return (
    <span
      className="rounded-full border px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide"
      style={{ color, borderColor: color }}
    >
      {rarity}
    </span>
  );
}

export default function CollectionPage() {
  const [cosmetics, setCosmetics] = useState<CosmeticsDto | null>(null);
  const [caches, setCaches] = useState<CacheStateDto | null>(null);
  const [reveal, setReveal] = useState<CacheOpenResultDto | null>(null);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<{ kind: "ok" | "error"; text: string } | null>(null);
  const [needsLogin, setNeedsLogin] = useState(false);

  const load = useCallback(async () => {
    try {
      const [mine, state] = await Promise.all([getMyCosmetics(), getCacheState()]);
      setCosmetics(mine);
      setCaches(state);
    } catch (error) {
      const text = getApiErrorMessage(error, "Could not load your collection.");
      if (text.includes("login")) {
        setNeedsLogin(true);
      } else {
        setMessage({ kind: "error", text });
      }
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function act(action: () => Promise<unknown>, okText: string) {
    setBusy(true);
    setMessage(null);
    try {
      await action();
      await load();
      setMessage({ kind: "ok", text: okText });
    } catch (error) {
      setMessage({ kind: "error", text: getApiErrorMessage(error, "That did not work.") });
    } finally {
      setBusy(false);
    }
  }

  async function openOne() {
    setBusy(true);
    setMessage(null);
    try {
      const result = await openCache();
      setReveal(result);
      await load();
    } catch (error) {
      setMessage({ kind: "error", text: getApiErrorMessage(error, "Could not open the cache.") });
    } finally {
      setBusy(false);
    }
  }

  if (needsLogin) {
    return (
      <main className="mx-auto max-w-2xl px-4 py-16 text-center">
        <h1 className="text-xl font-bold text-platform-text">Your collection</h1>
        <p className="mt-3 text-sm text-gray-400">Log in to see your caches, shards and cosmetics.</p>
        <Link href="/login" className="mt-6 inline-block rounded-lg border border-platform-border px-4 py-2 text-sm text-platform-text hover:border-platform-accent">
          Go to login
        </Link>
      </main>
    );
  }

  const pool = (cosmetics?.items ?? []).filter((item) => SCENE_CATEGORIES.includes(item.category as EquippableCategory));
  const byRarity = (rarity: Rarity) => pool.filter((item) => item.rarity === rarity);
  const craftCostOf = (rarity: string) => caches?.craftCost[RARITY_ORDER.indexOf(rarity as Rarity)] ?? 0;
  const unopened = caches?.unopenedCaches ?? 0;

  return (
    <main className="mx-auto max-w-4xl space-y-8 px-4 py-8">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold text-platform-text">Your collection</h1>
          <p className="text-sm text-gray-400">Mystery Caches, shards and every scene cosmetic you own.</p>
        </div>
        <Link href="/profile" className="rounded-lg border border-platform-border px-3 py-2 text-xs text-platform-text hover:border-platform-accent">
          Nation settings
        </Link>
      </header>

      {/* ---------- caches and shards ---------- */}
      <section className="rounded-xl border border-platform-border bg-platform-panel p-4">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h2 className="text-sm font-semibold uppercase tracking-wide text-gray-400">Mystery Caches</h2>
            <p className="mt-1 text-2xl font-bold text-platform-text">
              {unopened} <span className="text-sm font-normal text-gray-400">unopened</span>
              <span className="mx-3 text-platform-border">|</span>
              {(caches?.shards ?? 0).toLocaleString("en-US")} <span className="text-sm font-normal text-gray-400">shards</span>
            </p>
          </div>
          <button
            type="button"
            disabled={busy || unopened <= 0}
            onClick={openOne}
            className="rounded-lg bg-platform-accent px-5 py-2.5 font-semibold text-black transition hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-40"
          >
            {unopened > 0 ? "Open a cache" : "No caches to open"}
          </button>
        </div>

        {/* pity progress */}
        <div className="mt-4 grid gap-3 text-xs text-gray-400 sm:grid-cols-2">
          <div>
            <p className="font-semibold text-gray-300">Drop pity</p>
            <p>{caches ? `${caches.solvesSinceCache} / ${caches.solvesForGuarantee} first-solves since the last cache` : "..."}</p>
            <div className="mt-1 h-1.5 w-full rounded bg-black">
              <div className="h-1.5 rounded bg-platform-accent" style={{
                width: caches ? `${Math.min(100, (caches.solvesSinceCache / caches.solvesForGuarantee) * 100)}%` : "0%"
              }} />
            </div>
          </div>
          <div>
            <p className="font-semibold text-gray-300">Rare pity</p>
            <p>{caches ? `${caches.opensSinceRare} / ${caches.opensForRarePity} opens without Rare or better` : "..."}</p>
            <div className="mt-1 h-1.5 w-full rounded bg-black">
              <div className="h-1.5 rounded bg-platform-accent" style={{
                width: caches ? `${Math.min(100, (caches.opensSinceRare / caches.opensForRarePity) * 100)}%` : "0%"
              }} />
            </div>
          </div>
        </div>

        {/* the exact odds, no secrets */}
        {caches && (
          <div className="mt-4 grid gap-4 border-t border-platform-border pt-3 text-xs text-gray-400 sm:grid-cols-3">
            <div>
              <p className="font-semibold text-gray-300">Drop chance per first-solve</p>
              <p>Easy {caches.dropPercent[0]}% - Medium {caches.dropPercent[1]}% - Hard {caches.dropPercent[2]}%</p>
            </div>
            <div>
              <p className="font-semibold text-gray-300">Open odds</p>
              <p>{RARITY_ORDER.map((rarity, index) => `${rarity.charAt(0)}${rarity.slice(1).toLowerCase()} ${caches.openPercent[index]}%`).join(" - ")}</p>
            </div>
            <div>
              <p className="font-semibold text-gray-300">Duplicates turn into shards</p>
              <p>{RARITY_ORDER.map((rarity, index) => `${rarity.charAt(0)}${rarity.slice(1).toLowerCase()} ${caches.duplicateShards[index]}`).join(" - ")}</p>
            </div>
          </div>
        )}
      </section>

      {message && (
        <p role="status" className={`text-sm ${message.kind === "ok" ? "text-platform-accent" : "text-red-400"}`}>{message.text}</p>
      )}

      {/* ---------- the cache pool by rarity, with crafting ---------- */}
      <section className="space-y-5">
        <h2 className="text-sm font-semibold uppercase tracking-wide text-gray-400">Cache cosmetics</h2>
        {RARITY_ORDER.map((rarity) => (
          <div key={rarity}>
            <div className="flex items-center gap-2">
              <RarityChip rarity={rarity} />
              <span className="text-xs text-gray-500">craft for {craftCostOf(rarity)} shards</span>
            </div>
            <div className="mt-2 grid grid-cols-2 gap-3 md:grid-cols-4">
              {byRarity(rarity).map((item) => (
                <div key={item.key} className={`rounded-lg border p-3 text-xs ${item.equipped ? "border-platform-accent bg-platform-accentSoft/40" : "border-platform-border"}`}>
                  <div className="flex items-center justify-between gap-2">
                    <span className="font-semibold text-platform-text">{item.displayName}</span>
                    <RarityChip rarity={item.rarity} />
                  </div>
                  <p className="mt-0.5 text-gray-500">{CATEGORY_LABELS[item.category] ?? item.category.toLowerCase()}</p>
                  <div className="mt-2 flex gap-2">
                    <button
                      type="button"
                      disabled={busy || !item.owned}
                      onClick={() => act(() => equipCosmetic(item.category as EquippableCategory, item.key), `${item.displayName} equipped.`)}
                      aria-pressed={item.equipped}
                      className={`flex-1 rounded-md border px-2 py-1 font-semibold transition ${
                        item.equipped
                          ? "border-platform-accent text-platform-accent"
                          : item.owned
                            ? "border-platform-border text-platform-text hover:border-gray-500"
                            : "cursor-not-allowed border-platform-border text-gray-600"
                      }`}
                    >
                      {item.equipped ? "In use" : item.owned ? "Equip" : "Locked"}
                    </button>
                    {!item.owned && (
                      <button
                        type="button"
                        disabled={busy || (caches?.shards ?? 0) < craftCostOf(item.rarity)}
                        onClick={() => act(() => craftCosmetic(item.key), `${item.displayName} crafted.`)}
                        className="rounded-md border border-platform-border px-2 py-1 text-platform-text transition hover:border-platform-accent disabled:cursor-not-allowed disabled:opacity-40"
                      >
                        Craft
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        ))}
      </section>

      {reveal && (
        <CacheReveal
          result={reveal}
          hasMore={reveal.unopenedCaches > 0}
          onOpenAnother={openOne}
          onClose={() => setReveal(null)}
        />
      )}
    </main>
  );
}
