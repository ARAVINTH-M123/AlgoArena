import { STAGE_LABELS, TRACK_LABELS, formatPoints, paletteFor, type NationDto } from "@/lib/nation";

type Props = {
  nation: NationDto;
};

/** Three bars, one per track: what is being built right now and how far along it is. */
export default function TrackProgress({ nation }: Props) {
  const accent = paletteFor(nation.rankIndex).accent;

  return (
    <div>
      <h3 className="text-sm font-semibold uppercase tracking-wide text-gray-400">Under construction</h3>
      <ul className="mt-2 space-y-3">
        {nation.tracks.map((track) => {
          const percent = track.cost > 0 ? Math.min(100, Math.floor((track.progress / track.cost) * 100)) : 0;
          return (
            <li key={track.track}>
              <div className="flex items-baseline justify-between text-sm">
                <span className="text-gray-300">
                  {TRACK_LABELS[track.track]}: <span className="font-semibold text-platform-text">{track.itemName}</span>
                </span>
                <span className="text-xs text-gray-400">{STAGE_LABELS[track.stage]}</span>
              </div>
              <div
                className="mt-1 h-2 w-full overflow-hidden rounded-full border border-platform-border bg-black"
                role="progressbar"
                aria-valuemin={0}
                aria-valuemax={100}
                aria-valuenow={percent}
                aria-label={`${TRACK_LABELS[track.track]} construction progress`}
              >
                <div className="h-full rounded-full transition-all duration-500" style={{ width: `${percent}%`, backgroundColor: accent }} />
              </div>
              <p className="mt-0.5 text-xs text-gray-500">
                {formatPoints(track.progress)} / {formatPoints(track.cost)} points
              </p>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
