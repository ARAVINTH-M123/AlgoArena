import { Check, Circle } from "lucide-react";
import { formatPoints, type NationDto, type RequirementDto } from "@/lib/nation";

type Props = {
  nation: NationDto;
};

function valueText(requirement: RequirementDto): string {
  if (requirement.type === "XP") {
    return `${formatPoints(requirement.current)} / ${formatPoints(requirement.required)}`;
  }
  if (requirement.type === "GATE_LANDMARK") {
    return requirement.met ? "Built" : "Not built yet";
  }
  return `${requirement.current.toLocaleString("en-US")} / ${requirement.required.toLocaleString("en-US")}`;
}

/** Always shows the exact concrete target for the next rank. */
export default function RankRequirements({ nation }: Props) {
  const next = nation.nextRank;

  if (!next) {
    return (
      <div>
        <h3 className="text-sm font-semibold uppercase tracking-wide text-gray-400">Next rank</h3>
        <p className="mt-2 text-sm text-gray-300">This nation has reached the final rank.</p>
      </div>
    );
  }

  const firstUnmet = next.requirements.find((r) => !r.met);

  return (
    <div>
      <h3 className="text-sm font-semibold uppercase tracking-wide text-gray-400">
        Next rank: <span className="text-platform-accent">{next.name}</span>
      </h3>
      <ul className="mt-2 space-y-2">
        {next.requirements.map((requirement) => (
          <li key={requirement.type} className="flex items-start gap-2 text-sm">
            {requirement.met ? (
              <Check className="mt-0.5 h-4 w-4 shrink-0 text-platform-accent" aria-label="Done" />
            ) : (
              <Circle className="mt-0.5 h-4 w-4 shrink-0 text-gray-500" aria-label="Not done" />
            )}
            <span className="flex-1 text-gray-200">{requirement.label}</span>
            <span className={requirement.met ? "text-platform-accent" : "text-gray-400"}>{valueText(requirement)}</span>
          </li>
        ))}
      </ul>
      {firstUnmet && <p className="mt-3 text-xs text-gray-400">Next target: {firstUnmet.label.toLowerCase()}.</p>}
    </div>
  );
}
