"use client";

import { useEffect, useState } from "react";
import { AUTH_STATE_EVENT, getAuthState } from "@/lib/auth";
import { useHeartbeat } from "@/hooks/useHeartbeat";

/** Invisible. Keeps the logged-in user's presence alive from every page. */
export default function PresenceHeartbeat() {
  const [loggedIn, setLoggedIn] = useState(false);

  useEffect(() => {
    const sync = () => setLoggedIn(Boolean(getAuthState()?.token));
    sync();
    window.addEventListener(AUTH_STATE_EVENT, sync);
    window.addEventListener("storage", sync);
    return () => {
      window.removeEventListener(AUTH_STATE_EVENT, sync);
      window.removeEventListener("storage", sync);
    };
  }, []);

  useHeartbeat(loggedIn);
  return null;
}
