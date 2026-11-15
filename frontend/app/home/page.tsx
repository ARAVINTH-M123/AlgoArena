"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import RequireAuth from "@/components/RequireAuth";
import { getAuthState } from "@/lib/auth";

export default function HomePage() {
  const [username, setUsername] = useState("coder");

  useEffect(() => {
    setUsername(getAuthState()?.username ?? "coder");
  }, []);

  return (
    <RequireAuth>
      <section className="space-y-6">
        <div className="panel overflow-hidden p-0">
          <div className="border-b border-platform-border bg-gradient-to-br from-green-500/15 via-transparent to-transparent px-6 py-10 md:px-10 md:py-14">
            <p className="text-xs font-semibold uppercase tracking-[0.25em] text-platform-accent">Welcome to the arena</p>
            <h1 className="mt-3 text-3xl font-bold tracking-tight text-white md:text-5xl">
              Hello, <span className="text-platform-accent">{username}</span>
            </h1>
            <p className="mt-4 max-w-2xl text-base leading-7 text-gray-300 md:text-lg">
              AlgoArena is where consistent problem solving becomes visible progress. Practice algorithms, sharpen your coding skills, and turn every accepted solution into momentum.
            </p>
            <div className="mt-7 flex flex-wrap gap-3">
              <Link href="/problems" className="btn-accent">Start solving</Link>
              <Link href="/profile" className="rounded-lg border border-platform-border px-4 py-2 font-semibold text-gray-200 transition hover:border-platform-accent hover:text-platform-accent">
                View profile
              </Link>
            </div>
          </div>
        </div>

        <div className="grid gap-4 md:grid-cols-3">
          <article className="panel">
            <p className="text-xs font-semibold uppercase tracking-wider text-platform-accent">Practice</p>
            <h2 className="mt-2 text-lg font-bold">Solve with purpose</h2>
            <p className="mt-2 text-sm leading-6 text-gray-400">Explore problems across difficulties and topics, submit Java solutions, and learn from immediate judge feedback.</p>
          </article>
          <article className="panel">
            <p className="text-xs font-semibold uppercase tracking-wider text-platform-accent">Compete</p>
            <h2 className="mt-2 text-lg font-bold">Measure your progress</h2>
            <p className="mt-2 text-sm leading-6 text-gray-400">Build streaks, review submissions, and climb the leaderboard as your problem-solving record grows.</p>
          </article>
          <article className="panel">
            <p className="text-xs font-semibold uppercase tracking-wider text-platform-accent">Build</p>
            <h2 className="mt-2 text-lg font-bold">Grow your nation</h2>
            <p className="mt-2 text-sm leading-6 text-gray-400">Accepted solutions earn Nation XP, construct new landmarks, unlock cosmetics, and evolve your personal coding world.</p>
          </article>
        </div>
      </section>
    </RequireAuth>
  );
}
