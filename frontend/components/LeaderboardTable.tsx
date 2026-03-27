type Row = {
  username: string;
  score: number;
  timePenalty: number;
};

export default function LeaderboardTable({ rows }: { rows: Row[] }) {
  return (
    <div className="panel overflow-x-auto">
      <table className="w-full text-sm">
        <thead className="border-b border-platform-border text-gray-400">
          <tr>
            <th className="py-2 text-left">Rank</th>
            <th className="py-2 text-left">User</th>
            <th className="py-2 text-left">Score</th>
            <th className="py-2 text-left">Penalty</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row, index) => (
            <tr key={`${row.username}-${index}`} className="border-b border-platform-border/60">
              <td className="py-3">#{index + 1}</td>
              <td className="py-3 text-platform-accent">{row.username}</td>
              <td className="py-3">{row.score}</td>
              <td className="py-3">{row.timePenalty}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
