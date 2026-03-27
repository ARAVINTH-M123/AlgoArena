"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import RequireAdmin from "@/components/RequireAdmin";

export default function ManageUsersPage() {
  const [users, setUsers] = useState<string[]>([]);

  useEffect(() => {
    const token = localStorage.getItem("token");
    if (token) api.defaults.headers.common.Authorization = `Bearer ${token}`;
    api.get("/users").then((res) => setUsers(res.data)).catch(() => setUsers([]));
  }, []);

  return (
    <RequireAdmin>
      <section className="panel">
        <h1 className="text-2xl font-bold text-platform-accent">Manage Users</h1>
        <ul className="mt-3 space-y-2 text-gray-300">
          {users.map((user) => (
            <li key={user} className="rounded-md border border-platform-border px-3 py-2">{user}</li>
          ))}
        </ul>
      </section>
    </RequireAdmin>
  );
}
