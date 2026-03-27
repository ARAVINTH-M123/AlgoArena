import axios from "axios";
import { clearAuthState, getAuthState } from "@/lib/auth";

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
