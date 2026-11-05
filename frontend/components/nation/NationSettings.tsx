"use client";

import { useEffect, useMemo, useState } from "react";
import { getApiErrorMessage, updateNationProfile } from "@/lib/api";
import type { NationDto, UpdateNationProfilePayload } from "@/lib/nation";

type Props = {
  nation: NationDto;
  onUpdated: (nation: NationDto) => void;
};

const FALLBACK_ZONES = ["UTC", "Asia/Kolkata", "Europe/London", "Europe/Paris", "America/New_York", "America/Los_Angeles", "Asia/Tokyo", "Australia/Sydney"];

function listTimeZones(): string[] {
  try {
    if (typeof Intl.supportedValuesOf === "function") {
      const zones = Intl.supportedValuesOf("timeZone");
      return zones.includes("UTC") ? zones : ["UTC", ...zones];
    }
  } catch {
    // fall through to the short list
  }
  return FALLBACK_ZONES;
}

function browserTimeZone(): string | null {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || null;
  } catch {
    return null;
  }
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" });
}

/** Owner-only settings: nation name and time zone (both have cooldowns, enforced by the server). */
export default function NationSettings({ nation, onUpdated }: Props) {
  const [name, setName] = useState(nation.name);
  const [timeZone, setTimeZone] = useState(nation.timeZone);
  const [saving, setSaving] = useState(false);
  const [togglingVisibility, setTogglingVisibility] = useState(false);
  const [message, setMessage] = useState<{ kind: "ok" | "error"; text: string } | null>(null);

  useEffect(() => {
    setName(nation.name);
    setTimeZone(nation.timeZone);
  }, [nation.name, nation.timeZone]);

  const zones = useMemo(() => {
    const list = listTimeZones();
    return list.includes(nation.timeZone) ? list : [nation.timeZone, ...list];
  }, [nation.timeZone]);

  const browserZone = useMemo(() => browserTimeZone(), []);
  const renameLocked = nation.renameAvailableAt !== null;
  const zoneLocked = nation.timeZoneChangeAvailableAt !== null;
  const nameChanged = name.trim() !== nation.name;
  const zoneChanged = timeZone !== nation.timeZone;

  async function save() {
    const payload: UpdateNationProfilePayload = {};
    if (nameChanged && !renameLocked) payload.name = name;
    if (zoneChanged && !zoneLocked) payload.timeZone = timeZone;
    if (!payload.name && !payload.timeZone) return;

    setSaving(true);
    setMessage(null);
    try {
      const updated = await updateNationProfile(payload);
      onUpdated(updated);
      setMessage({ kind: "ok", text: "Saved." });
    } catch (error) {
      setMessage({ kind: "error", text: getApiErrorMessage(error, "Could not save your nation settings.") });
    } finally {
      setSaving(false);
    }
  }

  async function toggleAppearOffline(next: boolean) {
    setTogglingVisibility(true);
    setMessage(null);
    try {
      const updated = await updateNationProfile({ appearOffline: next });
      onUpdated(updated);
    } catch (error) {
      setMessage({ kind: "error", text: getApiErrorMessage(error, "Could not change your visibility.") });
    } finally {
      setTogglingVisibility(false);
    }
  }

  const canSave = !saving && ((nameChanged && !renameLocked) || (zoneChanged && !zoneLocked));

  return (
    <div>
      <h3 className="text-sm font-semibold uppercase tracking-wide text-gray-400">Nation settings</h3>
      <div className="mt-3 grid gap-4 md:grid-cols-2">
        <div>
          <label htmlFor="nation-name" className="mb-1 block text-sm text-gray-300">Nation name</label>
          <input
            id="nation-name"
            className="input-dark"
            value={name}
            maxLength={24}
            disabled={renameLocked || saving}
            onChange={(event) => setName(event.target.value)}
          />
          <p className="mt-1 text-xs text-gray-500">
            {renameLocked
              ? `You can rename again on ${formatDate(nation.renameAvailableAt as string)}.`
              : "3 to 24 characters: letters, digits, spaces, hyphens, apostrophes. Can be changed once every 7 days."}
          </p>
        </div>

        <div>
          <label htmlFor="nation-timezone" className="mb-1 block text-sm text-gray-300">Time zone</label>
          <select
            id="nation-timezone"
            className="input-dark"
            value={timeZone}
            disabled={zoneLocked || saving}
            onChange={(event) => setTimeZone(event.target.value)}
          >
            {zones.map((zone) => (
              <option key={zone} value={zone}>{zone}</option>
            ))}
          </select>
          <p className="mt-1 text-xs text-gray-500">
            {zoneLocked
              ? `You can change your time zone again on ${formatDate(nation.timeZoneChangeAvailableAt as string)}.`
              : "Decides when your day starts (daily easy limit, active days). Can be changed once every 30 days."}
          </p>
          {!zoneLocked && browserZone && browserZone !== nation.timeZone && browserZone !== timeZone && zones.includes(browserZone) && (
            <button type="button" className="mt-1 text-xs text-platform-accent underline" onClick={() => setTimeZone(browserZone)}>
              Use my browser time zone ({browserZone})
            </button>
          )}
        </div>
      </div>

      <div className="mt-4">
        <label className="flex cursor-pointer items-start gap-2 text-sm text-gray-300">
          <input
            type="checkbox"
            className="mt-1 h-4 w-4 accent-green-500"
            checked={nation.appearOffline}
            disabled={togglingVisibility}
            onChange={(event) => toggleAppearOffline(event.target.checked)}
          />
          <span>
            Appear offline
            <span className="block text-xs text-gray-500">
              Visitors always see you as offline, with no last-seen time. You still see your true status.
            </span>
          </span>
        </label>
      </div>

      <div className="mt-4 flex items-center gap-3">
        <button type="button" className="btn-accent disabled:cursor-not-allowed disabled:opacity-50" disabled={!canSave} onClick={save}>
          {saving ? "Saving..." : "Save"}
        </button>
        {message && (
          <span role="status" className={message.kind === "ok" ? "text-sm text-platform-accent" : "text-sm text-red-400"}>
            {message.text}
          </span>
        )}
      </div>
    </div>
  );
}
