"use client";

import { use, useState } from "react";
import CodeEditor from "@/components/CodeEditor";
import ResizablePanelLayout from "@/components/ResizablePanelLayout";
import ProblemDescriptionPanel from "@/components/ProblemDescriptionPanel";
import CustomTestCases from "@/components/CustomTestCases";
import RequireAuth from "@/components/RequireAuth";
import { api, getApiErrorMessage, initApiAuth } from "@/lib/api";
import BackArrowButton from "@/components/BackArrowButton";

type EditorTab = "code" | "result";

export default function SolvePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const problemId = Number(id);
  
  const [code, setCode] = useState(
    "import java.util.*;\n\npublic class Main {\n  public static void main(String[] args) {\n    Scanner sc = new Scanner(System.in);\n    // write solution\n  }\n}"
  );
  const [verdict, setVerdict] = useState("");
  const [expectedOutput, setExpectedOutput] = useState("");
  const [actualOutput, setActualOutput] = useState("");
  const [failedTestcaseInput, setFailedTestcaseInput] = useState("");
  const [executionMessage, setExecutionMessage] = useState("");
  const [error, setError] = useState("");
  const [activeTab, setActiveTab] = useState<EditorTab>("code");
  const [hasResultTab, setHasResultTab] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit() {
    if (isSubmitting) return;

    initApiAuth();
    setError("");
    setIsSubmitting(true);

    try {
      const { data } = await api.post("/submissions", { 
        problemId, 
        language: "JAVA", 
        code
      });
      setVerdict(data.verdict);
      setExpectedOutput(data.expectedOutput ?? "");
      setActualOutput(data.actualOutput ?? "");
      setFailedTestcaseInput(data.failedTestcaseInput ?? "");
      setExecutionMessage(data.executionMessage ?? "");
      setHasResultTab(true);
      setActiveTab("result");
    } catch (err) {
      setError(getApiErrorMessage(err, "Submission failed"));
    } finally {
      setIsSubmitting(false);
    }
  }

  function closeResultTab() {
    setHasResultTab(false);
    setActiveTab("code");
  }

  function verdictClass(value: string) {
    if (value === "ACCEPTED") return "text-green-400";
    if (value === "WRONG_ANSWER" || value === "RUNTIME_ERROR" || value === "COMPILATION_ERROR") return "text-red-400";
    return "text-yellow-300";
  }

  const leftPanel = (
    <div className="flex flex-col h-full">
      <div className="flex items-center justify-between p-4 border-b border-platform-border bg-platform-bg">
        <div className="flex items-center gap-2">
          <BackArrowButton fallbackHref="/problems" label="Back" className="px-2 py-1 text-xs" />
          <button
            type="button"
            onClick={() => setActiveTab("code")}
            className={`rounded-md px-3 py-1.5 text-xs font-semibold uppercase tracking-wide transition ${
              activeTab === "code"
                ? "bg-platform-accentSoft text-platform-accent"
                : "text-gray-300 hover:bg-platform-panel"
            }`}
          >
            Code
          </button>

          {hasResultTab && (
            <div className="flex items-center rounded-md border border-platform-border bg-platform-panel">
              <button
                type="button"
                onClick={() => setActiveTab("result")}
                className={`px-3 py-1.5 text-xs font-semibold uppercase tracking-wide transition ${
                  activeTab === "result"
                    ? "text-platform-accent"
                    : "text-gray-300 hover:bg-platform-accentSoft"
                }`}
              >
                Result
              </button>
              <button
                type="button"
                onClick={closeResultTab}
                className="border-l border-platform-border px-2 py-1.5 text-xs text-gray-400 transition hover:text-red-300"
                aria-label="Close result tab"
                title="Close result tab"
              >
                X
              </button>
            </div>
          )}
        </div>

        <button 
          onClick={handleSubmit}
          className="px-3 py-1 text-sm btn-accent disabled:cursor-not-allowed disabled:opacity-60"
          disabled={isSubmitting}
        >
          {isSubmitting ? "Submitting..." : "Submit"}
        </button>
      </div>

      {activeTab === "code" || !hasResultTab ? (
        <CodeEditor code={code} onChange={setCode} />
      ) : (
        <div className="flex-1 overflow-y-auto bg-platform-bg p-4">
          <div className="space-y-3 panel">
            <h3 className="text-lg font-semibold text-platform-accent">Submission Result</h3>
            <p className="text-sm text-gray-200">
              Verdict: <span className={`font-semibold ${verdictClass(verdict)}`}>{verdict || "UNKNOWN"}</span>
            </p>

            {executionMessage && <p className="text-sm text-gray-300">{executionMessage}</p>}

            {failedTestcaseInput && (
              <div>
                <p className="text-xs uppercase tracking-wide text-gray-400">Failed Testcase Input</p>
                <pre className="mt-1 max-h-36 overflow-auto rounded-md border border-platform-border p-2 text-xs text-gray-200">{failedTestcaseInput}</pre>
              </div>
            )}

            {verdict !== "ACCEPTED" && typeof expectedOutput === "string" && (
              <div>
                <p className="text-xs uppercase tracking-wide text-gray-400">Expected Output</p>
                <pre className="mt-1 max-h-36 overflow-auto rounded-md border border-platform-border p-2 text-xs text-gray-200">{expectedOutput || "(empty)"}</pre>
              </div>
            )}

            {verdict !== "ACCEPTED" && typeof actualOutput === "string" && (
              <div>
                <p className="text-xs uppercase tracking-wide text-gray-400">Your Output</p>
                <pre className="mt-1 max-h-36 overflow-auto rounded-md border border-platform-border p-2 text-xs text-gray-200">{actualOutput || "(empty)"}</pre>
              </div>
            )}
          </div>
        </div>
      )}

      {error && (
        <div className="px-4 py-2 text-xs text-red-400 border-t border-platform-border bg-red-900/10">
          {error}
        </div>
      )}
    </div>
  );

  const rightTopPanel = (
    <ProblemDescriptionPanel problemId={problemId} />
  );

  const rightBottomPanel = (
    <CustomTestCases problemId={problemId} code={code} />
  );

  return (
    <RequireAuth>
      <ResizablePanelLayout
        leftPanel={leftPanel}
        rightTopPanel={rightTopPanel}
        rightBottomPanel={rightBottomPanel}
      />
    </RequireAuth>
  );
}
