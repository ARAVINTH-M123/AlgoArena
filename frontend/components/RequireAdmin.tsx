"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import {
  ADMIN_AUTH_STATE_EVENT,
  clearAdminAuthState,
  getAdminAuthState,
  isAdminSession
} from "@/lib/adminAuth";
import { adminApi, initAdminApiAuth } from "@/lib/api";

/**
 * Route guard for /admin/*.
 *
 * 1. No console session            -> /admin/login
 * 2. Session without ADMIN role    -> /home (normal users are kept out of the console)
 * 3. Session present               -> re-verified against the backend (GET /api/admin/me);
 *    a stale/expired token (401) sends the user back to /admin/login, a role mismatch
 *    (403) sends them back to /home.
 *
 * The stored role is only a routing hint — real authorization is enforced by the
 * backend on every /api/admin/** request.
 */
export default function RequireAdmin({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const [authorized, setAuthorized] = useState(false);
  const [checking, setChecking] = useState(true);

  const verify = useCallback(async () => {
    const state = getAdminAuthState();

    if (!state?.token) {
      setAuthorized(false);
      setChecking(false);
      router.replace("/admin/login");
      return;
    }

    if (!isAdminSession(state)) {
      setAuthorized(false);
      setChecking(false);
      router.replace("/home");
      return;
    }

    try {
      await adminApi.get("/admin/me");
      setAuthorized(true);
    } catch (err) {
      setAuthorized(false);
      clearAdminAuthState();

      const status = (err as { response?: { status?: number } })?.response?.status;
      if (status === 403) {
        router.replace("/home");
      } else {
        router.replace("/admin/login");
      }
    } finally {
      setChecking(false);
    }
  }, [router]);

  useEffect(() => {
    initAdminApiAuth();
    void verify();

    const sync = () => void verify();
    window.addEventListener(ADMIN_AUTH_STATE_EVENT, sync);
    window.addEventListener("storage", sync);
    return () => {
      window.removeEventListener(ADMIN_AUTH_STATE_EVENT, sync);
      window.removeEventListener("storage", sync);
    };
  }, [verify]);

  if (checking || !authorized) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <div className="panel flex items-center gap-3">
          <span className="h-2 w-2 animate-pulse rounded-full bg-platform-accent" aria-hidden="true" />
          <p className="text-sm text-gray-400">Verifying admin access...</p>
        </div>
      </div>
    );
  }

  return <>{children}</>;
}
