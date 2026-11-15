"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter, usePathname } from "next/navigation";
import { AUTH_STATE_EVENT, getAuthState } from "@/lib/auth";
import {
  ADMIN_AUTH_STATE_EVENT,
  clearAdminAuthState,
  getAdminAuthState
} from "@/lib/adminAuth";
import { clearAdminToken, initAdminApiAuth, initApiAuth } from "@/lib/api";

const userLinks = [
  { href: "/problems", label: "Problem List" },
  { href: "/submissions", label: "Submissions" },
  { href: "/leaderboard", label: "Leaderboard" }
];

const adminLinks = [
  { href: "/admin", label: "Home" },
  { href: "/admin/problems", label: "Problems" },
  { href: "/admin/users", label: "Users" },
  { href: "/admin/leaderboard", label: "Leaderboard" }
];

export default function Navbar() {
  const router = useRouter();
  const pathname = usePathname();
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [username, setUsername] = useState("");
  const [role, setRole] = useState("");
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);
  const isAdminRoute = pathname.startsWith("/admin");
  const isAdminLoginPage = pathname === "/admin/login";
  const isAdminArea = isAdminRoute && !isAdminLoginPage;
  const isLandingPage = pathname === "/";
  const hideProfileButton = pathname === "/login" || pathname === "/register" || isAdminLoginPage || isLandingPage;
  const showAppMenu = pathname !== "/" && pathname !== "/login" && pathname !== "/register" && !isAdminLoginPage;
  const links = isAdminRoute ? adminLinks : userLinks;
  const profileHref = isAdminArea
    ? "/admin/profile"
    : role === "ADMIN"
      ? "/admin/profile"
      : "/profile";

  const isLinkActive = (href: string) =>
    href === "/admin" ? pathname === "/admin" : pathname === href || pathname.startsWith(`${href}/`);

  useEffect(() => {
    initApiAuth();
    initAdminApiAuth();
    syncIdentity();

    const onRouteChange = () => {
      syncIdentity();
    };
    onRouteChange();
  }, [pathname]);

  useEffect(() => {
    const syncAuthState = () => syncIdentity();
    window.addEventListener("storage", syncAuthState);
    window.addEventListener(AUTH_STATE_EVENT, syncAuthState);
    window.addEventListener(ADMIN_AUTH_STATE_EVENT, syncAuthState);
    return () => {
      window.removeEventListener("storage", syncAuthState);
      window.removeEventListener(AUTH_STATE_EVENT, syncAuthState);
      window.removeEventListener(ADMIN_AUTH_STATE_EVENT, syncAuthState);
    };
  }, []);

  useEffect(() => {
    const stored = localStorage.getItem("sidebarCollapsed");
    setIsSidebarCollapsed(stored === "true");

    const onSidebarToggle = () => {
      setIsSidebarCollapsed(localStorage.getItem("sidebarCollapsed") === "true");
    };

    window.addEventListener("sidebar-toggle", onSidebarToggle);
    window.addEventListener("storage", onSidebarToggle);
    return () => {
      window.removeEventListener("sidebar-toggle", onSidebarToggle);
      window.removeEventListener("storage", onSidebarToggle);
    };
  }, []);

  /** Admin area shows the console session; everywhere else the player session. */
  function syncIdentity() {
    if (pathname.startsWith("/admin") && pathname !== "/admin/login") {
      const adminState = getAdminAuthState();
      setIsLoggedIn(Boolean(adminState?.token));
      setUsername(adminState?.username ?? "");
      setRole(adminState?.role ?? "");
      return;
    }

    const authState = getAuthState();
    setIsLoggedIn(Boolean(authState?.token));
    setUsername(authState?.username ?? "");
    setRole(authState?.role ?? "");
  }

  const toggleSidebar = () => {
    const next = !isSidebarCollapsed;
    setIsSidebarCollapsed(next);
    localStorage.setItem("sidebarCollapsed", next ? "true" : "false");
    window.dispatchEvent(new Event("sidebar-toggle"));
  };

  function handleAdminLogout() {
    clearAdminToken();
    clearAdminAuthState();
    router.replace("/admin/login");
  }

  return (
    <>
      <header className="sticky top-0 z-50 border-b border-platform-border bg-black/95 backdrop-blur">
        <nav className="mx-auto flex w-full items-center justify-between px-4 py-3 md:px-8">
          <Link
            href={isLoggedIn ? (isAdminArea || role === "ADMIN" ? "/admin" : "/home") : "/"}
            className="text-lg font-bold text-platform-accent whitespace-nowrap"
          >
            {isAdminArea ? "AlgoArena · Console" : "AlgoArena"}
          </Link>
          <div className="flex items-center gap-3">
            {isLoggedIn && !hideProfileButton && (
              <>
                <span className="text-xs text-gray-400 hidden sm:inline">{username}</span>
                <Link href={profileHref} className="rounded-lg border border-platform-border px-3 py-2 text-xs sm:text-sm text-gray-200 hover:border-platform-accent transition">
                  Profile
                </Link>
                {isAdminArea && (
                  <button
                    type="button"
                    onClick={handleAdminLogout}
                    className="rounded-lg border border-platform-border px-3 py-2 text-xs sm:text-sm text-gray-200 transition hover:border-red-500/60 hover:text-red-300"
                  >
                    Logout
                  </button>
                )}
              </>
            )}
            {isAdminLoginPage && (
              <Link href="/login" className="rounded-lg border border-platform-border px-3 py-2 text-xs sm:text-sm text-gray-200 hover:border-platform-accent transition">
                Player Login
              </Link>
            )}
          </div>
        </nav>
      </header>

      {showAppMenu && (
        <aside
          className={`fixed left-0 top-[57px] z-40 flex h-[calc(100vh-57px)] flex-col border-r border-platform-border bg-platform-panel transition-all duration-300 ${
            isSidebarCollapsed ? "w-14 p-2" : "w-56 p-3"
          }`}
        >
          <button
            type="button"
            onClick={toggleSidebar}
            className={`mb-3 rounded-md border border-platform-border py-2 text-sm font-semibold text-platform-accent ${
              isSidebarCollapsed ? "px-0 text-center" : "px-3 text-left"
            }`}
            aria-label={isSidebarCollapsed ? "Expand menu" : "Collapse menu"}
          >
            {isSidebarCollapsed ? "☰" : "☰ Menu"}
          </button>

          <div className={`space-y-1 ${isSidebarCollapsed ? "hidden" : "block"}`}>
            {links
              .map((link) => (
                <Link
                  key={link.href}
                  href={link.href}
                  className={`block rounded-md px-3 py-2 text-sm transition ${
                    isLinkActive(link.href)
                      ? "bg-platform-accentSoft text-platform-accent"
                      : "text-gray-200 hover:bg-platform-accentSoft"
                  }`}
                >
                  {link.label}
                </Link>
              ))}
          </div>
        </aside>
      )}
    </>
  );
}
