import { Navigate, useLocation } from "react-router-dom";
import { useCurrentUser } from "../hooks/useAuth";
import FullPageLoader from "../components/FullPageLoader";
import Forbidden from "../components/Forbidden";

// Self-contained rather than composed with ProtectedRoute (react-router children are a fixed
// element, not a render-prop, so ProtectedRoute has no way to hand this the user it fetched) --
// but the underlying useCurrentUser() call is the same cached React Query entry either way, so
// nesting <RoleRoute> under <ProtectedRoute> in a route tree costs nothing extra.
//
// A valid, authenticated user without an allowed role gets Forbidden, never a redirect to
// /login -- "not logged in" and "logged in but not allowed" are different failures and must look
// different, or a legitimately-authenticated manager who mistypes a URL would see the same screen
// as someone who was never logged in at all.
export default function RoleRoute({ allow, children }) {
  const { data: user, isPending, isError } = useCurrentUser();
  const location = useLocation();

  if (isPending) {
    return <FullPageLoader />;
  }

  if (isError || !user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (!allow.includes(user.role)) {
    return <Forbidden />;
  }

  return children;
}
