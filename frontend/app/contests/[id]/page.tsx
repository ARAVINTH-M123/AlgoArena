"use client";

import { use, useEffect, useState } from "react";
import { api } from "@/lib/api";
import ContestTimer from "@/components/ContestTimer";
import Link from "next/link";
import BackArrowButton from "@/components/BackArrowButton";

type Contest = {
  id: number;
  name: string;
  description: string;
  endTime: string;
};

export default function ContestDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [contest, setContest] = useState<Contest | null>(null);
  const [problemIds, setProblemIds] = useState<number[]>([]);

  useEffect(() => {
    api.get(`/contests/${id}`).then((res) => setContest(res.data));
    api.get(`/contests/${id}/problems`).then((res) => setProblemIds(res.data));
  }, [id]);

  if (!contest) return <div className="panel">Loading contest...</div>;

  return (
    <section className="space-y-4">
      <BackArrowButton fallbackHref="/contests" />
      <div className="panel flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-platform-accent">{contest.name}</h1>
          <p className="text-gray-300">{contest.description}</p>
        </div>
        <ContestTimer endTime={contest.endTime} />
      </div>
      <div className="panel">
        <h3 className="font-semibold text-platform-accent">Contest Problems</h3>
        <ul className="mt-2 space-y-2">
          {problemIds.map((problemId) => (
            <li key={problemId}>
              <Link href={`/contests/${id}/problems/${problemId}`} className="text-platform-accent hover:underline">
                Problem #{problemId}
              </Link>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
