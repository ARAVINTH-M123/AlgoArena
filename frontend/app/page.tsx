import Link from "next/link";

export default function LandingPage() {
  return (
    <section className="flex min-h-[75vh] flex-col items-center justify-center text-center">
      <h1 className="text-4xl font-extrabold text-platform-accent md:text-6xl">AlgoArena</h1>
      <p className="mt-4 max-w-2xl text-gray-300">Practice coding interviews, join contests, track streaks, and climb the leaderboard.</p>
      <div className="mt-8 flex flex-wrap gap-3">
        <Link href="/register" className="btn-accent">Get Started</Link>
        <Link href="/problems" className="rounded-lg border border-platform-border px-4 py-2 text-gray-200 hover:border-platform-accent">Browse Problems</Link>
      </div>
    </section>
  );
}
