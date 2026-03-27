import Link from "next/link";
import BackArrowButton from "@/components/BackArrowButton";

export default async function ContestProblemPage({ params }: { params: Promise<{ id: string; problemId: string }> }) {
  const { id, problemId } = await params;
  return (
    <section className="panel">
      <BackArrowButton fallbackHref={`/contests/${id}`} className="mb-4" />
      <h1 className="text-2xl font-bold text-platform-accent">Contest {id} • Problem {problemId}</h1>
      <p className="mt-2 text-gray-300">Use the dedicated solve page to submit your answer during the contest.</p>
      <Link href={`/problems/${problemId}/solve`} className="btn-accent mt-4 inline-block">Open Editor</Link>
    </section>
  );
}
