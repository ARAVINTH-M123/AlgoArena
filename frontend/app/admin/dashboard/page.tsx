"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import RequireAdmin from "@/components/RequireAdmin";

type Stats = {
  users: number;
  problems: number;
  contests: number;
  submissions: number;
};

export default function AdminDashboardPage() {
  const [stats, setStats] = useState<Stats | null>(null);

  useEffect(() => {
    const token = localStorage.getItem("token");
    if (token) api.defaults.headers.common.Authorization = `Bearer ${token}`;
    api.get("/admin/stats").then((res) => setStats(res.data)).catch(() => setStats(null));
  }, []);

  return (
    <RequireAdmin>
      <section className="space-y-4">
        <h1 className="text-2xl font-bold text-platform-accent">Admin Dashboard</h1>
        <p className="text-gray-300">Manage platform content and users from one place.</p>

        <div className="grid gap-4 md:grid-cols-4">
          <div className="panel"><p className="text-gray-400">Users</p><p className="text-2xl font-bold">{stats?.users ?? 0}</p></div>
          <div className="panel"><p className="text-gray-400">Problems</p><p className="text-2xl font-bold">{stats?.problems ?? 0}</p></div>
          <div className="panel"><p className="text-gray-400">Contests</p><p className="text-2xl font-bold">{stats?.contests ?? 0}</p></div>
          <div className="panel"><p className="text-gray-400">Submissions</p><p className="text-2xl font-bold">{stats?.submissions ?? 0}</p></div>
        </div>
      </section>
    </RequireAdmin>
  );
}
