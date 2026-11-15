"use client";

import { useMemo, useState } from "react";
import { useEffect } from "react";
import { Search, ShieldCheck, User as UserIcon } from "lucide-react";
import RequireAdmin from "@/components/RequireAdmin";
import { adminApi, getApiErrorMessage, initAdminApiAuth } from "@/lib/api";

type AdminUser = {
  id: number;
  username: string;
  email: string;
  role: string;
  rating: number;
  streakDays: number;
  createdAt: string | null;
};

function formatJoined(value: string | null) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  return date.toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" });
}

function roleBadgeClass(role: string) {
  return role === "ADMIN"
    ? "border-platform-accent/40 bg-platform-accentSoft text-platform-accent"
    : "border-platform-border bg-platform-panel text-gray-400";
}

export default function ManageUsersPage() {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");

  useEffect(() => {
    let cancelled = false;
    initAdminApiAuth();
    setLoading(true);
    setError("");

    adminApi
      .get("/admin/users")
      .then((res) => {
        if (!cancelled) setUsers(res.data ?? []);
      })
      .catch((err) => {
        if (!cancelled) {
          setUsers([]);
          setError(getApiErrorMessage(err, "Unable to load users"));
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const filteredUsers = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return users;

    return users.filter(
      (user) =>
        user.username.toLowerCase().includes(query) ||
        user.email.toLowerCase().includes(query) ||
        user.role.toLowerCase().includes(query)
    );
  }, [users, search]);

  return (
    <RequireAdmin>
      <section className="space-y-4">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h1 className="text-2xl font-bold text-platform-accent">User Management</h1>
            <p className="mt-1 text-sm text-gray-400">
              {loading ? "Loading accounts..." : `${users.length} account${users.length === 1 ? "" : "s"} registered`}
            </p>
          </div>
          <div className="relative w-full sm:w-72">
            <Search size={14} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
            <input
              className="input-dark pl-9"
              placeholder="Search username, email or role"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
        </div>

        {error && (
          <div className="rounded-lg border border-red-500/40 bg-red-500/10 px-3 py-2 text-sm text-red-400" role="alert">
            {error}
          </div>
        )}

        <div className="panel overflow-hidden p-0">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[640px] text-left text-sm">
              <thead>
                <tr className="border-b border-platform-border text-[0.65rem] uppercase tracking-widest text-gray-500">
                  <th className="px-4 py-3 font-semibold">User</th>
                  <th className="px-4 py-3 font-semibold">Email</th>
                  <th className="px-4 py-3 font-semibold">Role</th>
                  <th className="px-4 py-3 text-right font-semibold">Rating</th>
                  <th className="px-4 py-3 text-right font-semibold">Streak</th>
                  <th className="px-4 py-3 text-right font-semibold">Joined</th>
                </tr>
              </thead>
              <tbody>
                {loading && (
                  Array.from({ length: 3 }).map((_, index) => (
                    <tr key={index} className="border-b border-platform-border/50">
                      <td colSpan={6} className="px-4 py-3">
                        <span className="block h-4 w-full max-w-md animate-pulse rounded bg-platform-accentSoft/40" />
                      </td>
                    </tr>
                  ))
                )}

                {!loading && filteredUsers.length === 0 && !error && (
                  <tr>
                    <td colSpan={6} className="px-4 py-6 text-center text-gray-400">
                      {search ? "No accounts match your search." : "No accounts registered yet."}
                    </td>
                  </tr>
                )}

                {!loading &&
                  filteredUsers.map((user) => (
                    <tr key={user.id} className="border-b border-platform-border/50 transition-colors last:border-0 hover:bg-platform-accentSoft/20">
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2.5">
                          <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-platform-accentSoft text-xs font-bold text-platform-accent">
                            {user.username.charAt(0).toUpperCase()}
                          </span>
                          <div className="flex items-center gap-1.5">
                            <span className="font-medium text-platform-text">{user.username}</span>
                            {user.role === "ADMIN" && <ShieldCheck size={13} className="text-platform-accent" />}
                          </div>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-gray-400">{user.email}</td>
                      <td className="px-4 py-3">
                        <span className={`inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-[0.6rem] font-bold uppercase tracking-widest ${roleBadgeClass(user.role)}`}>
                          {user.role === "ADMIN" ? <ShieldCheck size={11} /> : <UserIcon size={11} />}
                          {user.role}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-right font-semibold text-platform-text">{user.rating}</td>
                      <td className="px-4 py-3 text-right text-gray-300">{user.streakDays}d</td>
                      <td className="px-4 py-3 text-right text-gray-400">{formatJoined(user.createdAt)}</td>
                    </tr>
                  ))}
              </tbody>
            </table>
          </div>
        </div>

        <p className="text-xs text-gray-500">
          Account actions (block/unblock, role changes, deletion) arrive with the full user-management module.
        </p>
      </section>
    </RequireAdmin>
  );
}
