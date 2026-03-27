"use client";

import { useEffect, useState } from "react";
import ReactMarkdown from "react-markdown";
import { api, getApiErrorMessage } from "@/lib/api";

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

type Props = {
  problemId: number;
};

export default function ProblemDescriptionPanel({ problemId }: Props) {
  const [problem, setProblem] = useState<ProblemDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    setLoading(true);
    setError("");

    api.get(`/problems/${problemId}`)
      .then((res) => {
        setProblem(res.data);
      })
      .catch((err) => {
        setProblem(null);
        setError(getApiErrorMessage(err, "Failed to load problem"));
      })
      .finally(() => setLoading(false));
  }, [problemId]);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-full">
        <p className="text-gray-400">Loading problem...</p>
      </div>
    );
  }

  if (!problem) {
    return (
      <div className="flex items-center justify-center h-full">
        <p className="text-red-400">{error || "Failed to load problem"}</p>
      </div>
    );
  }

  return (
    <div className="h-full overflow-y-auto bg-platform-bg p-4">
      <div className="max-w-full">
        {/* Problem Title */}
        <div className="mb-4">
          <h1 className="text-2xl font-bold text-platform-accent">{problem.title}</h1>
          <p className="text-sm text-gray-400 mt-1">
            {problem.difficulty} • {problem.tags.join(", ")}
          </p>
        </div>

        {/* Problem Description */}
        <div className="mb-6 prose prose-invert max-w-none prose-headings:text-platform-accent prose-p:text-gray-300 prose-ul:text-gray-300 prose-ol:text-gray-300 prose-strong:text-platform-text prose-code:text-platform-accent prose-code:bg-platform-panel prose-code:px-1 prose-code:rounded">
          <ReactMarkdown>{problem.description}</ReactMarkdown>
        </div>

        {/* Input/Output Format and Constraints */}
        <div className="space-y-4 mb-6">
          <div>
            <h3 className="font-semibold text-platform-accent text-sm mb-2 uppercase tracking-wide">
              Input Format
            </h3>
            <p className="text-sm text-gray-300">{problem.inputFormat}</p>
          </div>

          <div>
            <h3 className="font-semibold text-platform-accent text-sm mb-2 uppercase tracking-wide">
              Output Format
            </h3>
            <p className="text-sm text-gray-300">{problem.outputFormat}</p>
          </div>

          <div>
            <h3 className="font-semibold text-platform-accent text-sm mb-2 uppercase tracking-wide">
              Constraints
            </h3>
            <p className="text-sm text-gray-300">{problem.constraints}</p>
          </div>
        </div>

      </div>
    </div>
  );
}
