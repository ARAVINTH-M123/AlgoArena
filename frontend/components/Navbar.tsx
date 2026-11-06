"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { usePathname } from "next/navigation";
import { AUTH_STATE_EVENT, getAuthState } from "@/lib/auth";
import { initApiAuth } from "@/lib/api";

const userLinks = [
  { href: "/problems", label: "Problem List" },
  { href: "/submissions", label: "Submissions" },
  { href: "/leaderboard", label: "Leaderboard" },
  { href: "/collection", label: "Collection" }
];

const adminLinks = [
  { href: "/admin/dashboard", label: "Home" },
  { href: "/admin/problems", label: "Problems" },
  { href: "/admin/contests", label: "Contest" },
  { href: "/admin/leaderboard", label: "Leaderboard" }
];

export default function Navbar() {
  const pathname = usePathname();
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [username, setUsername] = useState("");
  const [role, setRole] = useState("");
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);
  const isAdminRoute = pathname.startsWith("/admin");
  const isAdminLoginPage = pathname === "/admin";
  const isLandingPage = pathname === "/";
  const hideProfileButton = pathname === "/login" || pathname === "/register" || isAdminLoginPage || isLandingPage;
  const showAppMenu = pathname !== "/" && pathname !== "/login" && pathname !== "/register" && !isAdminLoginPage;
  const links = isAdminRoute ? adminLinks : userLinks;
  const profileHref = role === "ADMIN"
    ? "/admin/profile"
    : username
      ? `/profile/${encodeURIComponent(username)}`
      : "/profile";

  const isLinkActive = (href: string) => pathname === href || pathname.startsWith(`${href}/`);

  useEffect(() => {
    initApiAuth();

    const authState = getAuthState();
    setIsLoggedIn(Boolean(authState?.token));
    setUsername(authState?.username ?? "");
    setRole(authState?.role ?? "");
  }, [pathname]);

  useEffect(() => {
    const syncAuthState = () => {
      const authState = getAuthState();
      setIsLoggedIn(Boolean(authState?.token));
      setUsername(authState?.username ?? "");
      setRole(authState?.role ?? "");
    };

    syncAuthState();
    window.addEventListener("storage", syncAuthState);
    window.addEventListener(AUTH_STATE_EVENT, syncAuthState);
    return () => {
      window.removeEventListener("storage", syncAuthState);
      window.removeEventListener(AUTH_STATE_EVENT, syncAuthState);
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

  const toggleSidebar = () => {
    const next = !isSidebarCollapsed;
    setIsSidebarCollapsed(next);
    localStorage.setItem("sidebarCollapsed", next ? "true" : "false");
    window.dispatchEvent(new Event("sidebar-toggle"));
  };

  return (
    <>
      <header className="sticky top-0 z-50 border-b border-platform-border bg-black/95 backdrop-blur">
        <nav className="mx-auto flex w-full items-center justify-between px-4 py-3 md:px-8">
          <Link href={isLoggedIn ? (role === "ADMIN" ? "/admin/dashboard" : "/home") : "/"} className="text-lg font-bold text-platform-accent whitespace-nowrap">
            {isLoggedIn ? "AlgoArena" : "AlgoArena"}
          </Link>
          <div className="flex items-center gap-4">
            {isLoggedIn && !hideProfileButton && (
              <>
                <span className="text-xs text-gray-400 hidden sm:inline">{username}</span>
                <Link href={profileHref} className="rounded-lg border border-platform-border px-3 py-2 text-xs sm:text-sm text-gray-200 hover:border-platform-accent transition">
                  Profile
                </Link>
              </>
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
