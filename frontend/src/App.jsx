import { Route, Routes } from "react-router-dom";
import Login from "@/pages/auth/Login.jsx";
import ProtectedRoute from "@/routes/ProtectedRoute.jsx";
import { useCurrentUser } from "@/hooks/useAuth";

// Placeholder for the authenticated landing route -- roleHomePath() in useAuth.js is a deliberate
// stub returning "/" until real dashboard/manager/admin pages exist. Rendered behind
// ProtectedRoute so an unauthenticated visit to "/" still redirects to /login, but a user who just
// authenticated (and was navigated to "/" by useLogin) actually lands here instead of bouncing
// straight back to /login -- an unconditional <Navigate to="/login"> here would loop a fresh login
// right back to the login screen.
function AuthenticatedHomeStub() {
  const { data: user } = useCurrentUser();
  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50 p-4 text-center">
      <p className="text-gray-600">
        Signed in as <span className="font-medium">{user?.email}</span> ({user?.role}). No
        landing page has been built yet.
      </p>
    </div>
  );
}

// Minimal shell -- only enough routing exists to mount the screens built so far.
function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <AuthenticatedHomeStub />
          </ProtectedRoute>
        }
      />
    </Routes>
  );
}

export default App;
