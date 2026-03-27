export const AUTH_STATE_EVENT = "auth-state-changed";

export type AuthState = {
  token: string;
  username: string;
  role: string;
};

export function getAuthState(): AuthState | null {
  if (typeof window === "undefined") return null;

  const token = localStorage.getItem("token");
  const username = localStorage.getItem("username");
  const role = localStorage.getItem("role");

  if (!token || !username || !role) {
    return null;
  }

  return { token, username, role };
}

export function persistAuthState(state: AuthState) {
  if (typeof window === "undefined") return;

  localStorage.setItem("token", state.token);
  localStorage.setItem("username", state.username);
  localStorage.setItem("role", state.role);
  window.dispatchEvent(new Event(AUTH_STATE_EVENT));
}

export function clearAuthState() {
  if (typeof window === "undefined") return;

  localStorage.removeItem("token");
  localStorage.removeItem("username");
  localStorage.removeItem("role");
  window.dispatchEvent(new Event(AUTH_STATE_EVENT));
}