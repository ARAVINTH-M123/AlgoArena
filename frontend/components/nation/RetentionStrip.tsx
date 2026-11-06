import { type NationDto } from "@/lib/nation";

/**
 * Phase 7 retention strip: the current streak, the best streak and today's daily-goal progress.
 * Visible to everyone (streaks are public info, like the solve counters).
 */
export default function RetentionStrip({ nation }: { nation: NationDto }) {
  const { streakDays, bestStreakDays, solvesToday, dailyGoalTarget } = nation.retention;
  const percent = dailyGoalTarget > 0 ? Math.min(100, Math.floor((solvesToday / dailyGoalTarget) * 100)) : 0;
  const goalDone = dailyGoalTarget > 0 && solvesToday >= dailyGoalTarget;

  return (
    <div
      className="flex flex-wrap items-center gap-x-5 gap-y-2 rounded-lg border border-platform-border px-3 py-2 text-sm"
      aria-label="Streak and daily goal"
    >
      <span className="text-gray-200" title="Consecutive days with at least one first solve">
        🔥 <span className="font-bold">{streakDays}</span>-day streak
      </span>
      <span className="text-gray-400" title="Longest streak ever reached">
        Best: <span className="font-semibold text-gray-200">{bestStreakDays}</span>
      </span>
      <div className="min-w-45 flex-1" title="First solves today (any difficulty)">
        <div className="flex items-center justify-between gap-2 text-xs text-gray-300">
          <span>{goalDone ? "Daily goal reached" : "Daily goal"}</span>
          <span>
            {solvesToday}/{dailyGoalTarget}
          </span>
        </div>
        <div className="mt-1 h-1.5 w-full overflow-hidden rounded-full border border-platform-border bg-black">
          <div
            className="h-full rounded-full bg-platform-accent transition-[width] duration-500"
            style={{ width: `${percent}%` }}
          />
        </div>
      </div>
    </div>
  );
}
