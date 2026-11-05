import CityGrid from "@/components/nation/CityGrid";
import NationFrame from "@/components/nation/NationFrame";
import PresenceFigure, { FIGURE_X, PresenceCloudCover } from "@/components/nation/PresenceFigure";
import { SCENE_HEIGHT, SCENE_WIDTH, paletteFor, rankTitle, type NationDto } from "@/lib/nation";
import type { PresenceView } from "@/lib/presence";

const SCENE_CSS = `
@keyframes nation-drift { from { transform: translateX(-36px); } to { transform: translateX(36px); } }
@keyframes nation-blink { 50% { opacity: 0; } }
@keyframes nation-flicker { 0%, 100% { opacity: 0.9; } 50% { opacity: 0.55; } }
.nation-cloud { animation: nation-drift 38s ease-in-out infinite alternate; }
.nation-cloud-slow { animation-duration: 56s; animation-direction: alternate-reverse; }
.nation-cursor { animation: nation-blink 1s steps(1) infinite; }
.nation-fire { animation: nation-flicker 1.6s ease-in-out infinite; }
@keyframes nation-fadein { from { opacity: 0; } to { opacity: 1; } }
.nation-figure-slide { transition: transform 1.4s ease-in-out; }
.nation-figure-fade { animation: nation-fadein 0.8s ease-out; }
@media (prefers-reduced-motion: reduce) {
  .nation-cloud, .nation-cursor, .nation-fire { animation: none; }
  .nation-figure-slide { transition: none; }
}
`;

// fixed pseudo-random star field (deterministic, so server and client render the same thing)
const STARS = Array.from({ length: 46 }, (_, i) => {
  const a = (i * 7919 + 13) % 1000;
  const b = (i * 104729 + 71) % 1000;
  return { x: 10 + (a / 1000) * 780, y: 8 + (b / 1000) * 120, r: 0.6 + ((a + b) % 7) / 6 };
});

type Props = {
  nation: NationDto;
  /** What the presence figure should show right now. Omit to draw no figure. */
  presence?: PresenceView;
  /** Text for the offline clouds, e.g. "Last active 2 hours ago". */
  lastActiveText?: string;
};

/** Layered scene, back to front: sky and clouds, presence figure, mountain ridge, city. */
export default function NationScene({ nation, presence, lastActiveText = "Offline" }: Props) {
  const palette = paletteFor(nation.rankIndex);
  const built = nation.completedItems.length;
  const summary = `${nation.name}, ${rankTitle(nation.rankName, nation.tier)}. ${built} ${built === 1 ? "item" : "items"} built.`;

  return (
    <NationFrame borderKey={nation.borderKey}>
      <svg viewBox={`0 0 ${SCENE_WIDTH} ${SCENE_HEIGHT}`} role="img" aria-label={summary} className="block h-auto w-full"
        preserveAspectRatio="xMidYMid slice">
        <title>{summary}</title>
        <style>{SCENE_CSS}</style>
        <defs>
          <linearGradient id="nation-sky" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" stopColor={palette.skyTop} />
            <stop offset="1" stopColor={palette.skyBottom} />
          </linearGradient>
          <linearGradient id="nation-ground" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" stopColor={palette.mountainFront} />
            <stop offset="0.25" stopColor={palette.ground} />
            <stop offset="1" stopColor="#000000" />
          </linearGradient>
          <linearGradient id="nation-beam" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" stopColor={palette.accent} stopOpacity="0.5" />
            <stop offset="1" stopColor={palette.accent} stopOpacity="0" />
          </linearGradient>
          <filter id="nation-figure-glow" x="-60%" y="-60%" width="220%" height="220%">
            <feGaussianBlur in="SourceGraphic" stdDeviation="3.5" result="blur" />
            <feMerge>
              <feMergeNode in="blur" />
              <feMergeNode in="SourceGraphic" />
            </feMerge>
          </filter>
          <radialGradient id="nation-glow" cx="0.5" cy="1" r="0.8">
            <stop offset="0" stopColor={palette.accent} stopOpacity="0.28" />
            <stop offset="1" stopColor={palette.accent} stopOpacity="0" />
          </radialGradient>
        </defs>

        {/* sky */}
        <rect width={SCENE_WIDTH} height={SCENE_HEIGHT} fill="url(#nation-sky)" />
        {STARS.slice(0, palette.stars).map((star, i) => (
          <circle key={i} cx={star.x} cy={star.y} r={star.r} fill="#ffffff" opacity={0.35 + (i % 4) * 0.12} />
        ))}
        {palette.ambient === "glow" && <rect y={120} width={SCENE_WIDTH} height={150} fill="url(#nation-glow)" />}

        {/* clouds drift (and stop for reduced motion) */}
        <g className="nation-cloud" fill="#ffffff" opacity={0.1}>
          <ellipse cx={170} cy={78} rx={70} ry={13} />
          <ellipse cx={215} cy={68} rx={42} ry={11} />
          <ellipse cx={560} cy={52} rx={90} ry={12} />
        </g>
        <g className="nation-cloud nation-cloud-slow" fill={palette.accent} opacity={0.12}>
          <ellipse cx={380} cy={110} rx={80} ry={10} />
          <ellipse cx={690} cy={96} rx={58} ry={10} />
        </g>

        {/* the presence figure stands behind the ridge; thin cloud wisps drift in front of it */}
        {presence && <PresenceFigure presence={presence} rankIndex={nation.rankIndex} accent={palette.accent} />}
        <g className="nation-cloud nation-cloud-slow" fill="#ffffff" opacity={0.08}>
          <ellipse cx={FIGURE_X - 40} cy={150} rx={60} ry={5} />
          <ellipse cx={FIGURE_X + 50} cy={176} rx={46} ry={4} />
        </g>

        {/* mountain ridge: constant across ranks apart from its tint */}
        <path fill={palette.mountainBack}
          d="M0 275 L0 215 L70 170 L120 205 L200 120 L270 190 L330 150 L420 215 L520 135 L600 200 L680 160 L740 195 L800 150 L800 275 Z" />
        <path fill={palette.mountainFront}
          d="M0 275 L0 238 L90 202 L160 232 L250 178 L340 236 L450 192 L560 240 L650 197 L740 236 L800 208 L800 275 Z" />

        {/* ground / territory */}
        <rect y={262} width={SCENE_WIDTH} height={SCENE_HEIGHT - 262} fill="url(#nation-ground)" />
        <line x1={0} x2={SCENE_WIDTH} y1={262.5} y2={262.5} stroke={palette.accent} strokeOpacity={0.35} />

        {/* light beam from the figure onto the city right after a solve */}
        {presence?.justSolved && presence.status === "ONLINE" && (
          <path d={`M ${FIGURE_X - 14} 214 L ${FIGURE_X + 14} 214 L ${FIGURE_X + 190} 352 L ${FIGURE_X - 190} 352 Z`} fill="url(#nation-beam)" />
        )}
        {presence && <PresenceCloudCover presence={presence} lastActiveText={lastActiveText} />}

        {/* rank-specific ambient details */}
        {palette.ambient === "cursor" && (
          <text className="nation-cursor" x={150} y={330} fontSize={34} textAnchor="middle" fill={palette.accent}
            fontFamily="ui-monospace, Menlo, Consolas, monospace">_</text>
        )}
        {palette.ambient === "fire" && (
          <g className="nation-fire">
            <ellipse cx={400} cy={300} rx={46} ry={14} fill={palette.accent} opacity={0.18} />
          </g>
        )}

        <CityGrid nation={nation} accent={palette.accent} />
      </svg>
    </NationFrame>
  );
}
