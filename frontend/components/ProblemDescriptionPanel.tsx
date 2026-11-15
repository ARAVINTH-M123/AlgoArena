"use client";

import { useEffect, useState } from "react";
import ReactMarkdown from "react-markdown";
import {
  AlignLeft,
  ArrowRightToLine,
  ArrowLeftFromLine,
  ScrollText,
  Terminal,
  type LucideIcon
} from "lucide-react";
import { api, getApiErrorMessage } from "@/lib/api";

type ProblemDetail = {
  id: number;
  title: string;
  description: string;
  difficulty: string;
  tags: string[];
  inputFormat: string;
  outputFormat: string;
  constraints: string;
  sampleIo: string;
};

type Props = {
  problemId: number;
};

/* ------------------------------------------------------------------ */
/* Subtopic section: icon badge + uppercase label + accent divider     */
/* wrapped in a bordered card that lights up on hover.                 */
/* ------------------------------------------------------------------ */

function SubtopicSection({
  icon: Icon,
  title,
  children
}: {
  icon: LucideIcon;
  title: string;
  children: React.ReactNode;
}) {
  return (
    <section className="group mb-4 rounded-xl border border-platform-border bg-platform-panel/70 p-4 transition-all duration-300 hover:border-platform-accent/60 hover:shadow-glow">
      <header className="mb-3 flex items-center gap-2.5">
        <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-md border border-platform-accent/30 bg-platform-accentSoft text-platform-accent transition-transform duration-300 group-hover:scale-105">
          <Icon size={15} strokeWidth={2.2} />
        </span>
        <h3 className="text-xs font-bold uppercase tracking-[0.14em] text-platform-accent">
          {title}
        </h3>
        <span
          aria-hidden="true"
          className="h-px flex-1 bg-gradient-to-r from-platform-accent/50 via-platform-accent/15 to-transparent"
        />
      </header>
      <div className="text-sm leading-relaxed text-gray-300">{children}</div>
    </section>
  );
}

/* Monospaced content block used by Input/Output/Sample sections. */
function MonoBlock({ value, emptyHint }: { value: string; emptyHint: string }) {
  if (!value || !value.trim()) {
    return <p className="text-xs italic text-gray-500">{emptyHint}</p>;
  }

  return (
    <pre className="max-h-64 overflow-auto whitespace-pre-wrap rounded-lg border border-platform-border bg-black p-3 font-mono text-xs text-gray-200">
      {value}
    </pre>
  );
}

const difficultyStyles: Record<string, string> = {
  EASY: "border-green-500/40 bg-green-500/10 text-green-400",
  MEDIUM: "border-amber-500/40 bg-amber-500/10 text-amber-400",
  HARD: "border-red-500/40 bg-red-500/10 text-red-400"
};

/* Markdown element styling (no typography plugin dependency). */
const markdownComponents = {
  h1: (props: React.ComponentProps<"h1">) => (
    <h1 className="mt-4 mb-2 border-b border-platform-border pb-1 text-lg font-bold text-platform-text first:mt-0" {...props} />
  ),
  h2: (props: React.ComponentProps<"h2">) => (
    <h2 className="mt-4 mb-2 text-base font-bold text-platform-accent first:mt-0" {...props} />
  ),
  h3: (props: React.ComponentProps<"h3">) => (
    <h3 className="mt-3 mb-1.5 text-sm font-bold uppercase tracking-wide text-platform-accent first:mt-0" {...props} />
  ),
  h4: (props: React.ComponentProps<"h4">) => (
    <h4 className="mt-3 mb-1.5 text-sm font-semibold text-platform-text first:mt-0" {...props} />
  ),
  p: (props: React.ComponentProps<"p">) => <p className="mb-2 last:mb-0" {...props} />,
  ul: (props: React.ComponentProps<"ul">) => (
    <ul className="mb-2 list-disc space-y-1 pl-5 last:mb-0 marker:text-platform-accent" {...props} />
  ),
  ol: (props: React.ComponentProps<"ol">) => (
    <ol className="mb-2 list-decimal space-y-1 pl-5 last:mb-0 marker:text-platform-accent" {...props} />
  ),
  li: (props: React.ComponentProps<"li">) => <li className="pl-1" {...props} />,
  strong: (props: React.ComponentProps<"strong">) => <strong className="font-semibold text-platform-text" {...props} />,
  em: (props: React.ComponentProps<"em">) => <em className="text-gray-200" {...props} />,
  a: (props: React.ComponentProps<"a">) => (
    <a className="text-platform-accent underline decoration-platform-accent/40 underline-offset-2 hover:decoration-platform-accent" {...props} />
  ),
  blockquote: (props: React.ComponentProps<"blockquote">) => (
    <blockquote
      className="mb-2 border-l-2 border-platform-accent/60 bg-platform-accentSoft/30 px-3 py-1.5 text-gray-200 last:mb-0"
      {...props}
    />
  ),
  hr: () => <hr className="my-3 border-platform-border" />,
  code: ({ className, children, ...rest }: React.ComponentProps<"code">) => {
    const isBlock = /language-/.test(className ?? "");
    if (isBlock) {
      return (
        <code className={`block overflow-x-auto rounded-lg border border-platform-border bg-black p-3 font-mono text-xs text-platform-accent ${className ?? ""}`} {...rest}>
          {children}
        </code>
      );
    }
    return (
      <code className="rounded border border-platform-border bg-platform-panel px-1 py-0.5 font-mono text-[0.8em] text-platform-accent" {...rest}>
        {children}
      </code>
    );
  },
  pre: (props: React.ComponentProps<"pre">) => <pre className="mb-2 last:mb-0" {...props} />,
  table: (props: React.ComponentProps<"table">) => (
    <div className="mb-2 overflow-x-auto rounded-lg border border-platform-border">
      <table className="w-full text-left text-xs" {...props} />
    </div>
  ),
  th: (props: React.ComponentProps<"th">) => (
    <th className="border-b border-platform-border bg-platform-panel px-3 py-1.5 font-semibold text-platform-accent" {...props} />
  ),
  td: (props: React.ComponentProps<"td">) => (
    <td className="border-b border-platform-border/60 px-3 py-1.5 align-top" {...props} />
  )
};

/* ------------------------------------------------------------------ */

export default function ProblemDescriptionPanel({ problemId }: Props) {
  const [problem, setProblem] = useState<ProblemDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    setLoading(true);
    setError("");

    api.get(`/problems/${problemId}`)
      .then((res) => {
        setProblem(res.data);
      })
      .catch((err) => {
        setProblem(null);
        setError(getApiErrorMessage(err, "Failed to load problem"));
      })
      .finally(() => setLoading(false));
  }, [problemId]);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-full">
        <p className="text-gray-400">Loading problem...</p>
      </div>
    );
  }

  if (!problem) {
    return (
      <div className="flex items-center justify-center h-full">
        <p className="text-red-400">{error || "Failed to load problem"}</p>
      </div>
    );
  }

  const difficultyClass = difficultyStyles[problem.difficulty.toUpperCase()] ??
    "border-platform-border bg-platform-panel text-gray-300";

  return (
    <div className="h-full overflow-y-auto bg-platform-bg p-4">
      <div className="max-w-full">
        {/* Problem Title */}
        <div className="mb-5">
          <div className="flex flex-wrap items-center gap-2.5">
            <h1 className="text-2xl font-bold text-platform-accent">{problem.title}</h1>
            <span
              className={`rounded-full border px-2.5 py-0.5 text-[0.65rem] font-bold uppercase tracking-wider ${difficultyClass}`}
            >
              {problem.difficulty}
            </span>
          </div>

          {problem.tags.length > 0 && (
            <div className="mt-2.5 flex flex-wrap gap-1.5">
              {problem.tags.map((tag) => (
                <span
                  key={tag}
                  className="rounded-full border border-platform-border bg-platform-panel px-2.5 py-0.5 text-[0.65rem] font-medium uppercase tracking-wide text-gray-400 transition-colors hover:border-platform-accent/50 hover:text-platform-accent"
                >
                  {tag}
                </span>
              ))}
            </div>
          )}
        </div>

        {/* Problem Description */}
        <SubtopicSection icon={ScrollText} title="Description">
          <div className="text-gray-300">
            <ReactMarkdown components={markdownComponents}>{problem.description}</ReactMarkdown>
          </div>
        </SubtopicSection>

        {/* Input/Output Format, Constraints and Sample I/O */}
        <SubtopicSection icon={ArrowRightToLine} title="Input Format">
          <MonoBlock value={problem.inputFormat} emptyHint="No input format provided." />
        </SubtopicSection>

        <SubtopicSection icon={ArrowLeftFromLine} title="Output Format">
          <MonoBlock value={problem.outputFormat} emptyHint="No output format provided." />
        </SubtopicSection>

        <SubtopicSection icon={AlignLeft} title="Constraints">
          <MonoBlock value={problem.constraints} emptyHint="No constraints provided." />
        </SubtopicSection>

        <SubtopicSection icon={Terminal} title="Sample I/O">
          <MonoBlock value={problem.sampleIo} emptyHint="No sample input/output provided." />
        </SubtopicSection>
      </div>
    </div>
  );
}
