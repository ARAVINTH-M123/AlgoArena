"use client";

import { useState } from "react";
import { useReducedMotion } from "@/hooks/useReducedMotion";
import { auraLook } from "@/lib/cosmetics";
import type { PresenceView } from "@/lib/presence";

/** Where the figure stands in the scene (the valley of the mountain ridge). */
export const FIGURE_X = 420;

// vertical offset of the figure per state; the mountain layer in front hides everything below the ridge
const OFFSET_ONLINE = -35;
const OFFSET_JUST_SOLVED = -50;
const OFFSET_IDLE = 55;
const OFFSET_OFFLINE = 160;

type Look = {
  fill: string;
  fillOpacity: number;
  stroke: string;
  strokeWidth: number;
  glow: boolean;
  crown: boolean;
  halo: boolean;
  rays: boolean;
};

/** Styling by rank; the pose is identical for every rank so the art is reusable. */
export function figureLook(rankIndex: number, accent: string): Look {
  const base = { fill: "#000000", fillOpacity: 0.88, stroke: "none", strokeWidth: 0, glow: false, crown: false, halo: false, rays: false };
  if (rankIndex <= 1) return { ...base, fillOpacity: 0.2, stroke: accent, strokeWidth: 1.2 };             // faint outline
  if (rankIndex <= 3) return base;                                                                            // solid shadow
  if (rankIndex <= 5) return { ...base, stroke: accent, strokeWidth: 1.6, glow: true };                      // glowing edge
  if (rankIndex <= 7) return { ...base, stroke: accent, strokeWidth: 1.6, glow: true, crown: true };         // crown-like glow
  if (rankIndex === 8) return { ...base, stroke: accent, strokeWidth: 2, glow: true, crown: true, rays: true }; // radiant
  return { ...base, stroke: accent, strokeWidth: 2, glow: true, halo: true };                                // halo
}

type FigureProps = {
  presence: PresenceView;
  rankIndex: number;
  /** Aura color; later cosmetics can override the rank accent. */
  accent: string;
  /** Equipped aura cosmetic (Phase 6); null shows no ring. */
  auraKey?: string | null;
};

/** Drawn BEFORE the mountain layers so the ridge hides the lower body. */
export default function PresenceFigure({ presence, rankIndex, accent, auraKey }: FigureProps) {
  const reduced = useReducedMotion();
  const look = figureLook(rankIndex, accent);
  const aura = auraLook(auraKey);

  let offset = OFFSET_OFFLINE;
  if (presence.status === "ONLINE") offset = presence.justSolved ? OFFSET_JUST_SOLVED : OFFSET_ONLINE;
  else if (presence.status === "IDLE") offset = OFFSET_IDLE;

  const bright = presence.justSolved && presence.status === "ONLINE";
  // reduced motion: no sliding, a short crossfade instead (the key remounts the group when the state changes)
  const className = reduced ? "nation-figure-fade" : "nation-figure-slide";
  const key = reduced ? `${presence.status}-${presence.justSolved}` : "figure";

  return (
    <g transform={`translate(${FIGURE_X} 0)`} aria-hidden="true">
      <g key={key} className={className} style={{ transform: `translateY(${offset}px)` }} opacity={bright ? 1 : 0.9}
        filter={look.glow ? "url(#nation-figure-glow)" : undefined}>
        {look.rays && (
          <g stroke={accent} strokeWidth={1.4} opacity={0.5}>
            {[-70, -45, -22, 0, 22, 45, 70].map((angle) => (
              <line key={angle} x1={0} y1={118} x2={Math.sin((angle * Math.PI) / 180) * 70} y2={118 - Math.cos((angle * Math.PI) / 180) * 70} />
            ))}
          </g>
        )}
        {look.halo && <ellipse cx={0} cy={90} rx={26} ry={7} fill="none" stroke={accent} strokeWidth={2.4} />}
        {look.crown && <path d="M -14 101 L -11 84 L -4 94 L 0 80 L 4 94 L 11 84 L 14 101 Z" fill={accent} opacity={0.9} />}
        {aura && (
          <g>
            <circle cx={0} cy={118} r={26 + aura.width * 2} fill="none" stroke={aura.color}
              strokeWidth={aura.width} opacity={0.85}
              className={aura.animated && !reduced ? "nation-aura-pulse" : undefined} />
            {aura.width >= 4 && (
              <circle cx={0} cy={118} r={30 + aura.width * 2.4} fill="none" stroke={aura.color}
                strokeWidth={1} opacity={0.4} />
            )}
          </g>
        )}
        <circle cx={0} cy={118} r={19} fill={look.fill} fillOpacity={look.fillOpacity} stroke={look.stroke} strokeWidth={look.strokeWidth} />
        <path
          d="M -62 300 L -62 168 Q -62 148 -36 143 L -12 136 L -12 130 L 12 130 L 12 136 L 36 143 Q 62 148 62 168 L 62 300 Z"
          fill={look.fill}
          fillOpacity={look.fillOpacity}
          stroke={look.stroke}
          strokeWidth={look.strokeWidth}
        />
        {bright && <circle cx={0} cy={118} r={34} fill={accent} opacity={0.18} />}
      </g>
    </g>
  );
}

type CoverProps = {
  presence: PresenceView;
  /** Text revealed by tapping the clouds, e.g. "Last active 2 hours ago". */
  lastActiveText: string;
};

/** Drawn AFTER the mountains: when the owner is offline, clouds cover the spot; tapping them shows "Last active". */
export function PresenceCloudCover({ presence, lastActiveText }: CoverProps) {
  const [open, setOpen] = useState(false);
  if (presence.status !== "OFFLINE") return null;

  const toggle = () => setOpen((value) => !value);
  return (
    <g
      transform={`translate(${FIGURE_X} 188)`}
      role="button"
      tabIndex={0}
      aria-label={`Show when the owner was last active. ${lastActiveText}`}
      style={{ cursor: "pointer" }}
      onClick={toggle}
      onKeyDown={(event) => {
        if (event.key === "Enter" || event.key === " ") {
          event.preventDefault();
          toggle();
        }
      }}
    >
      <g className="nation-cloud nation-cloud-slow" fill="#ffffff" opacity={0.2}>
        <ellipse cx={-30} cy={6} rx={46} ry={11} />
        <ellipse cx={22} cy={-2} rx={40} ry={12} />
        <ellipse cx={4} cy={12} rx={54} ry={9} />
      </g>
      {open && (
        <g>
          <rect x={-86} y={-40} width={172} height={24} rx={6} fill="#000000" opacity={0.75} />
          <text x={0} y={-24} fontSize={12} textAnchor="middle" fill="#e5e7eb">{lastActiveText}</text>
        </g>
      )}
    </g>
  );
}
