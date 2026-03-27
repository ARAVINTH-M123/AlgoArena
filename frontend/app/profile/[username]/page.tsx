"use client";

import { use, useEffect, useState } from "react";
import { api } from "@/lib/api";
import BackArrowButton from "@/components/BackArrowButton";

type Profile = {
  username: string;
  streakDays: number;
  solvedProblems: number;
  acceptedSubmissions: number;
};

export default function ProfilePage({ params }: { params: Promise<{ username: string }> }) {
  const { username } = use(params);
  const [profile, setProfile] = useState<Profile | null>(null);

  useEffect(() => {
    api.get(`/users/${username}`).then((res) => setProfile(res.data));
  }, [username]);

  if (!profile) return <div className="panel">Loading profile...</div>;

  return (
    <section className="panel">
      <BackArrowButton fallbackHref="/leaderboard" className="mb-4" />
      <h1 className="text-2xl font-bold text-platform-accent">{profile.username}</h1>
      <div className="mt-4 grid gap-3 md:grid-cols-3">
        <div><p className="text-gray-400">Streak</p><p className="text-xl font-bold">{profile.streakDays}d</p></div>
        <div><p className="text-gray-400">Solved</p><p className="text-xl font-bold">{profile.solvedProblems}</p></div>
        <div><p className="text-gray-400">Accepted</p><p className="text-xl font-bold">{profile.acceptedSubmissions}</p></div>
      </div>
    </section>
  );
}
