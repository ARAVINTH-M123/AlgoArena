"use client";

import { useEffect, useMemo, useState } from "react";

export default function ContestTimer({ endTime }: { endTime: string }) {
  const end = useMemo(() => new Date(endTime).getTime(), [endTime]);
  const [remaining, setRemaining] = useState(end - Date.now());

  useEffect(() => {
    const timer = setInterval(() => setRemaining(end - Date.now()), 1000);
    return () => clearInterval(timer);
  }, [end]);

  const totalSec = Math.max(0, Math.floor(remaining / 1000));
  const hh = Math.floor(totalSec / 3600).toString().padStart(2, "0");
  const mm = Math.floor((totalSec % 3600) / 60).toString().padStart(2, "0");
  const ss = Math.floor(totalSec % 60).toString().padStart(2, "0");

  return <div className="rounded-md border border-platform-border px-3 py-2 text-platform-accent">{hh}:{mm}:{ss}</div>;
}
