"use client";

import Link from "next/link";
import { FormEvent, useState } from "react";
import { api, getApiErrorMessage, setToken } from "@/lib/api";
import { useRouter } from "next/navigation";
import { persistAuthState } from "@/lib/auth";

export default function RegisterPage() {
  const router = useRouter();
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const isEmailValid = email.includes("@") && email.includes(".");
  const canSubmit = username.trim().length >= 3 && isEmailValid && password.length >= 8;

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!canSubmit || isSubmitting) return;

    setError("");
    setIsSubmitting(true);
    try {
      const { data } = await api.post("/auth/register", {
        username: username.trim(),
        email: email.trim(),
        password
      });
      persistAuthState({ token: data.token, username: data.username, role: data.role });
      setToken(data.token);
      router.push("/home");
    } catch (err) {
      setError(getApiErrorMessage(err, "Unable to create account"));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="flex min-h-[calc(100vh-7rem)] items-center justify-center">
      <section className="w-full max-w-md panel">
        <h1 className="text-2xl font-bold text-platform-accent">Register</h1>
        <form onSubmit={onSubmit} className="mt-4 space-y-3">
          <label className="block text-xs font-medium uppercase tracking-wide text-gray-400" htmlFor="username">
            Username
          </label>
          <input
            id="username"
            className="input-dark"
            placeholder="choose_username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
            minLength={3}
            suppressHydrationWarning
            required
          />

          <label className="block text-xs font-medium uppercase tracking-wide text-gray-400" htmlFor="email">
            Email
          </label>
          <input
            id="email"
            className="input-dark"
            type="email"
            placeholder="you@example.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            autoComplete="email"
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
            placeholder="Minimum 8 characters"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="new-password"
            minLength={8}
            suppressHydrationWarning
            required
          />
          <p className="text-xs text-gray-500">Use at least 8 characters for better account security.</p>
          {error && <p className="text-sm text-red-400">{error}</p>}
          <button className="btn-accent w-full disabled:cursor-not-allowed disabled:opacity-60" type="submit" disabled={!canSubmit || isSubmitting}>
            {isSubmitting ? "Creating Account..." : "Create Account"}
          </button>
          <p className="text-sm text-gray-300">
            Already have an account? <Link href="/login" className="text-platform-accent hover:underline">Login</Link>
          </p>
        </form>
      </section>
    </div>
  );
}
