"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import {
  BarChart3,
  Database,
  FileCode2,
  ListChecks,
  Loader2,
  LogOut,
  Megaphone,
  Plus,
  RefreshCw,
  ScrollText,
  Settings,
  ShieldCheck,
  Trophy,
  UserCog,
  Users,
  type LucideIcon
} from "lucide-react";
import RequireAdmin from "@/components/RequireAdmin";
import { adminApi, clearAdminToken, getApiErrorMessage, initAdminApiAuth } from "@/lib/api";
import { clearAdminAuthState, getAdminAuthState } from "@/lib/adminAuth";

type Stats = {
  users: number;
  problems: number;
  contests: number;
  submissions: number;
};

type ModuleLink = {
  title: string;
  description: string;
  icon: LucideIcon;
  href?: string;
  status: "active" | "planned";
};

/**
 * Module registry for the management console. Active modules link to their pages;
 * planned modules render disabled so the roadmap stays visible. New modules plug in
 * here plus a backend endpoint under /api/admin/** (same authorization boundary).
 * Contest management is intentionally excluded — it ships in a later phase.
 */
const MODULES: ModuleLink[] = [
  {
    title: "User Management",
    description: "Accounts, roles and membership overview.",
    icon: UserCog,
    href: "/admin/users",
    status: "active"
  },
  {
    title: "Problem Management",
    description: "Create, edit and remove problems.",
    icon: ListChecks,
    href: "/admin/problems",
    status: "active"
  },
  {
    title: "Submission Management",
    description: "Review and moderate player submissions.",
    icon: FileCode2,
    status: "planned"
  },
  {
    title: "Analytics",
    description: "Platform activity, trends and engagement.",
    icon: BarChart3,
    status: "planned"
  },
  {
    title: "Announcements",
    description: "Broadcast platform-wide notices.",
    icon: Megaphone,
    status: "planned"
  },
  {
    title: "Dataset Import",
    description: "Bulk import problems and reference data.",
    icon: Database,
    status: "planned"
  },
  {
    title: "System Settings",
    description: "Platform configuration and toggles.",
    icon: Settings,
    status: "planned"
  },
  {
    title: "Audit Logs",
    description: "Security events and administrative actions.",
    icon: ScrollText,
    status: "planned"
  }
];

const STAT_CARDS: { key: keyof Stats; label: string; icon: LucideIcon }[] = [
  { key: "users", label: "Users", icon: Users },
  { key: "problems", label: "Problems", icon: ListChecks },
  { key: "contests", label: "Contests", icon: Trophy },
  { key: "submissions", label: "Submissions", icon: FileCode2 }
];

function ManagementDashboard() {
  const router = useRouter();
  const [stats, setStats] = useState<Stats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [username, setUsername] = useState("");
  const [isLoggingOut, setIsLoggingOut] = useState(false);

  useEffect(() => {
    initAdminApiAuth();
    setUsername(getAdminAuthState()?.username ?? "");
  }, []);

  const loadStats = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const res = await adminApi.get("/admin/stats");
      setStats(res.data);
    } catch (err) {
      setStats(null);
      setError(getApiErrorMessage(err, "Unable to load platform statistics"));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadStats();
  }, [loadStats]);

  async function handleLogout() {
    if (isLoggingOut) return;
    setIsLoggingOut(true);
    clearAdminToken();
    clearAdminAuthState();
    router.replace("/admin/login");
  }

  return (
    <section className="space-y-6">
      {/* Header */}
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="text-[0.65rem] font-bold uppercase tracking-[0.25em] text-platform-accent">
            Management Console
          </p>
          <h1 className="mt-1 text-2xl font-bold text-platform-accent">Management Dashboard</h1>
          <p className="mt-1 text-sm text-gray-400">
            Oversee the AlgoArena platform from a single control center.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2.5 rounded-xl border border-platform-border bg-platform-panel px-3 py-2">
            <span className="flex h-8 w-8 items-center justify-center rounded-full bg-platform-accentSoft text-sm font-bold text-platform-accent">
              {(username || "A").charAt(0).toUpperCase()}
            </span>
            <div className="leading-tight">
              <p className="text-sm font-semibold text-platform-text">{username || "admin"}</p>
              <p className="flex items-center gap-1 text-[0.6rem] font-bold uppercase tracking-widest text-platform-accent">
                <ShieldCheck size={11} /> Admin
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={handleLogout}
            disabled={isLoggingOut}
            className="flex items-center gap-2 rounded-lg border border-platform-border px-3 py-2 text-sm text-gray-200 transition hover:border-red-500/60 hover:text-red-300 disabled:opacity-60"
          >
            {isLoggingOut ? <Loader2 size={15} className="animate-spin" /> : <LogOut size={15} />}
            Logout
          </button>
        </div>
      </div>

      {/* Platform statistics */}
      <div>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-sm font-semibold uppercase tracking-wider text-gray-300">Platform Overview</h2>
          <button
            type="button"
            onClick={() => void loadStats()}
            className="flex items-center gap-1.5 rounded-md border border-platform-border px-2.5 py-1 text-xs text-gray-300 transition hover:border-platform-accent hover:text-platform-accent"
          >
            <RefreshCw size={12} className={loading ? "animate-spin" : ""} />
            Refresh
          </button>
        </div>

        {error && (
          <div className="mb-3 flex flex-wrap items-center justify-between gap-2 rounded-lg border border-red-500/40 bg-red-500/10 px-3 py-2 text-sm text-red-400">
            <span>{error}</span>
            <button type="button" onClick={() => void loadStats()} className="text-xs font-semibold underline">
              Retry
            </button>
          </div>
        )}

        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {STAT_CARDS.map(({ key, label, icon: Icon }) => (
            <div key={key} className="panel flex items-center gap-3">
              <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg border border-platform-accent/30 bg-platform-accentSoft text-platform-accent">
                <Icon size={18} />
              </span>
              <div className="min-w-0">
                <p className="text-xs uppercase tracking-wide text-gray-400">{label}</p>
                {loading ? (
                  <span className="mt-1 block h-6 w-12 animate-pulse rounded bg-platform-accentSoft/60" aria-label={`Loading ${label}`} />
                ) : (
                  <p className="text-2xl font-bold text-platform-text">{stats?.[key] ?? 0}</p>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Quick actions */}
      <div className="flex flex-wrap items-center gap-3">
        <Link href="/admin/problems/create" className="btn-accent flex items-center gap-2 text-sm">
          <Plus size={15} />
          Add Problem
        </Link>
        <Link
          href="/admin/users"
          className="flex items-center gap-2 rounded-lg border border-platform-border px-4 py-2 text-sm text-gray-200 transition hover:border-platform-accent"
        >
          <Users size={15} />
          View Users
        </Link>
        <Link
          href="/admin/leaderboard"
          className="flex items-center gap-2 rounded-lg border border-platform-border px-4 py-2 text-sm text-gray-200 transition hover:border-platform-accent"
        >
          <Trophy size={15} />
          Leaderboard
        </Link>
        <Link
          href="/admin/profile"
          className="flex items-center gap-2 rounded-lg border border-platform-border px-4 py-2 text-sm text-gray-200 transition hover:border-platform-accent"
        >
          <UserCog size={15} />
          Console Profile
        </Link>
      </div>

      {/* Modules */}
      <div>
        <h2 className="mb-3 text-sm font-semibold uppercase tracking-wider text-gray-300">Modules</h2>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {MODULES.map(({ title, description, icon: Icon, href, status }) => {
            const body = (
              <>
                <div className="flex items-center justify-between gap-2">
                  <span className="flex h-9 w-9 items-center justify-center rounded-lg border border-platform-accent/30 bg-platform-accentSoft text-platform-accent">
                    <Icon size={17} />
                  </span>
                  {status === "planned" && (
                    <span className="rounded-full border border-platform-border px-2 py-0.5 text-[0.6rem] font-bold uppercase tracking-widest text-gray-500">
                      Planned
                    </span>
                  )}
                </div>
                <p className="mt-3 font-semibold text-platform-text">{title}</p>
                <p className="mt-1 text-xs leading-relaxed text-gray-400">{description}</p>
              </>
            );

            if (status === "active" && href) {
              return (
                <Link
                  key={title}
                  href={href}
                  className="panel block transition-all duration-300 hover:-translate-y-0.5 hover:border-platform-accent"
                >
                  {body}
                </Link>
              );
            }

            return (
              <div key={title} className="panel cursor-default opacity-60" aria-disabled="true">
                {body}
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}

export default function ManagementDashboardPage() {
  return (
    <RequireAdmin>
      <ManagementDashboard />
    </RequireAdmin>
  );
}
