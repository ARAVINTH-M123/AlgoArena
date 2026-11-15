"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import RequireAuth from "@/components/RequireAuth";
import CosmeticPicker from "@/components/nation/CosmeticPicker";
import FlagEditor from "@/components/nation/FlagEditor";
import NationSettings from "@/components/nation/NationSettings";
import { getApiErrorMessage, getMyNation } from "@/lib/api";
import type { NationDto } from "@/lib/nation";

export default function NationSettingsPage() {
  const [nation, setNation] = useState<NationDto | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: "auto" });

    let cancelled = false;

    getMyNation()
      .then((data) => {
        if (!cancelled) setNation(data);
      })
      .catch((err) => {
        if (!cancelled) setError(getApiErrorMessage(err, "Could not load your nation settings."));
      });

    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <RequireAuth>
      <main className="space-y-5">
        <header className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-platform-accent">Nation control</p>
            <h1 className="mt-1 text-2xl font-bold">Edit your nation</h1>
            <p className="mt-1 text-sm text-gray-400">Manage your nation identity, visibility, flag, and cosmetics.</p>
          </div>
          <Link href="/profile" className="rounded-lg border border-platform-border px-4 py-2 text-sm text-gray-200 transition hover:border-platform-accent">
            Back to profile
          </Link>
        </header>

        {error && <section className="panel border-red-500/40 text-sm text-red-300">{error}</section>}
        {!nation && !error && <section className="panel text-sm text-gray-400">Loading nation settings...</section>}

        {nation && (
          <>
            <section className="panel">
              <NationSettings nation={nation} onUpdated={setNation} />
            </section>
            <section className="panel">
              <FlagEditor nation={nation} onUpdated={setNation} />
            </section>
            <section className="panel">
              <CosmeticPicker
                onChanged={(cosmetics) =>
                  setNation((current) => current ? {
                    ...current,
                    themeKey: cosmetics.themeKey,
                    borderKey: cosmetics.borderKey,
                    anthemKey: cosmetics.anthemKey,
                    skyKey: cosmetics.skyKey,
                    auraKey: cosmetics.auraKey,
                    ambientKey: cosmetics.ambientKey
                  } : current)
                }
              />
            </section>
          </>
        )}
      </main>
    </RequireAuth>
  );
}
