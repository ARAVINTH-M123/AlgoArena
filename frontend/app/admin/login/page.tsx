"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useEffect, useState } from "react";
import { Eye, EyeOff, Loader2, Lock, Mail, ShieldCheck } from "lucide-react";
import { adminApi, getApiErrorMessage, initAdminApiAuth, setAdminToken } from "@/lib/api";
import {
  ADMIN_AUTH_STATE_EVENT,
  clearAdminAuthState,
  getAdminAuthState,
  isAdminSession,
  persistAdminAuthState
} from "@/lib/adminAuth";

const EMAIL_PATTERN = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

export default function AdminLoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Already holding a verified console session? Go straight to the dashboard.
  useEffect(() => {
    initAdminApiAuth();
    const state = getAdminAuthState();
    if (!isAdminSession(state)) {
      if (state) clearAdminAuthState();
      return;
    }

    let cancelled = false;
    adminApi
      .get("/admin/me")
      .then(() => {
        if (!cancelled) router.replace("/admin");
      })
      .catch(() => {
        if (!cancelled) clearAdminAuthState();
      });
    return () => {
      cancelled = true;
    };
  }, [router]);

  // An expired session cleared elsewhere (e.g. 401 interceptor) must not keep the form blocked.
  useEffect(() => {
    const sync = () => setIsSubmitting(false);
    window.addEventListener(ADMIN_AUTH_STATE_EVENT, sync);
    return () => window.removeEventListener(ADMIN_AUTH_STATE_EVENT, sync);
  }, []);

  const emailValid = EMAIL_PATTERN.test(email.trim());
  const canSubmit = emailValid && password.length > 0 && !isSubmitting;

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!canSubmit || isSubmitting) return;

    setError("");
    setIsSubmitting(true);

    try {
      const { data } = await adminApi.post("/auth/admin/login", {
        email: email.trim(),
        password
      });

      if (data.role !== "ADMIN") {
        clearAdminAuthState();
        setError("This account does not have admin access.");
        setIsSubmitting(false);
        return;
      }

      persistAdminAuthState({ token: data.token, username: data.username, role: data.role });
      setAdminToken(data.token);
      router.replace("/admin");
    } catch (err) {
      const status = (err as { response?: { status?: number } })?.response?.status;
      setError(
        status === 401
          ? "Invalid admin credentials."
          : getApiErrorMessage(err, "Unable to sign in to the management console")
      );
      setIsSubmitting(false);
    }
  }

  return (
    <div className="flex min-h-[calc(100vh-7rem)] items-center justify-center px-4 py-10">
      <section className="w-full max-w-4xl overflow-hidden rounded-2xl border border-platform-border shadow-glow md:grid md:grid-cols-2">
        {/* Brand side — deliberately distinct from the player login */}
        <div className="relative hidden flex-col justify-between bg-platform-panel p-8 md:flex">
          <div
            aria-hidden="true"
            className="pointer-events-none absolute inset-0"
            style={{
              backgroundImage:
                "radial-gradient(circle at 15% 20%, rgba(34,197,94,0.12), transparent 40%), radial-gradient(circle at 90% 85%, rgba(34,197,94,0.08), transparent 35%)"
            }}
          />
          <div className="relative">
            <span className="flex h-11 w-11 items-center justify-center rounded-xl border border-platform-accent/40 bg-platform-accentSoft">
              <ShieldCheck size={22} className="text-platform-accent" />
            </span>
            <h1 className="mt-5 text-xl font-bold text-platform-text">AlgoArena</h1>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-platform-accent">
              Management Console
            </p>
          </div>
          <ul className="relative space-y-3 text-sm text-gray-400">
            <li className="flex items-start gap-2">
              <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-platform-accent" aria-hidden="true" />
              Restricted to accounts with the ADMIN role
            </li>
            <li className="flex items-start gap-2">
              <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-platform-accent" aria-hidden="true" />
              Every action is authorized on the backend
            </li>
            <li className="flex items-start gap-2">
              <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-platform-accent" aria-hidden="true" />
              Sessions stay separate from your player account
            </li>
          </ul>
          <p className="relative text-[0.65rem] uppercase tracking-widest text-gray-600">
            Authorized personnel only
          </p>
        </div>

        {/* Form side */}
        <div className="bg-black/60 p-8">
          <div className="mb-6 flex items-center gap-3 md:hidden">
            <span className="flex h-10 w-10 items-center justify-center rounded-xl border border-platform-accent/40 bg-platform-accentSoft">
              <ShieldCheck size={20} className="text-platform-accent" />
            </span>
            <div>
              <p className="text-sm font-bold text-platform-text">AlgoArena</p>
              <p className="text-[0.65rem] font-semibold uppercase tracking-[0.2em] text-platform-accent">
                Management Console
              </p>
            </div>
          </div>

          <h2 className="text-2xl font-bold text-platform-accent">Admin Login</h2>
          <p className="mt-1 text-sm text-gray-400">Sign in with your admin account.</p>

          <form onSubmit={onSubmit} className="mt-6 space-y-4" noValidate>
            <div>
              <label
                htmlFor="admin-email"
                className="mb-1.5 block text-xs font-medium uppercase tracking-wide text-gray-400"
              >
                Admin Email
              </label>
              <div className="relative">
                <Mail size={15} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
                <input
                  id="admin-email"
                  type="email"
                  autoComplete="username"
                  className="input-dark pl-9"
                  placeholder="admin@algoarena.dev"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  suppressHydrationWarning
                  required
                />
              </div>
              {email.length > 0 && !emailValid && (
                <p className="mt-1.5 text-xs text-amber-400">Enter a valid email address.</p>
              )}
            </div>

            <div>
              <label
                htmlFor="admin-password"
                className="mb-1.5 block text-xs font-medium uppercase tracking-wide text-gray-400"
              >
                Password
              </label>
              <div className="relative">
                <Lock size={15} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
                <input
                  id="admin-password"
                  type={showPassword ? "text" : "password"}
                  autoComplete="current-password"
                  className="input-dark pl-9 pr-10"
                  placeholder="Enter your password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  suppressHydrationWarning
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((v) => !v)}
                  className="absolute right-2 top-1/2 -translate-y-1/2 rounded p-1.5 text-gray-400 transition hover:text-platform-accent"
                  aria-label={showPassword ? "Hide password" : "Show password"}
                >
                  {showPassword ? <EyeOff size={15} /> : <Eye size={15} />}
                </button>
              </div>
            </div>

            {error && (
              <div className="rounded-lg border border-red-500/40 bg-red-500/10 px-3 py-2 text-sm text-red-400" role="alert">
                {error}
              </div>
            )}

            <button className="btn-accent flex w-full items-center justify-center gap-2 disabled:cursor-not-allowed disabled:opacity-60" type="submit" disabled={!canSubmit}>
              {isSubmitting ? (
                <>
                  <Loader2 size={16} className="animate-spin" />
                  Signing In...
                </>
              ) : (
                "Sign In to Console"
              )}
            </button>
          </form>

          <p className="mt-6 text-sm text-gray-400">
            Looking for the player site?{" "}
            <Link href="/login" className="text-platform-accent hover:underline">
              Player login
            </Link>
          </p>
        </div>
      </section>
    </div>
  );
}
