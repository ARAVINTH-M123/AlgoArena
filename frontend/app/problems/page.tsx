"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { Filter, RotateCcw, X } from "lucide-react";
import { api, getApiErrorMessage } from "@/lib/api";
import ProblemTable from "@/components/ProblemTable";

type Difficulty = "EASY" | "MEDIUM" | "HARD";

type Problem = {
  id: number;
  title: string;
  difficulty: Difficulty;
  tags: string[];
};

const LOAD_SIZE = 30;
const DIFFICULTIES: Difficulty[] = ["EASY", "MEDIUM", "HARD"];

export default function ProblemsPage() {
  const [problems, setProblems] = useState<Problem[]>([]);
  const [search, setSearch] = useState("");
  const [difficulty, setDifficulty] = useState<Difficulty | "">("");
  const [selectedTags, setSelectedTags] = useState<string[]>([]);
  const [filterOpen, setFilterOpen] = useState(false);
  const [visibleCount, setVisibleCount] = useState(LOAD_SIZE);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const filterRef = useRef<HTMLDivElement>(null);
  const loadMoreRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const controller = new AbortController();
    const timer = window.setTimeout(() => {
      setLoading(true);
      setError("");

      api.get("/problems", {
        params: { search: search.trim() || undefined },
        signal: controller.signal
      })
        .then((res) => setProblems(res.data.content ?? []))
        .catch((err) => {
          if (controller.signal.aborted) return;
          setProblems([]);
          setError(getApiErrorMessage(err, "Failed to load problems"));
        })
        .finally(() => {
          if (!controller.signal.aborted) setLoading(false);
        });
    }, 250);

    return () => {
      window.clearTimeout(timer);
      controller.abort();
    };
  }, [search]);

  useEffect(() => {
    function closeOnOutsideClick(event: MouseEvent) {
      if (filterRef.current && !filterRef.current.contains(event.target as Node)) setFilterOpen(false);
    }

    function closeOnEscape(event: KeyboardEvent) {
      if (event.key === "Escape") setFilterOpen(false);
    }

    document.addEventListener("mousedown", closeOnOutsideClick);
    document.addEventListener("keydown", closeOnEscape);
    return () => {
      document.removeEventListener("mousedown", closeOnOutsideClick);
      document.removeEventListener("keydown", closeOnEscape);
    };
  }, []);

  const availableTags = useMemo(() => {
    return Array.from(new Set(problems.flatMap((problem) => problem.tags).filter(Boolean)))
      .sort((a, b) => a.localeCompare(b));
  }, [problems]);

  const filteredProblems = useMemo(() => {
    return problems.filter((problem) => {
      const matchesDifficulty = !difficulty || problem.difficulty === difficulty;
      const matchesTags = selectedTags.length === 0 || selectedTags.some((tag) => problem.tags.includes(tag));
      return matchesDifficulty && matchesTags;
    });
  }, [difficulty, problems, selectedTags]);

  useEffect(() => {
    setVisibleCount(LOAD_SIZE);
  }, [search, difficulty, selectedTags]);

  const hasMore = visibleCount < filteredProblems.length;

  useEffect(() => {
    const target = loadMoreRef.current;
    if (!target || !hasMore) return;

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting) {
          setVisibleCount((current) => Math.min(current + LOAD_SIZE, filteredProblems.length));
        }
      },
      { rootMargin: "300px" }
    );

    observer.observe(target);
    return () => observer.disconnect();
  }, [filteredProblems.length, hasMore]);

  const visibleProblems = filteredProblems.slice(0, visibleCount);
  const activeFilterCount = (difficulty ? 1 : 0) + selectedTags.length;

  function toggleTag(tag: string) {
    setSelectedTags((current) => current.includes(tag) ? current.filter((item) => item !== tag) : [...current, tag]);
  }

  function resetFilters() {
    setDifficulty("");
    setSelectedTags([]);
  }

  return (
    <section className="space-y-4">
      <div className="panel flex flex-col gap-3 md:flex-row md:items-center">
        <input
          className="input-dark"
          placeholder="Search problems"
          aria-label="Search problems"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />

        <div ref={filterRef} className="relative md:w-56">
          <button
            type="button"
            className={`flex w-full items-center justify-between rounded-lg border px-3 py-2 text-sm transition ${
              activeFilterCount ? "border-platform-accent bg-platform-accentSoft text-platform-accent" : "border-platform-border bg-black text-gray-200 hover:border-platform-accent"
            }`}
            aria-expanded={filterOpen}
            aria-controls="problem-filter-panel"
            onClick={() => setFilterOpen((open) => !open)}
          >
            <span className="flex items-center gap-2"><Filter className="h-4 w-4" /> Filters</span>
            {activeFilterCount > 0 && <span className="rounded-full bg-platform-accent px-2 py-0.5 text-xs font-bold text-black">{activeFilterCount}</span>}
          </button>

          {filterOpen && (
            <div id="problem-filter-panel" className="absolute right-0 top-12 z-30 w-full min-w-72 rounded-xl border border-platform-border bg-platform-panel p-4 shadow-2xl md:w-80">
              <div className="flex items-center justify-between">
                <h2 className="font-semibold text-white">Filter problems</h2>
                <button type="button" className="text-gray-400 transition hover:text-white" aria-label="Close filters" onClick={() => setFilterOpen(false)}>
                  <X className="h-4 w-4" />
                </button>
              </div>

              <fieldset className="mt-4">
                <legend className="text-xs font-semibold uppercase tracking-wider text-gray-400">Difficulty</legend>
                <div className="mt-2 grid grid-cols-2 gap-2">
                  <button type="button" onClick={() => setDifficulty("")} className={`rounded-md border px-2 py-1.5 text-xs ${!difficulty ? "border-platform-accent text-platform-accent" : "border-platform-border text-gray-300"}`}>All</button>
                  {DIFFICULTIES.map((item) => (
                    <button key={item} type="button" onClick={() => setDifficulty(item)} className={`rounded-md border px-2 py-1.5 text-xs ${difficulty === item ? "border-platform-accent text-platform-accent" : "border-platform-border text-gray-300"}`}>
                      {item.charAt(0) + item.slice(1).toLowerCase()}
                    </button>
                  ))}
                </div>
              </fieldset>

              <fieldset className="mt-4">
                <legend className="text-xs font-semibold uppercase tracking-wider text-gray-400">Tags</legend>
                <p className="mt-1 text-xs text-gray-500">Match any selected tag</p>
                <div className="mt-2 max-h-48 space-y-1 overflow-y-auto pr-1">
                  {availableTags.length === 0 ? (
                    <p className="text-xs text-gray-500">No tags available.</p>
                  ) : availableTags.map((tag) => (
                    <label key={tag} className="flex cursor-pointer items-center gap-2 rounded-md px-2 py-1.5 text-sm text-gray-300 hover:bg-platform-accentSoft">
                      <input type="checkbox" className="h-4 w-4 accent-green-500" checked={selectedTags.includes(tag)} onChange={() => toggleTag(tag)} />
                      <span>{tag}</span>
                    </label>
                  ))}
                </div>
              </fieldset>

              <button type="button" className="mt-4 flex items-center gap-2 text-xs text-gray-400 transition hover:text-platform-accent disabled:opacity-40" disabled={activeFilterCount === 0} onClick={resetFilters}>
                <RotateCcw className="h-3.5 w-3.5" /> Reset filters
              </button>
            </div>
          )}
        </div>
      </div>

      {error && <div className="panel border-red-500/40 text-sm text-red-300">{error}</div>}
      {loading ? (
        <div className="panel text-sm text-gray-400">Loading problems...</div>
      ) : (
        <>
          <div className="flex items-center justify-between px-1 text-xs text-gray-500">
            <span>{filteredProblems.length} problem{filteredProblems.length === 1 ? "" : "s"}</span>
            {activeFilterCount > 0 && <span>{activeFilterCount} active filter{activeFilterCount === 1 ? "" : "s"}</span>}
          </div>
          <ProblemTable problems={visibleProblems} />
          <div ref={loadMoreRef} className="flex min-h-10 items-center justify-center text-xs text-gray-500" aria-live="polite">
            {hasMore ? "Loading more problems..." : filteredProblems.length > 0 ? "You reached the end." : ""}
          </div>
        </>
      )}
    </section>
  );
}
