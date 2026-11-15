/**
 * Admin (management console) session handling.
 *
 * Deliberately separate from the player session in lib/auth.ts so that:
 * - an admin and a normal user can hold sessions in the same browser without
 *   overwriting each other (previously both used the same localStorage keys);
 * - leaving the console ends the admin session with the browser tab session.
 *
 * The admin token lives in sessionStorage under namespaced keys, so it is never
 * persisted to disk and never leaks into the public user session. It is attached
 * to requests by lib/api.ts (`adminApi`), and every protected admin route still
 * re-verifies the session against the backend (GET /api/admin/me) — the stored
 * role is treated as a hint, never as proof of authorization.
 */

export const ADMIN_AUTH_STATE_EVENT = "admin-auth-state-changed";

const TOKEN_KEY = "algoarena.admin.token";
const USERNAME_KEY = "algoarena.admin.username";
const ROLE_KEY = "algoarena.admin.role";

export type AdminAuthState = {
  token: string;
  username: string;
  role: string;
};

export function getAdminAuthState(): AdminAuthState | null {
  if (typeof window === "undefined") return null;

  const token = sessionStorage.getItem(TOKEN_KEY);
  const username = sessionStorage.getItem(USERNAME_KEY);
  const role = sessionStorage.getItem(ROLE_KEY);

  if (!token || !username || !role) {
    return null;
  }

  return { token, username, role };
}

export function persistAdminAuthState(state: AdminAuthState) {
  if (typeof window === "undefined") return;

  sessionStorage.setItem(TOKEN_KEY, state.token);
  sessionStorage.setItem(USERNAME_KEY, state.username);
  sessionStorage.setItem(ROLE_KEY, state.role);
  window.dispatchEvent(new Event(ADMIN_AUTH_STATE_EVENT));
}

export function clearAdminAuthState() {
  if (typeof window === "undefined") return;

  sessionStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(USERNAME_KEY);
  sessionStorage.removeItem(ROLE_KEY);
  window.dispatchEvent(new Event(ADMIN_AUTH_STATE_EVENT));
}

/** Local hint only — real authorization is decided by the backend on every request. */
export function isAdminSession(state: AdminAuthState | null): boolean {
  return state?.role === "ADMIN";
}
