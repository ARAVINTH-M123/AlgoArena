"use client";

import { useEffect, useState } from "react";
import LeaderboardTable from "@/components/LeaderboardTable";
import { api } from "@/lib/api";

type Row = {
  username: string;
  score: number;
  timePenalty: number;
};

export default function LeaderboardPage() {
  const [rows, setRows] = useState<Row[]>([]);

  useEffect(() => {
    api.get("/leaderboard").then((res) => setRows(res.data)).catch(() => setRows([]));
  }, []);

  return (
    <section className="space-y-3">
      <h1 className="text-2xl font-bold text-platform-accent">Global Leaderboard</h1>
      <LeaderboardTable rows={rows} />
    </section>
  );
}
