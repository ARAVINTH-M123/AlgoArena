# AlgoArena - Full-Stack Problem Solving Platform

This workspace contains a LeetCode-like competitive programming platform with:

- `frontend/`: Next.js + Tailwind CSS (dark black + green accent UI)
- `backend/`: Spring Boot REST API (Java 21, Maven, JWT auth, JPA/Hibernate)

## Project Structure

```text
Problem_Solving/
├─ frontend/
│  ├─ app/
│  │  ├─ page.tsx
│  │  ├─ login/page.tsx
│  │  ├─ register/page.tsx
│  │  ├─ home/page.tsx
│  │  ├─ problems/page.tsx
│  │  ├─ problems/[id]/page.tsx
│  │  ├─ problems/[id]/solve/page.tsx
│  │  ├─ submissions/page.tsx
│  │  ├─ profile/[username]/page.tsx
│  │  ├─ leaderboard/page.tsx
│  │  ├─ contests/page.tsx
│  │  ├─ contests/[id]/page.tsx
│  │  ├─ contests/[id]/problems/[problemId]/page.tsx
│  │  └─ admin/... (dashboard, problems, contests, users)
│  ├─ components/
│  └─ lib/api.ts
├─ backend/
│  ├─ src/main/java/com/problemsolving/platform/
│  │  ├─ api/controller/
│  │  ├─ api/dto/
│  │  ├─ config/
│  │  ├─ domain/entity/
│  │  ├─ domain/enums/
│  │  ├─ repository/
│  │  ├─ security/
│  │  └─ service/
│  └─ src/main/resources/
│     ├─ application.yaml
│     └─ schema.sql
```

## Backend Features Implemented

- Spring Boot `4.0.3`, Java 21, JAR packaging, `application.yaml`
- JWT authentication with roles: `ADMIN`, `USER`
- Layered architecture: Controller → Service → Repository → DTO
- REST APIs for:
  - Authentication (`/api/auth`)
  - Problems (`/api/problems`)
  - Submissions (`/api/submissions`)
  - Contests (`/api/contests`)
  - Users/Profile (`/api/users`)
  - Leaderboard (`/api/leaderboard`)
  - Admin stats (`/api/admin/stats`)
- PostgreSQL + JPA/Hibernate entities:
  - users, problems, submissions, contests, leaderboard
  - contest_problems, problem_tags, bookmarks

## Frontend Features Implemented

- Next.js app-router setup with responsive dark theme UI
- Public pages: landing, login, register
- User pages: home, problems, problem detail, solve editor, submissions, profile, leaderboard, contests, contest details, contest problem page
- Admin pages: dashboard, manage/create/edit problems, manage/create contests, manage users
- Dynamic routes:
  - `/problems/[id]`
  - `/contests/[id]`
  - `/profile/[username]`
- Components:
  - Navbar, Sidebar
  - Problem table, Leaderboard table
  - Monaco code editor
  - Markdown renderer
  - Contest countdown timer
  - Submission result modal

## Run Instructions

### 1) Start local PostgreSQL service

- Ensure PostgreSQL is running on `localhost:5432`
- Use credentials:
  - Username: `postgres`
  - Password: `Itspostgresql`
- Create database `problem_platform` (if not already created)

### 2) Run Backend

```bash
cd backend
copy .env.example .env
./mvnw.cmd spring-boot:run
```

Backend URL: `http://localhost:8080`

### 3) Run Frontend

```bash
cd frontend
copy .env.example .env.local
npm install
npm run dev
```

Frontend URL: `http://localhost:3000`

## Sample API Calls

### Register

`POST /api/auth/register`

```json
{
  "username": "coder1",
  "email": "coder1@example.com",
  "password": "password123"
}
```

### Login

`POST /api/auth/login`

```json
{
  "usernameOrEmail": "coder1",
  "password": "password123"
}
```

### Create Problem (ADMIN)

`POST /api/problems`

### Submit Solution

`POST /api/submissions`

### Contest Leaderboard

`GET /api/leaderboard/contest/{contestId}`

## Nation system

Every user owns one personal **nation** that grows from empty land into a futuristic nation as they solve problems.
It is shown on the profile page (`/profile/<username>`) and its rank appears on the leaderboard.

**How it works**

- An accepted submission calls `NationService.awardForAcceptedSolve(...)`, the single integration point.
- A first solve gives Nation XP (easy 10, medium 30, hard 70) and track points for three build tracks:
  **Basics**, **Structures** and **Landmarks**. Items are built in a fixed order; leftover points carry over.
- After the 5th easy first-solve of a (nation-local) day, further easy solves give half. Solving a problem again
  after 24 hours gives a tiny 0.01% bonus; before that, nothing.
- Ten ranks: Null, Init, Boot, Root, Kernel, Stack, Cloud, Core, Quantum, Singularity, each with sub-tiers I to III.
  A rank-up needs enough XP, enough medium/hard solves, (for the top ranks) enough active days, and the rank's
  **gate landmark** to be built. Ranks never go down.
- **Presence**: a half-body figure behind the mountain shows whether the owner is online, idle or offline, live for
  visitors. Owners can choose "Appear offline".

**Configuration** (`application.yaml`, prefix `nation`)

```yaml
nation:
  award-enabled: true   # set to false on any public deployment until the judge runs in a sandbox
  xp-multiplier: 1      # development speed-up only; keep at 1 in production
```

All other gameplay numbers (XP, costs, rank table, cooldowns, presence timings) live in
`config/NationProperties.java`. For presence in production run a single backend instance (the live-session store
is in memory) and add `spring.threads.virtual.enabled=true`.

**API**

| Endpoint | Access | Purpose |
|---|---|---|
| `GET /api/nation/{username}` | public | nation, rank, requirements, tracks, items, presence |
| `GET /api/nation/me` | login | same, plus the owner's cooldown dates |
| `PUT /api/nation/me/profile` | login | `{ name?, timeZone?, appearOffline? }` |
| `POST /api/presence/heartbeat` | login | sent every ~30 s by each open tab |
| `POST /api/presence/offline` | login | sent when a tab closes |
| `GET /api/nation/{username}/presence/stream` | public | live presence (Server-Sent Events) |

`POST /api/submissions` returns an extra `nationResult` (XP, track points, completed items, rank-ups).
`GET /api/leaderboard` rows include `nationName`, `nationRankName`, `nationRankIndex` and `nationTier`.

**Tests**: `cd backend && ./mvnw clean verify` runs the nation and presence unit tests.

## Notes

- Submission verdicts are currently simulated in backend service to provide a working end-to-end prototype.
- You can replace the simulated judge with an actual sandboxed code execution engine.
- This setup is local PostgreSQL based; Docker is optional and not required.

## Management console (admin RBAC)

The admin area is a role-based management console, fully separated from the player experience.

### Roles

- `USER` (default) and `ADMIN` live as an enum on the `users` entity (`role` column).
- Public registration always creates `USER`; no public API accepts or changes a role.
- Spring Security stores the authority as `ROLE_ADMIN`; every `/api/admin/**` request is
  re-checked on the backend (URL rule + `@PreAuthorize`), regardless of what the UI shows.

### Admin login and sessions

- Player login (`POST /api/auth/login`) rejects ADMIN accounts; admin login
  (`POST /api/auth/admin/login`) rejects non-admin accounts, so the two flows cannot cross over.
- Frontend routes: `/admin/login` (admin login), `/admin` (management dashboard), `/admin/*`
  (console pages). Unauthenticated visitors are redirected to `/admin/login`; signed-in
  normal users are redirected to `/home`.
- The console keeps its own session (`sessionStorage`, namespaced `algoarena.admin.*`) and a
  dedicated axios instance, so an admin and a player session never overwrite each other.
  Every protected admin page re-verifies the session against `GET /api/admin/me`.
- Unauthenticated admin API calls get a JSON `401`; authenticated non-admin callers get a
  JSON `403` (configured via `RestAuthenticationEntryPoint` / `RestAccessDeniedHandler`).
- Logout clears the console session and returns to `/admin/login`; expired tokens are caught
  by a 401 interceptor and handled the same way.

### First admin account (bootstrap)

No admin credentials are hardcoded. On first startup the backend creates the initial admin
from environment variables (see `backend/.env.example`):

```bash
ADMIN_USERNAME=youradmin
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=change-me-8+chars   # stored as a BCrypt hash
# ADMIN_BOOTSTRAP_ENABLED=true     # optional kill switch
```

Rules enforced by `AdminBootstrapConfig`:

- If any `ADMIN` already exists, the bootstrap does nothing (never overwrites accounts).
- Missing or weak configuration is logged and skipped — the app still starts.
- There is no public admin registration endpoint; once setup succeeded the mechanism is inert.

### Management dashboard modules

`/admin` shows platform stats and the module registry. Live today: **User Management**
(read-only directory from `GET /api/admin/users` — no password hashes are ever returned) and
**Problem Management** (create/edit/delete). Planned modules (Submission Management,
Analytics, Announcements, Dataset Import, System Settings, Audit Logs) are registered as
placeholders and plug into the same `/api/admin/**` authorization boundary. Contest
management is deliberately deferred to a later phase.
