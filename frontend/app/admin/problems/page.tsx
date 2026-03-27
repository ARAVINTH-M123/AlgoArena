"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { api, getApiErrorMessage, initApiAuth } from "@/lib/api";
import RequireAdmin from "@/components/RequireAdmin";

type Problem = {
  id: number;
  title: string;
  difficulty: string;
};

export default function ManageProblemsPage() {
  const [problems, setProblems] = useState<Problem[]>([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const filteredProblems = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return problems;

    return problems.filter((problem) =>
      problem.title.toLowerCase().includes(query) ||
      problem.difficulty.toLowerCase().includes(query) ||
      String(problem.id).includes(query)
    );
  }, [problems, search]);

  async function loadProblems() {
    setLoading(true);
    initApiAuth();
    const res = await api.get("/problems");
    setProblems(res.data.content ?? []);
    setLoading(false);
  }

  useEffect(() => {
    loadProblems().catch((err) => {
      setProblems([]);
      setError(getApiErrorMessage(err, "Unable to load problems"));
      setLoading(false);
    });
  }, []);

  return (
    <RequireAdmin>
      <section className="panel">
        <div className="flex items-center justify-between gap-4">
          <h1 className="text-2xl font-bold text-platform-accent">Problems</h1>
          <Link href="/admin/problems/create" className="btn-accent">Add New Problem</Link>
        </div>

        <div className="mt-4">
          <input
            className="input-dark"
            placeholder="Search by title, difficulty, or problem id"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>

        {error && <p className="mt-3 text-sm text-red-400">{error}</p>}

        <div className="mt-5 space-y-2">
          {loading && <p className="text-sm text-gray-400">Loading problems...</p>}

          {!loading && filteredProblems.length === 0 && !error && (
            <p className="text-sm text-gray-400">No problems available.</p>
          )}

          {filteredProblems.map((problem) => (
            <Link
              key={problem.id}
              href={`/admin/problems/${problem.id}`}
              className="flex items-center justify-between rounded-md border border-platform-border px-3 py-2 transition hover:border-platform-accent"
            >
              <div>
                <p className="font-medium text-white">{problem.title}</p>
                <p className="text-xs text-gray-400">{problem.difficulty}</p>
              </div>
            </Link>
          ))}
        </div>
      </section>
    </RequireAdmin>
  );
}
