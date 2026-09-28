// Rendered while auth state is genuinely unknown (useCurrentUser is pending) -- never the login
// screen, since that would flash at an already-authenticated user for a moment on every hard
// refresh.
export default function FullPageLoader() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="flex flex-col items-center gap-3 text-gray-500">
        <div className="h-8 w-8 animate-spin rounded-full border-2 border-gray-300 border-t-blue-600" />
        <span className="text-sm">Loading...</span>
      </div>
    </div>
  );
}
