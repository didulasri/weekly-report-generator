// Shown by RoleRoute for an authenticated user who lacks the required role. Deliberately styled
// nothing like the login redirect -- "you're not allowed here" and "you're not logged in" must
// never look like the same failure.
export default function Forbidden() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-red-50">
      <div className="text-center px-4">
        <h1 className="text-3xl font-bold text-red-600">403 — Forbidden</h1>
        <p className="mt-2 text-red-500">
          You don&apos;t have permission to view this page.
        </p>
      </div>
    </div>
  );
}
