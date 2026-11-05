"use client";

import { useEffect, useRef } from "react";
import { presenceStreamUrl } from "@/lib/api";
import type { PresenceDto } from "@/lib/presence";

const POLL_INTERVAL_MS = 60_000;

/**
 * Live presence of someone else's nation through Server-Sent Events. If the stream cannot be opened (or breaks),
 * it falls back to polling every 60 s until the stream works again.
 */
export function usePresenceStream(
  username: string,
  enabled: boolean,
  onPresence: (presence: PresenceDto) => void,
  poll: () => Promise<PresenceDto>
) {
  const onPresenceRef = useRef(onPresence);
  const pollRef = useRef(poll);
  useEffect(() => {
    onPresenceRef.current = onPresence;
    pollRef.current = poll;
  });

  useEffect(() => {
    if (!enabled) return;

    let pollTimer: number | undefined;
    const startPolling = () => {
      if (pollTimer !== undefined) return;
      pollTimer = window.setInterval(() => {
        pollRef.current()
          .then((presence) => onPresenceRef.current(presence))
          .catch(() => undefined);
      }, POLL_INTERVAL_MS);
    };
    const stopPolling = () => {
      if (pollTimer === undefined) return;
      window.clearInterval(pollTimer);
      pollTimer = undefined;
    };

    if (typeof EventSource === "undefined") {
      startPolling();
      return stopPolling;
    }

    const source = new EventSource(presenceStreamUrl(username));
    source.addEventListener("presence", (event) => {
      try {
        onPresenceRef.current(JSON.parse((event as MessageEvent<string>).data) as PresenceDto);
      } catch {
        // ignore a malformed event
      }
    });
    source.onopen = stopPolling;
    source.onerror = startPolling;   // the browser retries by itself; poll in the meantime

    return () => {
      source.close();
      stopPolling();
    };
  }, [username, enabled]);
}
