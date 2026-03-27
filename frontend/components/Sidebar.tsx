import Link from "next/link";

const items = [
  { href: "/home", label: "Dashboard" },
  { href: "/problems", label: "Problem List" },
  { href: "/submissions", label: "Submissions" },
  { href: "/leaderboard", label: "Leaderboard" }
];

export default function Sidebar() {
  return (
    <aside className="panel h-fit w-full md:w-64">
      <h3 className="mb-3 text-sm font-semibold uppercase tracking-wider text-platform-accent">Navigation</h3>
      <div className="space-y-2">
        {items.map((item) => (
          <Link key={item.href} href={item.href} className="block rounded-md px-3 py-2 text-gray-300 transition hover:bg-platform-accentSoft hover:text-white">
            {item.label}
          </Link>
        ))}
      </div>
    </aside>
  );
}
