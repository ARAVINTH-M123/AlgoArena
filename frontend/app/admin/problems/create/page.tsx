"use client";

import { FormEvent, useState } from "react";
import { api, getApiErrorMessage } from "@/lib/api";
import RequireAdmin from "@/components/RequireAdmin";
import BackArrowButton from "@/components/BackArrowButton";

type TestCaseItem = {
  input: string;
  output: string;
};

export default function CreateProblemPage() {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [difficulty, setDifficulty] = useState("EASY");
  const [tags, setTags] = useState("");
  const [inputFormat, setInputFormat] = useState("");
  const [outputFormat, setOutputFormat] = useState("");
  const [constraints, setConstraints] = useState("");
  const [testCases, setTestCases] = useState<TestCaseItem[]>([]);
  const [draftInput, setDraftInput] = useState("");
  const [draftOutput, setDraftOutput] = useState("");
  const [editingIndex, setEditingIndex] = useState<number | null>(null);
  const [showTestCaseEditor, setShowTestCaseEditor] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  function startAddingTestCase() {
    setDraftInput("");
    setDraftOutput("");
    setEditingIndex(null);
    setShowTestCaseEditor(true);
  }

  function startEditingTestCase(index: number) {
    const selected = testCases[index];
    setDraftInput(selected.input);
    setDraftOutput(selected.output);
    setEditingIndex(index);
    setShowTestCaseEditor(true);
  }

  function cancelTestCaseEdit() {
    setDraftInput("");
    setDraftOutput("");
    setEditingIndex(null);
    setShowTestCaseEditor(false);
  }

  function saveTestCase() {
    if (!draftOutput.trim()) {
      setError("Testcase output cannot be empty");
      return;
    }

    setError("");
    const nextCase: TestCaseItem = { input: draftInput, output: draftOutput };

    if (editingIndex === null) {
      setTestCases((prev) => [...prev, nextCase]);
    } else {
      setTestCases((prev) => prev.map((item, idx) => (idx === editingIndex ? nextCase : item)));
    }

    cancelTestCaseEdit();
  }

  function toSampleIo(cases: TestCaseItem[]) {
    const first = cases[0];
    return `Input:\n${first.input}\n\nOutput:\n${first.output}`;
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setMessage("");
    setError("");

    if (showTestCaseEditor) {
      setError("Finish testcase editing before creating the problem");
      return;
    }

    if (testCases.length === 0) {
      setError("Add at least one testcase before creating the problem");
      return;
    }

    const token = localStorage.getItem("token");
    if (token) api.defaults.headers.common.Authorization = `Bearer ${token}`;
    try {
      await api.post("/problems", {
        title,
        description,
        difficulty,
        tags: tags.split(",").map((tag) => tag.trim()).filter(Boolean),
        inputFormat,
        outputFormat,
        constraints,
        sampleIo: toSampleIo(testCases),
        hiddenTestCases: JSON.stringify(testCases)
      });
      setMessage("Problem created successfully");
    } catch (err) {
      setError(getApiErrorMessage(err, "Problem creation failed"));
    }
  }

  return (
    <RequireAdmin>
      <section className="panel">
        <BackArrowButton fallbackHref="/admin/problems" className="mb-4" />
        <h1 className="text-2xl font-bold text-platform-accent">Create Problem</h1>
        <form onSubmit={onSubmit} className="mt-4 space-y-3">
          <input className="input-dark" placeholder="Title" value={title} onChange={(e) => setTitle(e.target.value)} required />
          <textarea className="input-dark min-h-48" placeholder="Description" value={description} onChange={(e) => setDescription(e.target.value)} required />
          <select className="input-dark w-56" value={difficulty} onChange={(e) => setDifficulty(e.target.value)}>
            <option value="EASY">Easy</option>
            <option value="MEDIUM">Medium</option>
            <option value="HARD">Hard</option>
          </select>
          <input className="input-dark" placeholder="Tags (comma separated)" value={tags} onChange={(e) => setTags(e.target.value)} required />
          <textarea className="input-dark" placeholder="Input Format" value={inputFormat} onChange={(e) => setInputFormat(e.target.value)} required />
          <textarea className="input-dark" placeholder="Output Format" value={outputFormat} onChange={(e) => setOutputFormat(e.target.value)} required />
          <textarea className="input-dark" placeholder="Constraints" value={constraints} onChange={(e) => setConstraints(e.target.value)} required />

          <div className="rounded-lg border border-platform-border p-3">
            <p className="text-sm font-semibold text-platform-accent">Testcases</p>

            {!showTestCaseEditor && (
              <button type="button" className="btn-accent mt-3" onClick={startAddingTestCase}>
                Add Testcase
              </button>
            )}

            {showTestCaseEditor && (
              <div className="mt-3 space-y-3">
                <textarea
                  className="input-dark min-h-24"
                  placeholder="Input"
                  value={draftInput}
                  onChange={(e) => setDraftInput(e.target.value)}
                />
                <textarea
                  className="input-dark min-h-24"
                  placeholder="Output"
                  value={draftOutput}
                  onChange={(e) => setDraftOutput(e.target.value)}
                  required
                />
                <div className="flex gap-2">
                  <button type="button" className="rounded-lg border border-platform-border px-4 py-2 text-sm text-gray-200" onClick={cancelTestCaseEdit}>
                    Cancel
                  </button>
                  <button type="button" className="btn-accent" onClick={saveTestCase}>
                    OK
                  </button>
                </div>
              </div>
            )}

            <div className="mt-4 space-y-2">
              {testCases.map((_, index) => (
                <button
                  key={index}
                  type="button"
                  onClick={() => startEditingTestCase(index)}
                  className="block w-full rounded-md border border-platform-border px-3 py-2 text-left text-sm text-gray-200 hover:border-platform-accent"
                >
                  Testcase-{index + 1}
                </button>
              ))}
            </div>
          </div>

          {error && <p className="text-sm text-red-400">{error}</p>}
          {message && <p className="text-sm text-green-400">{message}</p>}
          <button className="btn-accent">Create</button>
        </form>
      </section>
    </RequireAdmin>
  );
}
