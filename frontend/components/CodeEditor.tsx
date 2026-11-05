"use client";

import dynamic from "next/dynamic";
import { useEquippedThemeKey } from "@/hooks/useEquippedTheme";
import { monacoThemeName, registerEditorThemes } from "@/lib/editorThemes";

const MonacoEditor = dynamic(() => import("@monaco-editor/react"), { ssr: false });

const GLOW_CSS = `
@keyframes editor-gold-glow {
  0%, 100% { box-shadow: inset 0 0 36px rgba(255, 212, 90, 0.05); }
  50% { box-shadow: inset 0 0 60px rgba(255, 212, 90, 0.16); }
}
.editor-event-horizon { animation: editor-gold-glow 6s ease-in-out infinite; }
@media (prefers-reduced-motion: reduce) { .editor-event-horizon { animation: none; } }
`;

export default function CodeEditor({ code, onChange }: { code: string; onChange: (value: string) => void }) {
  const themeKey = useEquippedThemeKey();

  return (
    <div className="flex-1 overflow-hidden flex flex-col bg-platform-bg">
      <style>{GLOW_CSS}</style>
      <div className={`flex-1 overflow-hidden ${themeKey === "theme.event-horizon" ? "editor-event-horizon" : ""}`}>
        <MonacoEditor
          height="100%"
          theme={themeKey ? monacoThemeName(themeKey) : "vs-dark"}
          beforeMount={registerEditorThemes}
          language="java"
          value={code}
          onChange={(value) => onChange(value ?? "")}
          options={{ minimap: { enabled: false }, fontSize: 14 }}
        />
      </div>
    </div>
  );
}
