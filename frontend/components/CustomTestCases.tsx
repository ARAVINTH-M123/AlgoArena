"use client";

import { useState } from "react";
import { api, getApiErrorMessage, initApiAuth } from "@/lib/api";

type Props = {
  problemId: number;
  code: string;
};

type TestCaseResult = {
  output: string;
  error?: string;
};

type TestCase = {
  id: number;
  label: string;
  input: string;
  result?: TestCaseResult;
};

export default function CustomTestCases({ problemId, code }: Props) {
  const MAX_TEST_CASES = 5;
  const [testCases, setTestCases] = useState<TestCase[]>([
    { id: 1, label: "Testcase-1", input: "" }
  ]);
  const [activeTestCaseId, setActiveTestCaseId] = useState(1);
  const [nextTestCaseNumber, setNextTestCaseNumber] = useState(2);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const activeTestCase = testCases.find((testCaseItem) => testCaseItem.id === activeTestCaseId) ?? testCases[0];

  const handleAddTestCase = () => {
    if (testCases.length >= MAX_TEST_CASES) {
      setError(`You can add at most ${MAX_TEST_CASES} testcases.`);
      return;
    }

    const newTestCase: TestCase = {
      id: Date.now(),
      label: `Testcase-${nextTestCaseNumber}`,
      input: ""
    };

    setTestCases((prev) => [...prev, newTestCase]);
    setActiveTestCaseId(newTestCase.id);
    setNextTestCaseNumber((prev) => prev + 1);
    setError("");
  };

  const handleDeleteTestCase = (testCaseId: number) => {
    if (testCases.length === 1) {
      setError("At least one testcase is required.");
      return;
    }

    const remainingTestCases = testCases.filter((testCaseItem) => testCaseItem.id !== testCaseId);
    setTestCases(remainingTestCases);

    if (activeTestCaseId === testCaseId) {
      setActiveTestCaseId(remainingTestCases[0].id);
    }

    setError("");
  };

  const handleInputChange = (value: string) => {
    setTestCases((prev) =>
      prev.map((testCaseItem) =>
        testCaseItem.id === activeTestCaseId ? { ...testCaseItem, input: value } : testCaseItem
      )
    );
  };

  const handleRunTest = async () => {
    const emptyTestCases = testCases.filter((testCaseItem) => !testCaseItem.input.trim());
    if (emptyTestCases.length > 0) {
      setError(`${emptyTestCases[0].label} is empty. Please enter input for all testcases.`);
      return;
    }

    setError("");
    setLoading(true);

    try {
      initApiAuth();

      const updatedTestCases: TestCase[] = [];

      for (const testCaseItem of testCases) {
        try {
          const { data } = await api.post("/executions/test", {
            problemId,
            language: "JAVA",
            code,
            input: testCaseItem.input
          });

          updatedTestCases.push({
            ...testCaseItem,
            result: {
              output: data.output || "",
              error: data.error && data.error.trim() ? data.error : undefined
            }
          });
        } catch (caseError) {
          updatedTestCases.push({
            ...testCaseItem,
            result: {
              output: "",
              error: getApiErrorMessage(caseError, "Test execution failed")
            }
          });
        }
      }

      setTestCases(updatedTestCases);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex flex-col h-full overflow-hidden bg-platform-bg">
      {/* Input Section */}
      <div className="flex flex-col flex-1 overflow-hidden border-b border-platform-border">
        <div className="border-b border-platform-border bg-platform-bg">
          <div className="flex items-center justify-between p-3">
            <h3 className="text-sm font-semibold text-platform-accent uppercase tracking-wide">
              Custom Input
            </h3>
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={handleAddTestCase}
                className="rounded border border-platform-border bg-platform-panel px-2.5 py-1 text-xs font-medium text-platform-text transition hover:border-platform-accent hover:text-platform-accent disabled:cursor-not-allowed disabled:opacity-50"
                aria-label="Add testcase"
                title="Add testcase"
                disabled={testCases.length >= MAX_TEST_CASES}
              >
                Add testcase
              </button>
              <button
                onClick={handleRunTest}
                disabled={loading}
                className="px-3 py-1 text-sm btn-accent disabled:opacity-50"
              >
                {loading ? "Running..." : "Run"}
              </button>
            </div>
          </div>

          <div className="flex items-center gap-2 overflow-x-auto px-3 pb-3">
            {testCases.map((testCaseItem) => (
              <div
                key={testCaseItem.id}
                className={`flex items-center rounded border ${
                  testCaseItem.id === activeTestCaseId
                    ? "border-platform-accent bg-platform-accentSoft"
                    : "border-platform-border bg-platform-panel"
                }`}
              >
                <button
                  type="button"
                  onClick={() => setActiveTestCaseId(testCaseItem.id)}
                  className={`whitespace-nowrap px-2.5 py-1 text-xs font-medium transition ${
                    testCaseItem.id === activeTestCaseId
                      ? "text-platform-accent"
                      : "text-gray-300 hover:text-platform-accent"
                  }`}
                >
                  {testCaseItem.label}
                </button>
                <button
                  type="button"
                  onClick={() => handleDeleteTestCase(testCaseItem.id)}
                  className="border-l border-platform-border px-2 py-1 text-xs text-gray-400 transition hover:text-red-300"
                  aria-label={`Delete ${testCaseItem.label}`}
                  title={`Delete ${testCaseItem.label}`}
                >
                  x
                </button>
              </div>
            ))}
          </div>
        </div>

        <div className="flex-1 overflow-hidden p-3">
          <textarea
            value={activeTestCase?.input ?? ""}
            onChange={(e) => handleInputChange(e.target.value)}
            placeholder="Enter custom test input here..."
            className="w-full h-full bg-platform-panel text-platform-text border border-platform-border rounded p-2 text-xs font-mono focus:outline-none focus:border-platform-accent resize-none"
          />
        </div>
        {error && <p className="text-xs text-red-400 px-3 pb-2">{error}</p>}
      </div>

      {/* Results Section */}
      <div className="flex-1 overflow-y-auto p-4">
        {!activeTestCase?.result ? (
          <p className="text-xs text-gray-500 text-center py-8">No test results yet</p>
        ) : (
          <div className="border border-platform-border rounded p-3 bg-platform-panel">
            <p className="text-xs font-semibold text-platform-accent mb-2">{activeTestCase.label}</p>

            <p className="text-xs text-gray-400 mb-1">Input:</p>
            <pre className="bg-platform-bg p-2 rounded text-xs text-gray-300 mb-2 max-h-20 overflow-auto">
              {activeTestCase.input}
            </pre>

            {activeTestCase.result.error ? (
              <>
                <p className="text-xs font-semibold text-red-400">Error:</p>
                <pre className="bg-platform-bg p-2 rounded text-xs text-red-300 max-h-20 overflow-auto">
                  {activeTestCase.result.error}
                </pre>
              </>
            ) : (
              <>
                <p className="text-xs text-gray-400 mb-1">Output:</p>
                <pre className="bg-platform-bg p-2 rounded text-xs text-gray-300 max-h-20 overflow-auto">
                  {activeTestCase.result.output || "(empty)"}
                </pre>
              </>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
