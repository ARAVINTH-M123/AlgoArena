import type { ReactNode } from "react";
import { borderStyleFor } from "@/lib/cosmetics";

const FRAME_CSS = `
@keyframes nation-frame-shimmer { from { background-position: 0% 50%; } to { background-position: 300% 50%; } }
.nation-frame-animated { background-size: 300% 100%; animation: nation-frame-shimmer 7s linear infinite; }
@media (prefers-reduced-motion: reduce) { .nation-frame-animated { animation: none; } }
`;

type Props = {
  borderKey?: string | null;
  children: ReactNode;
};

/** The equipped frame around the nation scene (and, small, in the picker). */
export default function NationFrame({ borderKey, children }: Props) {
  const style = borderStyleFor(borderKey);
  return (
    <div
      className={style.animated ? "nation-frame-animated" : undefined}
      data-border={borderKey ?? undefined}
      style={{ padding: style.padding, background: style.background, borderRadius: style.radius, boxShadow: style.glow }}
    >
      <style>{FRAME_CSS}</style>
      <div style={{ borderRadius: Math.max(0, style.radius - style.padding), overflow: "hidden" }}>{children}</div>
    </div>
  );
}
