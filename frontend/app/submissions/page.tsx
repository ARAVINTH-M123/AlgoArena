"use client";

import { useEffect, useState } from "react";
import { api, getApiErrorMessage, initApiAuth } from "@/lib/api";
import RequireAuth from "@/components/RequireAuth";

type Submission = {
  id: number;
  problemTitle: string;
  language: string;
  verdict: string;
  submittedAt: string;
};

export default function SubmissionsPage() {
  const [submissions, setSubmissions] = useState<Submission[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    initApiAuth();
    setLoading(true);
    setError("");

    api.get("/submissions/me")
      .then((res) => setSubmissions(res.data))
      .catch((err) => {
        setSubmissions([]);
        setError(getApiErrorMessage(err, "Failed to load submissions"));
      })
      .finally(() => setLoading(false));
  }, []);

  const verdictClass = (verdict: string) => {
    if (verdict === "ACCEPTED") return "text-green-400";
    if (verdict === "WRONG_ANSWER") return "text-red-400";
    return "text-yellow-400";
  };

  return (
    <RequireAuth>
      <section className="panel overflow-x-auto">
        <h1 className="mb-3 text-2xl font-bold text-platform-accent">My Submissions</h1>
        {error && <p className="mb-3 text-sm text-red-300">{error}</p>}
        {loading ? (
          <p className="text-sm text-gray-400">Loading submissions...</p>
        ) : submissions.length === 0 ? (
          <p className="text-sm text-gray-400">No submissions yet. Try solving a problem first.</p>
        ) : (
        <table className="w-full text-sm">
          <thead className="border-b border-platform-border text-gray-400">
            <tr>
              <th className="py-2 text-left">Problem</th>
              <th className="py-2 text-left">Language</th>
              <th className="py-2 text-left">Verdict</th>
              <th className="py-2 text-left">Time</th>
            </tr>
          </thead>
          <tbody>
            {submissions.map((submission) => (
              <tr key={submission.id} className="border-b border-platform-border/60">
                <td className="py-3">{submission.problemTitle}</td>
                <td className="py-3">{submission.language}</td>
                <td className={`py-3 font-medium ${verdictClass(submission.verdict)}`}>{submission.verdict}</td>
                <td className="py-3">{new Date(submission.submittedAt).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
        )}
      </section>
    </RequireAuth>
  );
}
