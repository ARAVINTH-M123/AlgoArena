"use client";

import { useEffect, useState } from "react";
import { getApiErrorMessage, getNation } from "@/lib/api";
import type { NationDto } from "@/lib/nation";
import type { PresenceDto } from "@/lib/presence";
import { presenceLabel } from "@/lib/presence";
import { usePresenceStream } from "@/hooks/usePresenceStream";
import { usePresenceView } from "@/hooks/usePresenceView";
import CosmeticPicker from "@/components/nation/CosmeticPicker";
import NationScene from "@/components/nation/NationScene";
import NationSettings from "@/components/nation/NationSettings";
import PresenceStatusLine from "@/components/nation/PresenceStatusLine";
import RankBadge from "@/components/nation/RankBadge";
import RankRequirements from "@/components/nation/RankRequirements";
import TrackProgress from "@/components/nation/TrackProgress";
import XpBar from "@/components/nation/XpBar";

type Props = {
  username: string;
};

const OWNER_REFRESH_MS = 30_000;

/** The whole Nation block of the profile page. */
export default function NationSection({ username }: Props) {
  const [nation, setNation] = useState<NationDto | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setNation(null);
    setError(null);

    getNation(username)
      .then((data) => {
        if (!cancelled) setNation(data);
      })
      .catch((err) => {
        if (!cancelled) setError(getApiErrorMessage(err, "Could not load this nation."));
      });

    return () => {
      cancelled = true;
    };
  }, [username]);

  if (error) {
    return <section className="panel text-sm text-red-400">{error}</section>;
  }
  if (!nation) {
    return <section className="panel text-sm text-gray-400">Loading nation...</section>;
  }

  return <NationBody key={nation.username} initial={nation} />;
}

function NationBody({ initial }: { initial: NationDto }) {
  const [nation, setNation] = useState<NationDto>(initial);
  const [live, setLive] = useState<{ presence: PresenceDto; receivedAt: number }>({
    presence: initial.presence,
    receivedAt: Date.now()
  });

  // whenever a fresh nation payload arrives (first load, settings saved, owner refresh), take its presence
  useEffect(() => {
    setLive({ presence: nation.presence, receivedAt: Date.now() });
  }, [nation.presence]);

  // owners see their own true status: refresh the nation every 30 s (plus once shortly after load, because the
  // page's own first heartbeat may not have been processed yet when the nation was first requested)
  useEffect(() => {
    if (!nation.owner) return;
    const refresh = () => {
      getNation(nation.username).then(setNation).catch(() => undefined);
    };
    const first = window.setTimeout(refresh, 2000);
    const timer = window.setInterval(refresh, OWNER_REFRESH_MS);
    return () => {
      window.clearTimeout(first);
      window.clearInterval(timer);
    };
  }, [nation.owner, nation.username]);

  // visitors get live changes through the stream (with polling as a fallback)
  usePresenceStream(
    nation.username,
    !nation.owner,
    (presence) => setLive({ presence, receivedAt: Date.now() }),
    () => getNation(nation.username).then((data) => data.presence)
  );

  const { view, nowMs } = usePresenceView(live.presence, live.receivedAt);
  const lastActiveText = presenceLabel({ ...view, status: "OFFLINE" }, nowMs).replace("Offline · ", "");

  return (
    <section className="panel space-y-5" aria-label="Nation">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-xl font-bold text-platform-accent">Nation</h2>
        <RankBadge name={nation.name} rankName={nation.rankName} rankIndex={nation.rankIndex} tier={nation.tier} />
      </div>

      <NationScene nation={nation} presence={view} lastActiveText={lastActiveText} />
      <PresenceStatusLine presence={view} nowMs={nowMs} hiddenFromVisitors={nation.owner && nation.appearOffline} />

      <XpBar nation={nation} />

      <div className="grid gap-6 md:grid-cols-2">
        <RankRequirements nation={nation} />
        <TrackProgress nation={nation} />
      </div>

      <dl className="grid grid-cols-2 gap-3 text-sm md:grid-cols-5">
        <div><dt className="text-gray-400">Easy</dt><dd className="text-lg font-bold">{nation.solves.easy}</dd></div>
        <div><dt className="text-gray-400">Medium</dt><dd className="text-lg font-bold">{nation.solves.medium}</dd></div>
        <div><dt className="text-gray-400">Hard</dt><dd className="text-lg font-bold">{nation.solves.hard}</dd></div>
        <div><dt className="text-gray-400">Active days</dt><dd className="text-lg font-bold">{nation.solves.activeDays}</dd></div>
        <div><dt className="text-gray-400">Items built</dt><dd className="text-lg font-bold">{nation.completedItems.length}</dd></div>
      </dl>

      {nation.owner && <NationSettings nation={nation} onUpdated={setNation} />}
      {nation.owner && (
        <CosmeticPicker
          onChanged={(cosmetics) => setNation((current) => ({ ...current, themeKey: cosmetics.themeKey, borderKey: cosmetics.borderKey }))}
        />
      )}
    </section>
  );
}
