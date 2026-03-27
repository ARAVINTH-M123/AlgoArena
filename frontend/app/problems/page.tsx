"use client";

import { useEffect, useState } from "react";
import { api, getApiErrorMessage } from "@/lib/api";
import ProblemTable from "@/components/ProblemTable";

type Problem = {
  id: number;
  title: string;
  difficulty: "EASY" | "MEDIUM" | "HARD";
  tags: string[];
};

export default function ProblemsPage() {
  const [problems, setProblems] = useState<Problem[]>([]);
  const [search, setSearch] = useState("");
  const [difficulty, setDifficulty] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    setLoading(true);
    setError("");

    api.get("/problems", { params: { search: search.trim() || undefined, difficulty: difficulty || undefined } })
      .then((res) => setProblems(res.data.content ?? []))
      .catch((err) => {
        setProblems([]);
        setError(getApiErrorMessage(err, "Failed to load problems"));
      })
      .finally(() => setLoading(false));
  }, [search, difficulty]);

  return (
    <section className="space-y-4">
      <div className="panel flex flex-col gap-3 md:flex-row">
        <input className="input-dark" placeholder="Search problems" value={search} onChange={(e) => setSearch(e.target.value)} />
        <select className="input-dark md:w-56" value={difficulty} onChange={(e) => setDifficulty(e.target.value)}>
          <option value="">All Difficulties</option>
          <option value="EASY">Easy</option>
          <option value="MEDIUM">Medium</option>
          <option value="HARD">Hard</option>
        </select>
      </div>
      {error && <div className="panel border-red-500/40 text-sm text-red-300">{error}</div>}
      {loading ? <div className="panel text-sm text-gray-400">Loading problems...</div> : <ProblemTable problems={problems} />}
    </section>
  );
}
