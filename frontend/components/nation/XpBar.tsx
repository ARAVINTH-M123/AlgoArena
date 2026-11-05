import { formatPoints, paletteFor, type NationDto } from "@/lib/nation";

type Props = {
  nation: NationDto;
};

export default function XpBar({ nation }: Props) {
  const accent = paletteFor(nation.rankIndex).accent;
  const next = nation.nextRank;
  const percent = next ? next.percentToNext : 100;

  return (
    <div>
      <div className="mb-1 flex items-baseline justify-between text-sm">
        <span className="text-gray-300">Nation XP</span>
        <span className="font-semibold text-platform-text">
          {next ? `${formatPoints(nation.xp)} / ${formatPoints(next.xpRequired)}` : `${formatPoints(nation.xp)} (max rank)`}
        </span>
      </div>
      <div
        className="h-3 w-full overflow-hidden rounded-full border border-platform-border bg-black"
        role="progressbar"
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={percent}
        aria-label="Progress to the next rank"
      >
        <div className="h-full rounded-full transition-all duration-500" style={{ width: `${percent}%`, backgroundColor: accent }} />
      </div>
      <p className="mt-1 text-xs text-gray-400">
        {next ? `${percent}% of the way to ${next.name}` : nation.ascended ? "Ascended. There is no higher rank." : "Final rank reached."}
      </p>
    </div>
  );
}
