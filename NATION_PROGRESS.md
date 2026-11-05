# Nation system progress

Last updated: 2026-10-30 by Claude (Phase 5A delivered)
Branch: feature/nation

## Checklist
- [x] Phase 1: Backend core
  - [x] Config (NationProperties) + application.yaml switches
  - [x] Entities + repositories (tables created by Hibernate ddl-auto=update)
  - [x] Catalog (in code), E1-E5
  - [x] NationService (first solve, daily easy halving, re-solve bonus, tracks, gate landmarks, ranks, sub-tiers, active days, time zones)
  - [x] Hooked into accepted submissions + nationResult in submission response
  - [x] Tests for the above (NationServiceTest, in-memory store)
  - [x] 1B: GET /api/nation/{username} (public), GET /api/nation/me, PUT /api/nation/me/profile (rename + time zone with cooldowns/validation) + tests (NationProfileServiceTest)
- [x] Phase 2: Profile Nation view (scene, city grid, rank badge, XP bar, requirements, track bars, name/time-zone settings)
- [x] Phase 3: Presence (heartbeat, status rules, SSE stream with polling fallback, appear-offline, figure behind the mountain)
- [x] Phase 4: Feedback and polish (result panel, rank-up overlay, nation rank on the leaderboard, README section)
- [ ] Phase 5: Cosmetics
  - [x] 5A: ownership + equip API, 10 Monaco editor themes, 10 nation frames, pickers, unlock messages
  - [ ] 5B: flags (pattern/colors/emblem by rank), anthems (Web Audio), equip for both
- [ ] Phase 6: Mystery Caches
- [ ] Phase 7: Extra XP and retention

## Decisions and deviations from the blueprint
- Item catalog lives in code (NationCatalog), not in a nation_item_catalog table; completed items store catalog_key. Same behavior, fewer moving parts.
- Only scalar switches are overridable in application.yaml; the rank/XP/cost tables are defaults inside NationProperties (still the single central place).
- Tables come from Hibernate ddl-auto=update (the repo does not run schema.sql). No schema.sql change needed.
- Cache/pity/shard/cosmetic columns are deferred to Phases 5-6 (not in the nation table yet).
- Blueprint 4.5 "a single solve can never complete an item" is implemented as "never completes more than one item per track at multiplier 1" (overflow carry means items do complete).
- xp-multiplier also scales the re-solve bonus (testing convenience).
- The judge in the repo is real but NOT sandboxed (blueprint 2.3 said simulated). Keep nation.award-enabled=false on any public deployment until the judge runs in a sandbox.
- Award failures are caught in SubmissionService and logged; they never break a submission.

## Known issues / next steps
- Run backend `clean verify` locally (the author could not run Maven; code was compiled and the tests run against stand-ins for Spring/JPA/JUnit).
- Reading a nation never creates one: a user with no solves gets an empty default nation (rank Null) until their first award or profile update.
- Name filter is a short starter list (NationProperties.BLOCKED_NAME_WORDS); extend it.
- GET /api/nation/{username} is public (same as profiles); /me and the PUT require login. Cooldown dates are only returned to the owner.
- appearOffline, flag, equipped cosmetics and presence are NOT in the nation DTO yet (Phases 3 and 5).
- Phase 2 art is v1: emoji tiles on a layered SVG scene (sky, clouds, mountain ridge, city). The city holds 30 landmarks / 72 structures / 120 basics visually (three stacked layers); beyond that the oldest are hidden and a "+N older buildings" label shows. Territory does not grow with rank yet (polish).
- The nation border is a plain accent-colored frame; per-rank borders, flags and themes are Phase 5.
- Time zone: the blueprint says "default from the browser at creation". Doing that silently would use up the 30-day cooldown, so the settings form only OFFERS "Use my browser time zone" when the zone is still different.
- The owner view (settings form) depends on the API token: the Nation API helpers call initApiAuth() first.
- Presence figure (above the mountain) is Phase 3; result panel and rank-up overlay are Phase 4.
- Presence store: only the in-memory implementation exists (correct for ONE backend instance). The PresenceStore interface is ready for a Redis implementation (presence.store=redis in the blueprint); it was not added because it needs a new dependency and Redis infrastructure.
- Offline signal: the blueprint says navigator.sendBeacon, but sendBeacon cannot send the Authorization header. The client uses fetch(..., { keepalive: true }) with the token instead (same effect, authenticated).
- Visitors watch through SSE (public, EventSource cannot send auth headers). Owners see their TRUE status through a nation refresh every 30 s instead, so "Appear offline" never hides their own status from them.
- New nations columns (added automatically by Hibernate): appear_offline (default false) and last_seen_at. Add spring.threads.virtual.enabled=true to application.yaml.
- SSE per-client limit uses request.getRemoteAddr(); behind a reverse proxy configure forwarded headers so it sees real client IPs.
- The glow after a first-solve is triggered from SubmissionService (presenceService.markSolved) when the Nation result mode is FIRST.
- Presence art is v1 (one silhouette, rank styling by stroke/crown/rays/halo); aura-color cosmetics come in Phase 5.
- The nation result panel is shown in the Result tab of the solve page (app/problems/[id]/solve/page.tsx). The existing SubmissionResultModal component is not used anywhere in the app, so it was left alone.
- NationResultDto gained a "reason" field: for NO_OP it says ALREADY_COUNTED (panel explains the 24 h re-solve rule) or AWARDS_DISABLED (panel shows nothing).
- The light beam after a solve is part of the presence scene (Phase 3) on the profile page; it is not drawn on the solve page.
- Leaderboard: LeaderboardService now also returns nationName / nationRankName / nationRankIndex / nationTier per row (one batched query via NationRankLookup). Ordering and scores are unchanged (verified identical to the old logic on 150 random users incl. ties). The board is still ordered by score, not by nation rank.
- Rank-up overlay appears right after the accepted submission that caused it; rank-up rewards (cosmetics, caches) come with Phases 5 and 6.
- README: added a "Nation system" section. The old Notes section still says the judge is simulated; that is outdated (it is a real but unsandboxed judge).
- Rank cosmetics are DERIVED from the nation's rank (owned = rank reached). No ownership rows exist yet, so nothing needs back-filling for existing nations; the nation_cosmetic_owned table arrives with Phase 6 (caches/shards).
- "Automatic" (null) follows the cosmetic of the current rank, so a rank-up visibly changes the look; picking one explicitly pins it, and it stays pinned across rank-ups.
- New nations columns (added by Hibernate, nullable): equipped_theme_key, equipped_border_key.
- API: GET /api/cosmetics/catalog (public), GET /api/cosmetics/me, POST /api/cosmetics/equip { category: THEME|BORDER, key|"auto" } (login). NationDto now carries themeKey/borderKey (visitors see the owner's frame); NationResultDto carries "unlocked".
- The look of every key lives in the frontend (lib/cosmetics.ts, lib/editorThemes.ts) keyed by the backend catalog keys; a check confirms both sides define exactly the same 10 theme + 10 border keys. If you add a cosmetic, add it in both places.
- Gradients use plain one-position color stops on purpose (the two-position shorthand is not supported everywhere).
- The editor falls back to the built-in vs-dark theme while the equipped theme loads or if it cannot be loaded.
- Not done yet: the Singularity "Ascended" Legendary cosmetic (granted when the Teleport Gate completes) and cosmetics from caches/shards: Phase 6.
- Next: Phase 5B (flags and anthems).
