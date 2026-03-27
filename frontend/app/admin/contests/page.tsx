import RequireAdmin from "@/components/RequireAdmin";

export default function ManageContestsPage() {
  return (
    <RequireAdmin>
      <section className="panel">
        <h1 className="text-2xl font-bold text-platform-accent">Contest</h1>
        <p className="mt-2 text-gray-300">Contest management features are coming soon.</p>
      </section>
    </RequireAdmin>
  );
}
