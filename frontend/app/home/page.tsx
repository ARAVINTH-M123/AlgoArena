"use client";

import { useEffect, useState } from "react";
import RequireAuth from "@/components/RequireAuth";
import { api, getApiErrorMessage, initApiAuth } from "@/lib/api";
import { getAuthState } from "@/lib/auth";

type Profile = {
  username: string;
  streakDays: number;
  solvedProblems: number;
  acceptedSubmissions: number;
};

export default function HomePage() {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    initApiAuth();
    const username = getAuthState()?.username;
    if (!username) return;

    api.get(`/users/${username}`)
      .then((res) => setProfile(res.data))
      .catch((err) => {
        setProfile(null);
        setError(getApiErrorMessage(err, "Unable to load your profile summary"));
      })
      .finally(() => setLoading(false));
  }, []);

  return (
    <RequireAuth>
      <section className="space-y-4">
        <div className="panel">
          <h2 className="text-xl font-bold text-platform-accent">Welcome Back</h2>
          <p className="mt-2 text-gray-300">
            {profile ? `Keep going, ${profile.username}.` : "Track your streak and solve new problems."}
          </p>
          {error && <p className="mt-2 text-sm text-red-300">{error}</p>}
        </div>
        <div className="grid gap-4 md:grid-cols-2">
          <div className="panel"><p className="text-sm text-gray-400">Solved</p><p className="text-2xl font-bold">{loading ? "..." : profile?.solvedProblems ?? 0}</p></div>
          <div className="panel"><p className="text-sm text-gray-400">Streak</p><p className="text-2xl font-bold">{loading ? "..." : `${profile?.streakDays ?? 0} days`}</p></div>
        </div>
      </section>
    </RequireAuth>
  );
}
