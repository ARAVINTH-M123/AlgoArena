import Link from "next/link";

type Problem = {
  id: number;
  title: string;
  difficulty: "EASY" | "MEDIUM" | "HARD";
  tags: string[];
};

function badgeColor(difficulty: string) {
  if (difficulty === "EASY") return "text-green-400 border-green-500/40 bg-green-900/20";
  if (difficulty === "MEDIUM") return "text-yellow-400 border-yellow-500/40 bg-yellow-900/20";
  return "text-red-400 border-red-500/40 bg-red-900/20";
}

export default function ProblemTable({ problems }: { problems: Problem[] }) {
  if (problems.length === 0) {
    return <div className="panel text-sm text-gray-400">No problems found for the current filters.</div>;
  }

  return (
    <div className="panel overflow-x-auto">
      <table className="w-full text-left text-sm">
        <thead className="border-b border-platform-border text-gray-400">
          <tr>
            <th className="py-2">Title</th>
            <th className="py-2">Difficulty</th>
            <th className="py-2">Tags</th>
          </tr>
        </thead>
        <tbody>
          {problems.map((problem) => (
            <tr key={problem.id} className="border-b border-platform-border/60">
              <td className="py-3">
                <Link href={`/problems/${problem.id}/solve`} className="font-medium text-platform-accent hover:underline">
                  {problem.title}
                </Link>
              </td>
              <td className="py-3">
                <span className={`inline-flex rounded-md border px-2 py-1 text-xs font-semibold ${badgeColor(problem.difficulty)}`}>
                  {problem.difficulty}
                </span>
              </td>
              <td className="py-3 text-gray-400">{problem.tags.length ? problem.tags.join(", ") : "-"}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
