"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AUTH_STATE_EVENT, getAuthState } from "@/lib/auth";
import { initApiAuth } from "@/lib/api";

export default function RequireAuth({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const [authorized, setAuthorized] = useState(false);
  const [checking, setChecking] = useState(true);

  useEffect(() => {
    initApiAuth();

    const syncAuthorization = () => {
      const authState = getAuthState();
      const hasToken = Boolean(authState?.token);

      if (!hasToken) {
        setAuthorized(false);
        router.replace("/login");
      } else {
        setAuthorized(true);
      }

      setChecking(false);
    };

    syncAuthorization();
    window.addEventListener(AUTH_STATE_EVENT, syncAuthorization);
    window.addEventListener("storage", syncAuthorization);

    return () => {
      window.removeEventListener(AUTH_STATE_EVENT, syncAuthorization);
      window.removeEventListener("storage", syncAuthorization);
    };
  }, [router]);

  if (checking || !authorized) return <div className="panel">Checking authentication...</div>;

  return <>{children}</>;
}
