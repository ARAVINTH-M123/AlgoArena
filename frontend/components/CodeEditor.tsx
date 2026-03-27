"use client";

import dynamic from "next/dynamic";

const MonacoEditor = dynamic(() => import("@monaco-editor/react"), { ssr: false });

export default function CodeEditor({ code, onChange }: { code: string; onChange: (value: string) => void }) {
  return (
    <div className="flex-1 overflow-hidden flex flex-col bg-platform-bg">
      <div className="flex-1 overflow-hidden">
        <MonacoEditor
          height="100%"
          theme="vs-dark"
          language="java"
          value={code}
          onChange={(value) => onChange(value ?? "")}
          options={{ minimap: { enabled: false }, fontSize: 14 }}
        />
      </div>
    </div>
  );
}
