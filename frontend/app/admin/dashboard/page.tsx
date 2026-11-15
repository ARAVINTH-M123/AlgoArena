"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

/**
 * Legacy admin dashboard URL. The console home now lives at /admin;
 * keep this redirect so old bookmarks keep working.
 */
export default function AdminDashboardRedirectPage() {
  const router = useRouter();

  useEffect(() => {
    router.replace("/admin");
  }, [router]);

  return (
    <div className="flex min-h-[50vh] items-center justify-center">
      <div className="panel flex items-center gap-3">
        <span className="h-2 w-2 animate-pulse rounded-full bg-platform-accent" aria-hidden="true" />
        <p className="text-sm text-gray-400">Redirecting to the Management Dashboard...</p>
      </div>
    </div>
  );
}
