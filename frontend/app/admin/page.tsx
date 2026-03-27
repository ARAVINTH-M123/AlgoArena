"use client";

import { FormEvent, useState } from "react";
import { api, getApiErrorMessage, setToken } from "@/lib/api";
import { useRouter } from "next/navigation";

export default function AdminLoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");

    try {
      const { data } = await api.post("/auth/admin/login", { email, password });
      localStorage.setItem("token", data.token);
      localStorage.setItem("username", data.username);
      localStorage.setItem("role", data.role);
      setToken(data.token);
      router.push("/admin/dashboard");
    } catch (err) {
      setError(getApiErrorMessage(err, "Unable to sign in as admin"));
    }
  }

  return (
    <div className="flex min-h-[calc(100vh-7rem)] items-center justify-center">
      <section className="w-full max-w-md panel">
        <h1 className="text-2xl font-bold text-platform-accent">Admin Login</h1>
        <form onSubmit={onSubmit} className="mt-4 space-y-3">
          <input
            className="input-dark"
            type="email"
            placeholder="Admin Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          <input
            className="input-dark"
            type="password"
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          {error && <p className="text-sm text-red-400">{error}</p>}
          <button className="btn-accent w-full" type="submit">Login</button>
        </form>
      </section>
    </div>
  );
}
