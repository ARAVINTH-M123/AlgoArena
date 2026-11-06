"use client";

import { useCallback, useSyncExternalStore } from "react";
import { anthemController } from "@/lib/anthemPlayer";

/** Live access to the one anthem player: which anthem is sounding and the controls. */
export function useAnthem() {
  const playingKey = useSyncExternalStore(
    anthemController.subscribe,
    anthemController.getSnapshot,
    () => null                       // server snapshot: nothing can be playing on the server
  );

  const toggle = useCallback((key: string) => anthemController.toggle(key), []);
  const stop = useCallback(() => anthemController.stop(), []);

  return { playingKey, toggle, stop, muted: anthemController.isMuted(), setMuted: (muted: boolean) => anthemController.setMuted(muted) };
}
