import { paletteFor, tierLabel } from "@/lib/nation";

type Row = {
  username: string;
  score: number;
  timePenalty: number;
  // nation fields are optional so older callers keep working
  nationName?: string;
  nationRankName?: string;
  nationRankIndex?: number;
  nationTier?: number;
};

export default function LeaderboardTable({ rows }: { rows: Row[] }) {
  return (
    <div className="panel overflow-x-auto">
      <table className="w-full text-sm">
        <thead className="border-b border-platform-border text-gray-400">
          <tr>
            <th className="py-2 text-left">Rank</th>
            <th className="py-2 text-left">User</th>
            <th className="py-2 text-left">Nation</th>
            <th className="py-2 text-left">Score</th>
            <th className="py-2 text-left">Penalty</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row, index) => (
            <tr key={`${row.username}-${index}`} className="border-b border-platform-border/60">
              <td className="py-3">#{index + 1}</td>
              <td className="py-3 text-platform-accent">{row.username}</td>
              <td className="py-3">
                {row.nationRankName ? (
                  <span className="whitespace-nowrap">
                    <span className="text-gray-200">{row.nationName}</span>
                    <span className="text-gray-500"> · </span>
                    <span className="font-semibold" style={{ color: paletteFor(row.nationRankIndex ?? 0).accent }}>
                      {row.nationRankName} {tierLabel(row.nationTier ?? 1)}
                    </span>
                  </span>
                ) : (
                  <span className="text-gray-500">-</span>
                )}
              </td>
              <td className="py-3">{row.score}</td>
              <td className="py-3">{row.timePenalty}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
