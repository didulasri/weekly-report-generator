import { Navigate, useLocation } from "react-router-dom";
import { useCurrentUser } from "../hooks/useAuth";
import FullPageLoader from "../components/FullPageLoader";

// Auth state is derived from GET /api/auth/me via useCurrentUser -- there is no separate
// isLoggedIn flag anywhere that could drift out of sync with the server. isPending genuinely means
// "don't know yet" (not "logged out"), so this renders a loader rather than ever flashing the
// login screen at someone who turns out to be authenticated once the request resolves.
export default function ProtectedRoute({ children }) {
  const { data: user, isPending, isError } = useCurrentUser();
  const location = useLocation();

  if (isPending) {
    return <FullPageLoader />;
  }

  if (isError || !user) {
    // Preserve the attempted path so the login flow can send the user back where they meant to go.
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return children;
}
