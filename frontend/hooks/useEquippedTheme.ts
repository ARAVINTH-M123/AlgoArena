"use client";

import { useEffect, useState } from "react";
import { getMyCosmetics } from "@/lib/api";

/** The editor theme in effect for the signed-in user, or null while unknown / if it could not be loaded. */
export function useEquippedThemeKey(): string | null {
  const [key, setKey] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    getMyCosmetics()
      .then((cosmetics) => {
        if (!cancelled) setKey(cosmetics.themeKey);
      })
      .catch(() => undefined);   // keep the default editor look
    return () => {
      cancelled = true;
    };
  }, []);

  return key;
}
