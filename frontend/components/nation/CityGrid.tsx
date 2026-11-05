import {
  MAX_LAYERS,
  SCENE_WIDTH,
  SLOT_CAPACITY,
  itemGlyph,
  layoutSlot,
  type CompletedItemDto,
  type NationDto,
  type TrackName
} from "@/lib/nation";

const GLYPH_SIZE: Record<TrackName, number> = { LANDMARKS: 38, STRUCTURES: 24, BASICS: 15 };
const DRAW_ORDER: TrackName[] = ["LANDMARKS", "STRUCTURES", "BASICS"]; // back to front

type Props = {
  nation: NationDto;
  accent: string;
};

/**
 * Draws the completed items of a nation (SVG group, scene coordinates). Positions come from layoutSlot, so the
 * layout is deterministic. The item currently under construction is drawn as a faded "ghost" whose opacity and
 * scaffold grow with its construction stage, so every solve visibly changes something.
 */
export default function CityGrid({ nation, accent }: Props) {
  const byTrack: Record<TrackName, CompletedItemDto[]> = { BASICS: [], STRUCTURES: [], LANDMARKS: [] };
  for (const item of nation.completedItems) {
    byTrack[item.track].push(item);
  }

  let hidden = 0;
  const layers = DRAW_ORDER.map((track) => {
    const items = byTrack[track].slice().sort((a, b) => a.sequenceNumber - b.sequenceNumber);
    const visibleCount = SLOT_CAPACITY[track] * MAX_LAYERS;
    hidden += Math.max(0, items.length - visibleCount);
    const visible = items.slice(-visibleCount);
    const size = GLYPH_SIZE[track];

    const ghost = nation.tracks.find((t) => t.track === track);
    const ghostSlot = ghost ? layoutSlot(track, items.length + 1) : null;

    return (
      <g key={track} data-track={track}>
        {track === "LANDMARKS" &&
          visible.map((item) => {
            const slot = layoutSlot(track, item.sequenceNumber);
            return (
              <ellipse key={`glow-${item.sequenceNumber}`} cx={slot.x} cy={slot.y + size * 0.55} rx={size * 0.7} ry={size * 0.16}
                fill={accent} opacity={0.18} />
            );
          })}
        {visible.map((item) => {
          const slot = layoutSlot(track, item.sequenceNumber);
          return (
            <text key={`${track}-${item.sequenceNumber}`} x={slot.x} y={slot.y + size * 0.35} fontSize={size} textAnchor="middle">
              <title>{item.name}</title>
              {itemGlyph(item.key, track)}
            </text>
          );
        })}
        {ghost && ghostSlot && (
          <g opacity={0.9}>
            <rect x={ghostSlot.x - size * 0.6} y={ghostSlot.y - size * 0.6} width={size * 1.2} height={size * 1.2}
              fill="none" stroke={accent} strokeWidth={1} strokeDasharray="3 3" opacity={0.55} rx={3} />
            <text x={ghostSlot.x} y={ghostSlot.y + size * 0.35} fontSize={size} textAnchor="middle" opacity={0.15 + 0.2 * ghost.stage}>
              <title>{`${ghost.itemName} (under construction)`}</title>
              {itemGlyph(ghost.itemKey, track)}
            </text>
            <rect x={ghostSlot.x - size * 0.6} y={ghostSlot.y + size * 0.68} width={size * 1.2 * ((ghost.stage + 1) / 4)} height={2}
              fill={accent} opacity={0.9} />
          </g>
        )}
      </g>
    );
  });

  return (
    <g>
      {layers}
      {hidden > 0 && (
        <text x={SCENE_WIDTH - 12} y={256} fontSize={10} textAnchor="end" fill="#9ca3af">{`+${hidden} older buildings`}</text>
      )}
    </g>
  );
}
