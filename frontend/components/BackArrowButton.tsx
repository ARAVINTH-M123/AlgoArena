"use client";

import { useRouter } from "next/navigation";

type Props = {
  fallbackHref: string;
  label?: string;
  className?: string;
};

export default function BackArrowButton({ fallbackHref, label = "Back", className = "" }: Props) {
  const router = useRouter();

  const handleBack = () => {
    if (window.history.length > 1) {
      router.back();
      return;
    }
    router.push(fallbackHref);
  };

  return (
    <button
      type="button"
      onClick={handleBack}
      className={`inline-flex items-center gap-2 rounded-md border border-platform-border px-3 py-1.5 text-sm text-gray-200 transition hover:border-platform-accent hover:text-platform-accent ${className}`}
      aria-label={label}
      title={label}
    >
      <span aria-hidden="true">&lt;</span>
      <span>{label}</span>
    </button>
  );
}
