// Nation flags (Phase 5B): the SVG drawing of a flag (90x60) and the small emblem glyph.
// Pure rendering: every value comes from lib/flags.ts / the API.

import type { CSSProperties } from "react";
import { emblemGlyph, flagColorHex } from "@/lib/flags";
import type { FlagDto } from "@/lib/cosmetics";

const W = 90;
const H = 60;

type GraphicProps = {
  flag: FlagDto;
  className?: string;
  style?: CSSProperties;
  /** Draw a thin outline (default true). */
  outline?: boolean;
};

/**
 * The flag shapes only (no <svg> wrapper): reused by FlagGraphic and inside the nation scene,
 * where the scene controls position and scale.
 */
export function FlagContent({ flag }: { flag: FlagDto }) {
  const [first, second, third] = [0, 1, 2].map((i) => flagColorHex(flag.colors[i] ?? flag.colors[0] ?? "color.slate"));
  const c0 = first;
  const c1 = second;
  const c2 = third ?? first;
  const three = flag.colors.length >= 3;
  const glyph = flag.emblem ? emblemGlyph(flag.emblem) : null;

  return (
    <>
      {flag.pattern === "pattern.vertical" && (
        <>
          <rect width={three ? W / 3 : W / 2} height={H} fill={c0} />
          <rect x={three ? W / 3 : W / 2} width={three ? W / 3 : W / 2} height={H} fill={c1} />
          {three && <rect x={(2 * W) / 3} width={W / 3} height={H} fill={c2} />}
        </>
      )}
      {flag.pattern === "pattern.horizontal" && (
        <>
          <rect width={W} height={three ? H / 3 : H / 2} fill={c0} />
          <rect y={three ? H / 3 : H / 2} width={W} height={three ? H / 3 : H / 2} fill={c1} />
          {three && <rect y={(2 * H) / 3} width={W} height={H / 3} fill={c2} />}
        </>
      )}
      {flag.pattern === "pattern.diagonal" && (
        <>
          <rect width={W} height={H} fill={c0} />
          <path d={`M0 ${H} L${W} 0 L${W} ${H} Z`} fill={c1} />
          {three && <path d={`M0 ${H} L${W} 0 L${W} 8 L8 ${H} Z`} fill={c2} />}
        </>
      )}
      {flag.pattern === "pattern.cross" && (
        <>
          <rect width={W} height={H} fill={c0} />
          {three && <rect x={26} width={14} height={H} fill={c2} />}
          {three && <rect y={23} width={W} height={14} fill={c2} />}
          <rect x={29} width={8} height={H} fill={c1} />
          <rect y={26} width={W} height={8} fill={c1} />
        </>
      )}
      {flag.pattern === "pattern.saltire" && (
        <>
          <rect width={W} height={H} fill={c0} />
          {three && (
            <>
              <path d={`M0 0 L10 0 L${W} ${H} L${W - 10} ${H} Z`} fill={c2} />
              <path d={`M${W} 0 L${W} 10 L0 ${H} L0 ${H - 10} Z`} fill={c2} />
            </>
          )}
          <path d={`M0 0 L6 0 L${W} ${H} L${W - 6} ${H} Z`} fill={c1} />
          <path d={`M${W} 0 L${W} 6 L0 ${H} L0 ${H - 6} Z`} fill={c1} />
        </>
      )}
      {flag.pattern === "pattern.triangle" && (
        <>
          <rect width={W} height={three ? H / 2 : H} fill={c0} />
          <rect y={three ? H / 2 : 0} width={W} height={three ? H / 2 : H} fill={c1} />
          <path d={`M0 0 L34 ${H / 2} L0 ${H} Z`} fill={three ? c2 : c1} />
        </>
      )}
      {flag.pattern === "pattern.canton" && (
        <>
          <rect width={W} height={H} fill={c0} />
          {three && <rect y={H * 0.62} width={W} height={H * 0.38} fill={c2} />}
          <rect width={36} height={26} fill={c1} />
        </>
      )}
      {glyph && (
        <g transform={`translate(${flag.pattern === "pattern.canton" ? 52 : 33} 18)`}>
          {glyph.map((d, i) => (
            <path key={i} d={d} fill={readableInk(c1)} fillOpacity={0.92} />
          ))}
        </g>
      )}
    </>
  );
}

/** The flag itself, 90x60 SVG. */
export function FlagGraphic({ flag, className, style, outline = true }: GraphicProps) {
  return (
    <svg viewBox={`0 0 ${W} ${H}`} width={W} height={H} className={className} style={style} aria-hidden="true">
      <FlagContent flag={flag} />
      {outline && <rect width={W} height={H} fill="none" stroke="#000000" strokeOpacity={0.35} strokeWidth={1} />}
    </svg>
  );
}

/** Emblem ink: dark on light flags, light on dark flags. */
function readableInk(field: string): string {
  const hex = field.replace("#", "");
  const r = parseInt(hex.slice(0, 2), 16);
  const g = parseInt(hex.slice(2, 4), 16);
  const b = parseInt(hex.slice(4, 6), 16);
  const luma = 0.299 * r + 0.587 * g + 0.114 * b;
  return luma > 140 ? "#111827" : "#f3f4f6";
}

/** The emblem alone, e.g. for the rank badge. */
export function EmblemGlyph({ emblemKey, size = 16, color }: { emblemKey: string; size?: number; color?: string }) {
  const glyph = emblemGlyph(emblemKey);
  if (!glyph) {
    return null;
  }
  return (
    <svg viewBox="0 0 24 24" width={size} height={size} aria-hidden="true">
      {glyph.map((d, i) => (
        <path key={i} d={d} fill={color ?? "currentColor"} />
      ))}
    </svg>
  );
}
