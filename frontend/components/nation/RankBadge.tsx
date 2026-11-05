import { paletteFor, rankTitle } from "@/lib/nation";

type Props = {
  name: string;
  rankName: string;
  rankIndex: number;
  tier: number;
};

/** "Stonebridge · Kernel III" */
export default function RankBadge({ name, rankName, rankIndex, tier }: Props) {
  const accent = paletteFor(rankIndex).accent;
  return (
    <span
      className="inline-flex flex-wrap items-center gap-x-2 rounded-lg border px-3 py-1 text-sm font-semibold"
      style={{ borderColor: accent + "99", color: accent }}
    >
      <span className="text-platform-text">{name}</span>
      <span aria-hidden="true">·</span>
      <span>{rankTitle(rankName, tier)}</span>
    </span>
  );
}
