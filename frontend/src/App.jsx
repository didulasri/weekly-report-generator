import { Navigate, Route, Routes } from "react-router-dom";
import Login from "@/pages/auth/Login.jsx";
import PlaceholderPage from "@/pages/PlaceholderPage.jsx";
import AppShell from "@/components/layout/AppShell.jsx";
import ProtectedRoute from "@/routes/ProtectedRoute.jsx";
import RoleRoute from "@/routes/RoleRoute.jsx";
import { NAV_ITEMS } from "@/routes/navItems";

// Route table built from NAV_ITEMS (routes/navItems.js) rather than hand-listed here, so a nav
// link and its route can never drift apart -- adding/renaming a destination happens in exactly
// one place. roleHomePath() in useAuth.js is still the "/" stub from the login checkpoint; "/"
// itself just forwards to /reports, which is now a real (if placeholder) destination for every
// role, rather than useLogin needing to know real paths yet.
function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />

      <Route
        element={
          <ProtectedRoute>
            <AppShell />
          </ProtectedRoute>
        }
      >
        {NAV_ITEMS.map((item) => {
          const page = <PlaceholderPage title={item.label} />;
          return (
            <Route
              key={item.path}
              path={item.path}
              element={item.roles ? <RoleRoute allow={item.roles}>{page}</RoleRoute> : page}
            />
          );
        })}
        <Route path="/" element={<Navigate to="/reports" replace />} />
      </Route>
    </Routes>
  );
}

export default App;
