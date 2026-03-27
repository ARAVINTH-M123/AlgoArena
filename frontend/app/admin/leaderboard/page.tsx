"use client";

import { useEffect, useState } from "react";
import LeaderboardTable from "@/components/LeaderboardTable";
import RequireAdmin from "@/components/RequireAdmin";
import { api } from "@/lib/api";

type Row = {
  username: string;
  score: number;
  timePenalty: number;
};

export default function AdminLeaderboardPage() {
  const [rows, setRows] = useState<Row[]>([]);

  useEffect(() => {
    api.get("/leaderboard").then((res) => setRows(res.data)).catch(() => setRows([]));
  }, []);

  return (
    <RequireAdmin>
      <section className="space-y-3">
        <h1 className="text-2xl font-bold text-platform-accent">Leaderboard</h1>
        <LeaderboardTable rows={rows} />
      </section>
    </RequireAdmin>
  );
}
