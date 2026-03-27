"use client";

import Link from "next/link";
import { FormEvent, useState } from "react";
import { api, getApiErrorMessage, setToken } from "@/lib/api";
import { useRouter } from "next/navigation";
import { persistAuthState } from "@/lib/auth";

export default function LoginPage() {
  const router = useRouter();
  const [usernameOrEmail, setUsernameOrEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const canSubmit = usernameOrEmail.trim().length > 0 && password.length >= 6;

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!canSubmit || isSubmitting) return;

    setError("");
    setIsSubmitting(true);
    try {
      const { data } = await api.post("/auth/login", {
        usernameOrEmail: usernameOrEmail.trim(),
        password
      });
      persistAuthState({ token: data.token, username: data.username, role: data.role });
      setToken(data.token);
      router.push("/home");
    } catch (err) {
      setError(getApiErrorMessage(err, "Unable to sign in"));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="flex min-h-[calc(100vh-7rem)] items-center justify-center">
      <section className="w-full max-w-md panel">
        <h1 className="text-2xl font-bold text-platform-accent">Login</h1>
        <form onSubmit={onSubmit} className="mt-4 space-y-3">
          <label className="block text-xs font-medium uppercase tracking-wide text-gray-400" htmlFor="usernameOrEmail">
            Username or Email
          </label>
          <input
            id="usernameOrEmail"
            className="input-dark"
            placeholder="you@example.com"
            value={usernameOrEmail}
            onChange={(e) => setUsernameOrEmail(e.target.value)}
            autoComplete="username"
            suppressHydrationWarning
            required
          />

          <label className="block text-xs font-medium uppercase tracking-wide text-gray-400" htmlFor="password">
            Password
          </label>
          <input
            id="password"
            className="input-dark"
            type="password"
            placeholder="Enter your password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            suppressHydrationWarning
            required
          />
          {error && <p className="text-sm text-red-400">{error}</p>}
          <button className="btn-accent w-full disabled:cursor-not-allowed disabled:opacity-60" type="submit" disabled={!canSubmit || isSubmitting}>
            {isSubmitting ? "Signing In..." : "Sign In"}
          </button>
          <p className="text-sm text-gray-300">
            Don&apos;t have an account? <Link href="/register" className="text-platform-accent hover:underline">Sign up</Link>
          </p>
        </form>
      </section>
    </div>
  );
}
