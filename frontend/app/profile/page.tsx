"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import RequireAuth from "@/components/RequireAuth";
import NationSection from "@/components/nation/NationSection";
import { api, clearToken, getApiErrorMessage, initApiAuth, setToken } from "@/lib/api";
import { clearAuthState, getAuthState, persistAuthState } from "@/lib/auth";

type Profile = {
  username: string;
  streakDays: number;
  solvedProblems: number;
  acceptedSubmissions: number;
};

type UsernameUpdateResponse = {
  token: string;
  username: string;
  role: string;
};

export default function ProfilePage() {
  const router = useRouter();
  const [profile, setProfile] = useState<Profile | null>(null);
  const [editingUsername, setEditingUsername] = useState(false);
  const [nextUsername, setNextUsername] = useState("");
  const [savingUsername, setSavingUsername] = useState(false);
  const [toastMessage, setToastMessage] = useState("");
  const [toastProgress, setToastProgress] = useState(100);
  const [error, setError] = useState("");
  const toastTimeoutRef = useRef<number | null>(null);

  useEffect(() => {
    initApiAuth();

    const username = getAuthState()?.username;
    if (!username) {
      router.replace("/login");
      return;
    }

    api.get<Profile>(`/users/${encodeURIComponent(username)}`)
      .then((res) => setProfile(res.data))
      .catch((err) => {
        setProfile(null);
        setError(getApiErrorMessage(err, "Unable to load profile"));
      });
  }, [router]);

  useEffect(() => {
    return () => {
      if (toastTimeoutRef.current !== null) window.clearTimeout(toastTimeoutRef.current);
    };
  }, []);

  function handleLogout() {
    clearToken();
    clearAuthState();
    router.push("/login");
  }

  function showSuccessToast(message: string) {
    if (toastTimeoutRef.current !== null) window.clearTimeout(toastTimeoutRef.current);

    setToastMessage(message);
    setToastProgress(100);
    window.setTimeout(() => setToastProgress(0), 20);
    toastTimeoutRef.current = window.setTimeout(() => {
      setToastMessage("");
      setToastProgress(100);
      toastTimeoutRef.current = null;
    }, 3000);
  }

  async function handleSaveUsername() {
    const username = nextUsername.trim();
    if (username.length < 3) {
      setError("Username must contain at least 3 characters");
      return;
    }
    if (username.length > 50) {
      setError("Username cannot exceed 50 characters");
      return;
    }
    if (!profile || username === profile.username) {
      setEditingUsername(false);
      setError("");
      return;
    }

    setError("");
    setSavingUsername(true);
    try {
      const { data } = await api.put<UsernameUpdateResponse>("/users/me/username", { username });
      persistAuthState({ token: data.token, username: data.username, role: data.role });
      setToken(data.token);
      setProfile((current) => current ? { ...current, username: data.username } : current);
      setEditingUsername(false);
      setNextUsername(data.username);
      showSuccessToast("Username updated");
    } catch (err) {
      setError(getApiErrorMessage(err, "Unable to update username"));
    } finally {
      setSavingUsername(false);
    }
  }

  return (
    <RequireAuth>
      <div className="space-y-6">
        <section className="panel">
          {!profile ? (
            <p className="text-gray-300">{error || "Loading profile..."}</p>
          ) : (
            <>
              <div className="flex items-start justify-between gap-4">
                <div>
                  <div className="flex flex-wrap items-center gap-2">
                    {!editingUsername ? (
                      <>
                        <h1 className="text-2xl font-bold text-platform-accent">{profile.username}</h1>
                        <button
                          type="button"
                          className="rounded border border-platform-border px-2 py-1 text-xs text-gray-200 transition hover:border-platform-accent"
                          onClick={() => {
                            setNextUsername(profile.username);
                            setEditingUsername(true);
                            setError("");
                          }}
                        >
                          Edit username
                        </button>
                      </>
                    ) : (
                      <>
                        <input
                          className="input-dark w-56"
                          aria-label="New username"
                          value={nextUsername}
                          onChange={(event) => setNextUsername(event.target.value)}
                          minLength={3}
                          maxLength={50}
                          autoFocus
                        />
                        <button type="button" className="btn-accent px-3 py-1 text-xs disabled:opacity-60" onClick={handleSaveUsername} disabled={savingUsername}>
                          {savingUsername ? "Checking..." : "Save"}
                        </button>
                        <button
                          type="button"
                          className="rounded border border-platform-border px-2 py-1 text-xs text-gray-200 transition hover:border-platform-accent"
                          onClick={() => {
                            setEditingUsername(false);
                            setNextUsername(profile.username);
                            setError("");
                          }}
                        >
                          Cancel
                        </button>
                      </>
                    )}
                  </div>
                  <p className="mt-1 text-sm text-gray-400">Your account profile</p>
                  {editingUsername && <p className="mt-1 text-xs text-gray-500">3–50 characters. Availability is verified before the change is saved.</p>}
                </div>
                <button type="button" className="rounded-lg border border-platform-border px-4 py-2 text-sm text-gray-200 hover:border-platform-accent" onClick={handleLogout}>
                  Logout
                </button>
              </div>

              {error && <p className="mt-3 text-sm text-red-400">{error}</p>}

              <div className="mt-4 grid gap-3 md:grid-cols-3">
                <div><p className="text-gray-400">Streak</p><p className="text-xl font-bold">{profile.streakDays}d</p></div>
                <div><p className="text-gray-400">Solved</p><p className="text-xl font-bold">{profile.solvedProblems}</p></div>
                <div><p className="text-gray-400">Accepted</p><p className="text-xl font-bold">{profile.acceptedSubmissions}</p></div>
              </div>
            </>
          )}
        </section>

        {profile && <NationSection username={profile.username} />}

        {profile && (
          <section className="grid gap-3 sm:grid-cols-2" aria-label="Profile actions">
            <Link href="/collection" className="panel group block hover:border-platform-accent">
              <p className="text-xs font-semibold uppercase tracking-wider text-platform-accent">Collection</p>
              <h2 className="mt-1 font-bold text-white group-hover:text-platform-accent">View caches and cosmetics</h2>
              <p className="mt-1 text-sm text-gray-400">Open Mystery Caches, craft rewards, and browse everything you own.</p>
            </Link>
            <Link href="/nation/settings" className="panel group block hover:border-platform-accent">
              <p className="text-xs font-semibold uppercase tracking-wider text-platform-accent">Nation settings</p>
              <h2 className="mt-1 font-bold text-white group-hover:text-platform-accent">Edit your nation</h2>
              <p className="mt-1 text-sm text-gray-400">Change your nation identity, Stealth mode, flag, and equipped cosmetics.</p>
            </Link>
          </section>
        )}
      </div>

      {toastMessage && (
        <div className="fixed right-4 top-20 z-50 w-72 overflow-hidden rounded-md border border-green-500/40 bg-platform-panel shadow-lg">
          <p className="px-3 py-2 text-sm text-green-300">{toastMessage}</p>
          <div className="h-1 bg-green-900/50">
            <div className="h-full bg-green-400" style={{ width: `${toastProgress}%`, transition: "width 3s linear" }} />
          </div>
        </div>
      )}
    </RequireAuth>
  );
}
