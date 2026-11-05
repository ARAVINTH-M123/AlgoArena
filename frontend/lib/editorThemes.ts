// Monaco editor themes, one per rank (blueprint 9.1). Registered once; selected by the equipped theme key.

import type { Monaco } from "@monaco-editor/react";
import { THEME_LOOKS } from "@/lib/cosmetics";

/** "theme.event-horizon" -> "algo-event-horizon" */
export function monacoThemeName(key: string): string {
  return `algo-${key.replace(/^theme\./, "")}`;
}

function toRgb(hex: string): [number, number, number] {
  const value = hex.replace("#", "");
  return [parseInt(value.slice(0, 2), 16), parseInt(value.slice(2, 4), 16), parseInt(value.slice(4, 6), 16)];
}

function toHex(rgb: [number, number, number]): string {
  return rgb.map((part) => Math.round(Math.min(255, Math.max(0, part))).toString(16).padStart(2, "0")).join("");
}

/** Blend two #rrggbb colors; amount 0 = a, 1 = b. Returns "rrggbb" (no #). */
export function mix(a: string, b: string, amount: number): string {
  const [ar, ag, ab] = toRgb(a);
  const [br, bg, bb] = toRgb(b);
  return toHex([ar + (br - ar) * amount, ag + (bg - ag) * amount, ab + (bb - ab) * amount]);
}

function luminance(hex: string): number {
  const [r, g, b] = toRgb(hex).map((c) => {
    const v = c / 255;
    return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
  });
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

/** Dim accents (like Void's muted gray) are lightened until text in that color is readable on the dark background. */
export function readable(accent: string): string {
  let color = accent.replace("#", "");
  for (let i = 0; i < 12 && luminance(`#${color}`) < 0.3; i++) {
    color = mix(`#${color}`, "#ffffff", 0.14);
  }
  return color;
}

export function registerEditorThemes(monaco: Pick<Monaco, "editor">): void {
  for (const [key, look] of Object.entries(THEME_LOOKS)) {
    const accent = readable(look.accent);
    const background = look.background;
    monaco.editor.defineTheme(monacoThemeName(key), {
      base: "vs-dark",
      inherit: true,
      rules: [
        { token: "comment", foreground: mix(`#${accent}`, "#6b7280", 0.7), fontStyle: "italic" },
        { token: "keyword", foreground: accent },
        { token: "string", foreground: mix(`#${accent}`, "#ffffff", 0.35) },
        { token: "number", foreground: mix(`#${accent}`, "#ffffff", 0.55) },
        { token: "type", foreground: mix(`#${accent}`, "#ffffff", 0.2) },
        { token: "delimiter", foreground: "9aa4b2" }
      ],
      colors: {
        "editor.background": background,
        "editor.foreground": "#d6deeb",
        "editorGutter.background": background,
        "editorLineNumber.foreground": "#5b6574",
        "editorLineNumber.activeForeground": `#${accent}`,
        "editorCursor.foreground": `#${accent}`,
        "editor.lineHighlightBackground": `#${mix(background, look.accent, 0.12)}`,
        "editor.selectionBackground": `${look.accent}55`,
        "editor.inactiveSelectionBackground": `${look.accent}2a`,
        "editorIndentGuide.background1": `${look.accent}2a`,
        "editorBracketMatch.border": `#${accent}`,
        "editorWidget.background": `#${mix(background, "#ffffff", 0.06)}`,
        "scrollbarSlider.background": `${look.accent}40`
      }
    });
  }
}
