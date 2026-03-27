"use client";

import { useEffect, useState } from "react";
import { usePathname } from "next/navigation";

export default function AppFrame({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);
  const showAppMenu = pathname !== "/" && pathname !== "/login" && pathname !== "/register" && pathname !== "/admin";
  const isSolvePage = pathname.includes("/solve");

  useEffect(() => {
    const syncSidebarState = () => {
      setIsSidebarCollapsed(localStorage.getItem("sidebarCollapsed") === "true");
    };

    syncSidebarState();
    window.addEventListener("sidebar-toggle", syncSidebarState);
    window.addEventListener("storage", syncSidebarState);

    return () => {
      window.removeEventListener("sidebar-toggle", syncSidebarState);
      window.removeEventListener("storage", syncSidebarState);
    };
  }, []);

  if (isSolvePage) {
    return (
      <main className={`fixed inset-0 top-16 left-0 right-0 bg-platform-bg ${
        showAppMenu ? (isSidebarCollapsed ? "md:left-14" : "md:left-56") : ""
      }`}>
        {children}
      </main>
    );
  }

  return (
    <main className={`mx-auto min-h-screen w-full max-w-7xl px-4 py-6 sm:px-6 ${showAppMenu ? (isSidebarCollapsed ? "md:pl-20" : "md:pl-64") : ""}`}>
      {children}
    </main>
  );
}
