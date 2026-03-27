"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { api } from "@/lib/api";

type Contest = {
  id: number;
  name: string;
  description: string;
  startTime: string;
  endTime: string;
};

export default function ContestListPage() {
  const [contests, setContests] = useState<Contest[]>([]);

  useEffect(() => {
    api.get("/contests").then((res) => setContests(res.data)).catch(() => setContests([]));
  }, []);

  return (
    <section className="space-y-3">
      <h1 className="text-2xl font-bold text-platform-accent">Contests</h1>
      {contests.map((contest) => (
        <div key={contest.id} className="panel">
          <h3 className="text-lg font-semibold text-platform-accent">{contest.name}</h3>
          <p className="text-gray-300">{contest.description}</p>
          <Link href={`/contests/${contest.id}`} className="mt-3 inline-block text-platform-accent hover:underline">View Contest</Link>
        </div>
      ))}
    </section>
  );
}
