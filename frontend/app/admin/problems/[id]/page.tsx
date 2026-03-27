"use client";

import Link from "next/link";
import { use, useEffect, useState } from "react";
import ReactMarkdown from "react-markdown";
import RequireAdmin from "@/components/RequireAdmin";
import { api, getApiErrorMessage, initApiAuth } from "@/lib/api";
import BackArrowButton from "@/components/BackArrowButton";

type ProblemDetail = {
  id: number;
  title: string;
  description: string;
  difficulty: string;
  tags: string[];
  inputFormat: string;
  outputFormat: string;
  constraints: string;
  sampleIo: string;
};

type TestCaseItem = {
  input: string;
  output: string;
};

export default function AdminProblemDetailsPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [problem, setProblem] = useState<ProblemDetail | null>(null);
  const [testCases, setTestCases] = useState<TestCaseItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  function parseHiddenTestCases(raw: string) {
    try {
      const parsed = JSON.parse(raw);
      if (!Array.isArray(parsed)) return [];

      return parsed
        .filter((item) => item && typeof item === "object")
        .map((item) => ({
          input: typeof item.input === "string" ? item.input : "",
          output: typeof item.output === "string" ? item.output : ""
        }))
        .filter((item) => item.output.trim().length > 0);
    } catch {
      return [];
    }
  }

  function parseLegacySampleIo(raw: string): TestCaseItem[] {
    const match = /(?:sample\s*)?input\s*[:\-]?\s*([\s\S]*?)\s*(?:sample\s*)?output\s*[:\-]?\s*([\s\S]*)/i.exec(raw || "");
    if (!match) return [];

    const input = match[1]?.trim() ?? "";
    const output = match[2]?.trim() ?? "";
    if (!output) return [];
    return [{ input, output }];
  }

  useEffect(() => {
    setLoading(true);
    setError("");

    initApiAuth();

    Promise.all([api.get(`/problems/${id}`), api.get(`/problems/admin/${id}`)])
      .then(([publicRes, adminRes]) => {
        const problemData = publicRes.data as ProblemDetail;
        setProblem(problemData);

        const parsedFromHidden = parseHiddenTestCases(adminRes.data?.hiddenTestCases ?? "");
        if (parsedFromHidden.length > 0) {
          setTestCases(parsedFromHidden);
        } else {
          setTestCases(parseLegacySampleIo(problemData.sampleIo ?? ""));
        }
      })
      .catch((err) => {
        setProblem(null);
        setTestCases([]);
        setError(getApiErrorMessage(err, "Unable to load problem details"));
      })
      .finally(() => setLoading(false));
  }, [id]);

  return (
    <RequireAdmin>
      <section className="space-y-4">
        <BackArrowButton fallbackHref="/admin/problems" />
        <div className="panel">
          <div className="flex items-start justify-between gap-4">
            <div>
              <h1 className="text-2xl font-bold text-platform-accent">{problem?.title ?? `Problem #${id}`}</h1>
              {problem && (
                <p className="mt-1 text-sm text-gray-400">
                  {problem.difficulty} • {(problem.tags ?? []).join(", ")}
                </p>
              )}
            </div>
            <Link href={`/admin/problems/${id}/edit`} className="btn-accent whitespace-nowrap">
              Edit Problem
            </Link>
          </div>
        </div>

        {loading && <div className="panel text-sm text-gray-400">Loading problem...</div>}
        {!loading && error && <div className="panel text-sm text-red-400">{error}</div>}

        {!loading && !error && problem && (
          <>
            <div className="panel">
              <h2 className="text-sm font-semibold uppercase tracking-wide text-platform-accent">Description</h2>
              <div className="mt-4 prose prose-invert max-w-none prose-headings:text-platform-accent prose-p:text-gray-300 prose-ul:text-gray-300 prose-ol:text-gray-300 prose-strong:text-platform-text prose-code:text-platform-accent prose-code:bg-platform-panel prose-code:px-1 prose-code:rounded">
                <ReactMarkdown>{problem.description}</ReactMarkdown>
              </div>
            </div>

            <div className="grid gap-4 md:grid-cols-3">
              <div className="panel">
                <h3 className="text-xs font-semibold uppercase tracking-wide text-platform-accent">Input Format</h3>
                <p className="mt-2 whitespace-pre-wrap text-sm text-gray-300">{problem.inputFormat || "-"}</p>
              </div>
              <div className="panel">
                <h3 className="text-xs font-semibold uppercase tracking-wide text-platform-accent">Output Format</h3>
                <p className="mt-2 whitespace-pre-wrap text-sm text-gray-300">{problem.outputFormat || "-"}</p>
              </div>
              <div className="panel">
                <h3 className="text-xs font-semibold uppercase tracking-wide text-platform-accent">Constraints</h3>
                <p className="mt-2 whitespace-pre-wrap text-sm text-gray-300">{problem.constraints || "-"}</p>
              </div>
            </div>

            <div className="panel">
              <h2 className="text-sm font-semibold uppercase tracking-wide text-platform-accent">Hidden Testcases</h2>
              <div className="mt-4 space-y-3">
                {testCases.length === 0 ? (
                  <p className="text-sm text-gray-400">No testcase data available.</p>
                ) : (
                  testCases.map((testCaseItem, index) => (
                    <div key={index} className="rounded-md border border-platform-border p-3">
                      <p className="text-xs font-semibold text-platform-accent">Testcase-{index + 1}</p>

                      <p className="mt-2 text-xs uppercase tracking-wide text-gray-400">Input</p>
                      <pre className="mt-1 max-h-32 overflow-auto rounded border border-platform-border bg-platform-bg p-2 text-xs text-gray-200">
                        {testCaseItem.input || "(empty)"}
                      </pre>

                      <p className="mt-2 text-xs uppercase tracking-wide text-gray-400">Expected Output</p>
                      <pre className="mt-1 max-h-32 overflow-auto rounded border border-platform-border bg-platform-bg p-2 text-xs text-gray-200">
                        {testCaseItem.output || "(empty)"}
                      </pre>
                    </div>
                  ))
                )}
              </div>
            </div>
          </>
        )}
      </section>
    </RequireAdmin>
  );
}
