import { FlagGraphic } from "@/components/nation/Flag";
import { normalizeFlag } from "@/lib/flags";
import type { FlagDto } from "@/lib/cosmetics";
import { paletteFor, rankTitle } from "@/lib/nation";

type Props = {
  name: string;
  rankName: string;
  rankIndex: number;
  tier: number;
  /** The flag in effect (Phase 5B). */
  flag?: FlagDto | null;
};

/** "🏳 Stonebridge · Kernel III" — the small flag is the nation's own. */
export default function RankBadge({ name, rankName, rankIndex, tier, flag }: Props) {
  const accent = paletteFor(rankIndex).accent;
  const graphic = normalizeFlag(flag ?? null, rankIndex);
  return (
    <span
      className="inline-flex flex-wrap items-center gap-x-2 rounded-lg border px-3 py-1 text-sm font-semibold"
      style={{ borderColor: accent + "99", color: accent }}
    >
      {flag && <FlagGraphic flag={graphic} className="h-[18px] w-[27px] rounded-[2px]" />}
      <span className="text-platform-text">{name}</span>
      <span aria-hidden="true">·</span>
      <span>{rankTitle(rankName, tier)}</span>
    </span>
  );
}
