"use client";

import { FormEvent, useState } from "react";
import { api } from "@/lib/api";

export default function CreateContestPage() {
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    const token = localStorage.getItem("token");
    if (token) api.defaults.headers.common.Authorization = `Bearer ${token}`;
    const now = new Date();
    const end = new Date(now.getTime() + 2 * 60 * 60 * 1000);
    await api.post("/contests", { name, description, startTime: now.toISOString(), endTime: end.toISOString() });
    alert("Contest created");
  }

  return (
    <section className="panel">
      <h1 className="text-2xl font-bold text-platform-accent">Create Contest</h1>
      <form onSubmit={onSubmit} className="mt-4 space-y-3">
        <input className="input-dark" placeholder="Contest Name" value={name} onChange={(e) => setName(e.target.value)} />
        <textarea className="input-dark" placeholder="Description" value={description} onChange={(e) => setDescription(e.target.value)} />
        <button className="btn-accent">Create</button>
      </form>
    </section>
  );
}
