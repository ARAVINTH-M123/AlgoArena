"use client";

type Props = {
  open: boolean;
  verdict: string;
  expectedOutput?: string;
  actualOutput?: string;
  executionMessage?: string;
  onClose: () => void;
};

export default function SubmissionResultModal({
  open,
  verdict,
  expectedOutput,
  actualOutput,
  executionMessage,
  onClose
}: Props) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 p-4">
      <div className="panel w-full max-w-md">
        <h3 className="text-lg font-bold text-platform-accent">Submission Result</h3>
        <p className="mt-3 text-gray-300">Verdict: <span className="font-semibold text-white">{verdict}</span></p>
        {executionMessage && <p className="mt-2 text-sm text-gray-300">{executionMessage}</p>}
        {typeof expectedOutput === "string" && (
          <div className="mt-3">
            <p className="text-xs uppercase tracking-wide text-gray-400">Expected Output</p>
            <pre className="mt-1 max-h-24 overflow-auto rounded-md border border-platform-border p-2 text-xs text-gray-200">{expectedOutput || "(empty)"}</pre>
          </div>
        )}
        {typeof actualOutput === "string" && (
          <div className="mt-3">
            <p className="text-xs uppercase tracking-wide text-gray-400">Your Output</p>
            <pre className="mt-1 max-h-24 overflow-auto rounded-md border border-platform-border p-2 text-xs text-gray-200">{actualOutput || "(empty)"}</pre>
          </div>
        )}
        <button onClick={onClose} className="btn-accent mt-4">Close</button>
      </div>
    </div>
  );
}
