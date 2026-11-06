# Nation system progress

Last updated: 2026-11-06 by CIEL (Phase 5B + Phase 6 + Phase 7 delivered)
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
  - [x] 5B: flags (7 patterns, 12 colors, 10 emblems by rank, FlagEditor, waving flagpole), 10 generated Web Audio anthems (AnthemPlayer + seeded scheduler), equip for ANTHEM + the flag via PUT /api/nation/me/profile
  - Verified in the delivery sandbox: backend clean verify 83/83 (Maven 3.9.9 + Temurin JDK 21), frontend tsc + next build green, 59/59 catalog keys in sync both sides, all 10 anthems deterministic (seeded PRNG) and clip-free, 12 Monaco/scene files type-checked
- [x] Phase 6: Mystery Caches (3/6/12% drops by difficulty, 40-solve pity, 70/22/7/1 open odds, every-10th >= Rare, duplicates -> shards (10/30/100/300), crafting 40/120/400/1200, nation_cosmetic_owned + cache_open_log tables, SKY/AURA/AMBIENT scene cosmetics, the Ascended Aura on Teleport-Gate completion, CacheReveal + /collection page with visible odds)
- [x] Phase 7: Extra XP and retention (daily goal 3 solves -> +50% XP each, streak with 3/7/14/30/60/100-day milestones, 14-day comeback bonus, lifetime solve milestones 10..1000, RetentionStrip + bonus lines in the result panel)

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
- Phase 5B notes:
  - Flag validation order: every field is validated BEFORE anything is applied (name, zone, flag, appear-offline); a bad color reports "Unknown flag color" even when the color count is also wrong, and the 2-3 color count is checked AFTER deduping (a repeated color is one color).
  - Flags: pattern + 2-3 colors + optional emblem stored on the nation row (flag_pattern, flag_colors comma-separated, flag_emblem - all nullable; null pattern = never customized = default flag of the rank). A rank-up unlocks exactly four cosmetics now: theme, border, anthem, emblem. Flag patterns/colors are owned by everyone from rank 0.
  - Anthems: fully generated in the browser (no audio files). lib/anthems.ts holds 10 seeded songs (pure buildEvents, seeded PRNG per key -> same events every time); lib/anthemPlayer.ts is a lookahead scheduler (250 ms poll, 2 s queue, ADSR gains, DynamicsCompressor, MASTER_LEVEL 0.3, 0.15 s/0.3 s fades, AudioContext created only inside play(), closed after fade-out). Mute state in localStorage key "anthemMuted"; never auto-plays.
  - Equip: ANTHEM joins THEME/BORDER in POST /api/cosmetics/equip ("auto" resets to the rank's anthem). The flag is NOT equipped via /equip: it is changed through PUT /api/nation/me/profile { flag: { pattern, colors, emblem } } so the same validate-before-apply rules apply.
  - NationDto now carries themeKey/borderKey/anthemKey + flag; CosmeticsDto carries anthemKey/pinnedAnthemKey + flag; new columns are all nullable (no migration risk for existing rows).
  - Frontend: lib/flags.ts (pattern geometry, 12 color hexes, 10 emblem glyph paths), lib/anthems.ts, lib/anthemPlayer.ts, Flag.tsx (waving flagpole, reduced-motion aware), FlagEditor.tsx, AnthemPlayer.tsx, useAnthem.ts. CosmeticPicker handles the flag parts.
  - Not verifiable in the sandbox: how the anthems actually sound, real browser playback, the live PostgreSQL ALTER TABLE (columns are nullable, so risk is minimal).
- Phase 6 notes:
  - All randomness comes from one injected java.util.Random (production: unseeded; tests: ScriptedRandom replays scripted doubles/ints), so every drop, rarity roll and pool pick is deterministic in tests.
  - Drop rolls happen ONLY on FIRST solves, inside the award transaction (re-solves and award-disabled deployments never roll); the 40-solve pity counts first-solves since the last DROP (caches can pile up unopened).
  - Rarity pity: the 10th open without a Rare-or-better is forced into the Rare-or-better space with renormalized weights (0..21 RARE, 22..28 EPIC, 29 LEGENDARY); the counter resets on any Rare or better.
  - Pool picks prefer UNOWNED items of the rolled rarity (collections fill up); only an exhausted rarity band can produce a duplicate, which converts to shards (10/30/100/300 = a quarter of the craft cost 40/120/400/1200).
  - Crafting targets a SPECIFIC unowned cache-pool key; insufficient shards, unknown keys, non-pool keys (rank cosmetics) and owned keys are rejected with clear messages.
  - Ownership: rank cosmetics stay DERIVED from the rank (no back-fill); cache/shard/special cosmetics are rows in nation_cosmetic_owned (unique user_id + cosmetic_key, acquired_via CACHE|SHARDS|SPECIAL). CosmeticSource gained SPECIAL for the Ascended Aura, granted idempotently when the Teleport Gate completes at Singularity (the false->true flip of nations.ascended).
  - New nation columns: unopened_caches, solves_since_cache, opens_since_rare (int default 0), shards (bigint default 0) - all with DB defaults, so ddl-auto=update migrates existing rows safely; equipped_sky_key / equipped_aura_key / equipped_ambient_key are nullable.
  - SKY/AURA/AMBIENT have NO rank default: nothing equipped means the rank's plain look; "auto" resets to none. CosmeticService.effectiveKey gained an ownedKeys overload; the 2-arg version still works for rank categories.
  - API: GET /api/cosmetics/caches (state + exact odds, login), POST /api/cosmetics/caches/open (reveal, login), POST /api/craft { key } (login). NationDto carries skyKey/auraKey/ambientKey; CosmeticsDto carries them plus their pinned keys; NationResultDto gained cacheDropped (result panel links to /collection).
  - Frontend: app/collection/page.tsx (unopened caches, shards, both pity bars, visible odds, craft grid by rarity, equip from the page), components/nation/CacheReveal.tsx (rarity-colored reveal card, duplicate -> shards messaging, open-next), Navbar gained Collection; CosmeticPicker gained SKY/AURA/AMBIENT sections with previews and "From caches or crafting" for locked pool items; NationScene applies cache skies and ambient motes (deterministic positions, reduced-motion aware); PresenceFigure draws the equipped aura ring (pulsing for animated looks, double ring for the Ascended Aura).
  - Catalog is now 72 items: 40 rank + 7 patterns + 12 colors + 12 cache pool + 1 Ascended Aura; key-sync check confirms all 72 keys exist on both sides.
  - Verified in the delivery sandbox: backend clean verify 102/102 (Maven 3.9.9 + Temurin JDK 21; 83 -> 102 with CacheServiceTest 15 + CosmeticServiceTest +3 + NationServiceTest +1), frontend tsc + next build green (/collection prerendered), 72/72 key sync, mutation check: breaking the HARD drop rate was caught by the tests.
  - Not verifiable in the sandbox: live PostgreSQL migration (new columns have defaults; risk minimal), real browser feel of the reveal animation.
- Phase 7 notes (Extra XP and retention):
  - All four retention features live in the ONE award transaction of NationService.awardForAcceptedSolve and only touch FIRST solves: the daily goal (+50% XP for each of the first 3 first-solves of the nation-local day), the streak (consecutive local days with >= 1 first-solve; milestone bonuses at 3/7/14/30/60/100 days), the comeback (first solve after >= 14 days away: flat +100 points) and lifetime solve milestones (10/25/50/100/250/500/1000 first-solves: one-time flat bonuses). Re-solves and award-disabled deployments never touch any of it.
  - Bonuses are XP-ONLY: track points (the building pace of the city) are unchanged, so item/rank pacing stays exactly as in Phases 1-6. Everything is multiplied by nation.xp-multiplier like the base XP, and the daily-goal bonus is a percent of the actually awarded XP (it respects the daily easy halving).
  - Streak milestones are per cycle: when the streak breaks, last_streak_milestone resets to 0 and the milestones can be earned again on the way back up (that is the retention point). Lifetime milestones are strictly once: last_lifetime_milestone never resets, and nations that existed before Phase 7 catch up (all crossed milestones granted at once, summed) on their next first-solve.
  - Streak bookkeeping keys off recordActiveDay's "newly added" return, so a second solve on the same day never grows the streak. The comeback compares local dates in the nation's CURRENT zone (a zone change mid-absence shifts the gap slightly; same accepted class of edge case as the daily halving).
  - New nation columns (Hibernate adds them automatically; all safe for existing rows): streak_days / best_streak_days / last_streak_milestone / last_lifetime_milestone (int default 0) and last_streak_day (nullable date). nation_daily_counters gained first_solves (int default 0); it is now created for every first-solve (not only easies).
  - API: NationResultDto gained bonusXp (already included in xpGained), bonusLabels (DAILY_GOAL, STREAK_MILESTONE_n, COMEBACK, LIFETIME_MILESTONE_n), streakDays, dailyGoalSolvedToday, dailyGoalTarget; NationDto gained retention { streakDays, bestStreakDays, solvesToday, dailyGoalTarget } (visitors see it too - streaks are public like the solve counters). NationStore gained a read-only findDailyCounter.
  - Frontend: lib/nation.ts gained RetentionDto + bonusLabelText (STREAK_MILESTONE_7 -> "7-day streak bonus"); NationResultPanel shows the bonus line and the streak/goal line; new RetentionStrip component (flame streak, best, goal progress bar) sits under the XP bar on the profile.
  - Config: nation.daily-goal-solves / daily-goal-bonus-percent / comeback-gap-days / comeback-bonus-points are yaml-overridable scalars; the milestone tables are defaults inside NationProperties (same pattern as the rank/XP tables).
  - Verified in the delivery sandbox: backend clean verify 111/111 (was 102; NationServiceTest +7 retention tests, NationProfileServiceTest +2), frontend tsc + next build green. Tests were updated where the default +50% goal bonus and the milestone bonuses shift XP expectations (first-solve tests, rank-up threshold tests now 11 hard solves instead of 12/13, 45-day profile test).
  - Not verifiable in the sandbox: live PostgreSQL migration (all new columns have defaults or are nullable; risk minimal), real browser feel of the strip and the reveal lines.
- Next: Phase 8 (open - no blueprint line yet).
