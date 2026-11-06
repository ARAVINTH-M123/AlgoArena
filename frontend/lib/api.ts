import axios from "axios";
import { clearAuthState, getAuthState } from "@/lib/auth";
import type {
  CacheOpenResultDto,
  CacheStateDto,
  CraftResultDto,
  CosmeticsDto,
  EquippableCategory
} from "@/lib/cosmetics";
import type { NationDto, UpdateNationProfilePayload } from "@/lib/nation";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api";

export const api = axios.create({
  baseURL: API_BASE_URL
});

let authInitialized = false;

export function initApiAuth() {
  if (authInitialized || typeof window === "undefined") {
    return;
  }

  authInitialized = true;
  const authState = getAuthState();
  if (authState?.token) {
    setToken(authState.token);
  }
}

export function setToken(token: string) {
  api.defaults.headers.common.Authorization = `Bearer ${token}`;
}

export function clearToken() {
  delete api.defaults.headers.common.Authorization;
}

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error) && error.response?.status === 401 && typeof window !== "undefined") {
      clearToken();
      clearAuthState();
    }
    return Promise.reject(error);
  }
);

export function getApiErrorMessage(error: unknown, fallback = "Request failed") {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as
      | { message?: string; error?: string; detail?: string; path?: string }
      | string
      | undefined;

    if (typeof data === "string" && data.trim().length > 0) {
      return data;
    }

    if (data && typeof data === "object") {
      const message = data.message?.trim();
      if (message) return message;
      const detail = data.detail?.trim();
      if (detail) return detail;
      const errorText = data.error?.trim();
      if (errorText) return errorText;
    }

    if (error.response?.status === 401) {
      return "Please login to run or submit code.";
    }
    if (error.response?.status === 403) {
      return "You are not allowed to perform this action.";
    }
  }
  return fallback;
}

// ---------------------------------------------------------------- Nation system

/** Public: anyone can view a nation. The token is attached when present so the owner gets owner-only fields. */
export async function getNation(username: string): Promise<NationDto> {
  initApiAuth();
  const response = await api.get<NationDto>(`/nation/${encodeURIComponent(username)}`);
  return response.data;
}

export async function getMyNation(): Promise<NationDto> {
  initApiAuth();
  const response = await api.get<NationDto>("/nation/me");
  return response.data;
}

export async function updateNationProfile(payload: UpdateNationProfilePayload): Promise<NationDto> {
  initApiAuth();
  const response = await api.put<NationDto>("/nation/me/profile", payload);
  return response.data;
}

// ---------------------------------------------------------------- Presence

export async function sendHeartbeat(sessionId: string, active: boolean): Promise<void> {
  initApiAuth();
  await api.post("/presence/heartbeat", { sessionId, active });
}

/**
 * Tells the server this tab is closing. Uses fetch with keepalive (it survives the page unloading) instead of
 * navigator.sendBeacon, because sendBeacon cannot attach the Authorization header.
 */
export function sendOfflineBeacon(sessionId: string): void {
  const authState = getAuthState();
  if (!authState?.token) return;
  try {
    void fetch(`${API_BASE_URL}/presence/offline`, {
      method: "POST",
      keepalive: true,
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${authState.token}` },
      body: JSON.stringify({ sessionId })
    }).catch(() => undefined);
  } catch {
    // best effort only
  }
}

export function presenceStreamUrl(username: string): string {
  return `${API_BASE_URL}/nation/${encodeURIComponent(username)}/presence/stream`;
}

// ---------------------------------------------------------------- Cosmetics

export async function getMyCosmetics(): Promise<CosmeticsDto> {
  initApiAuth();
  const response = await api.get<CosmeticsDto>("/cosmetics/me");
  return response.data;
}

/** key null (or "auto") goes back to the automatic cosmetic of the current rank (or none for scene cosmetics). */
export async function equipCosmetic(category: EquippableCategory, key: string | null): Promise<CosmeticsDto> {
  initApiAuth();
  const response = await api.post<CosmeticsDto>("/cosmetics/equip", { category, key });
  return response.data;
}

/** Cache and shard state plus the exact odds (login). */
export async function getCacheState(): Promise<CacheStateDto> {
  initApiAuth();
  const response = await api.get<CacheStateDto>("/cosmetics/caches");
  return response.data;
}

/** Opens one Mystery Cache and reveals what it contained. */
export async function openCache(): Promise<CacheOpenResultDto> {
  initApiAuth();
  const response = await api.post<CacheOpenResultDto>("/cosmetics/caches/open");
  return response.data;
}

/** Crafts one cache-pool cosmetic for shards. */
export async function craftCosmetic(key: string): Promise<CraftResultDto> {
  initApiAuth();
  const response = await api.post<CraftResultDto>("/cosmetics/craft", { key });
  return response.data;
}
