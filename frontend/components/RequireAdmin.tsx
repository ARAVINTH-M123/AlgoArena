"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

export default function RequireAdmin({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const [authorized, setAuthorized] = useState(false);

  useEffect(() => {
    const token = localStorage.getItem("token");
    const role = localStorage.getItem("role");
    if (!token) {
      router.replace("/admin");
      return;
    }
    if (role !== "ADMIN") {
      router.replace("/home");
      return;
    }
    setAuthorized(true);
  }, [router]);

  if (!authorized) return <div className="panel">Checking admin access...</div>;

  return <>{children}</>;
}
