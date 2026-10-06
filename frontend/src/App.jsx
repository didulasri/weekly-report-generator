import { Navigate, Route, Routes } from "react-router-dom";
import Login from "@/pages/auth/Login.jsx";
import ForgotPassword from "@/pages/auth/ForgotPassword.jsx";
import ResetPassword from "@/pages/auth/ResetPassword.jsx";
import PlaceholderPage from "@/pages/PlaceholderPage.jsx";
import MyReports from "@/pages/reports/MyReports.jsx";
import CreateWeeklyReport from "@/pages/reports/CreateWeeklyReport.jsx";
import EditWeeklyReport from "@/pages/reports/EditWeeklyReport.jsx";
import TeamReports from "@/pages/reports/TeamReports.jsx";
import TeamMembers from "@/pages/reports/TeamMembers.jsx";
import ManagerProjects from "@/pages/reports/ManagerProjects.jsx";
import UsersList from "@/pages/users/UsersList.jsx";
import InviteUser from "@/pages/users/InviteUser.jsx";
import UserDetail from "@/pages/users/UserDetail.jsx";
import EditUser from "@/pages/users/EditUser.jsx";
import DashboardPage from "@/pages/DashboardPage.jsx";
import AppShell from "@/components/layout/AppShell.jsx";
import ProtectedRoute from "@/routes/ProtectedRoute.jsx";
import RoleRoute from "@/routes/RoleRoute.jsx";
import { NAV_ITEMS } from "@/routes/navItems";

// Per-path page overrides -- a nav item still routes through the generic placeholder unless it has
// a real screen here. Keyed by path so this stays a one-line addition per screen as more land.
const PAGE_OVERRIDES = {
  "/reports": MyReports,
  "/reports/new": CreateWeeklyReport,
  "/dashboard": DashboardPage,
  "/team": TeamReports,
  "/team/members": TeamMembers,
  "/projects": ManagerProjects,
  "/admin/users": UsersList,
};

// Route table built from NAV_ITEMS (routes/navItems.js) rather than hand-listed here, so a nav
// link and its route can never drift apart -- adding/renaming a destination happens in exactly
// one place. roleHomePath() in useAuth.js is still the "/" stub from the login checkpoint; "/"
// itself forwards to /dashboard -- the one destination every role has (My Reports is now
// TEAM_MEMBER-only, so redirecting there would 403 a manager/admin landing on "/").
function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/reset-password" element={<ResetPassword />} />

      <Route
        element={
          <ProtectedRoute>
            <AppShell />
          </ProtectedRoute>
        }
      >
        {NAV_ITEMS.map((item) => {
          const Page = PAGE_OVERRIDES[item.path];
          const page = Page ? <Page /> : <PlaceholderPage title={item.label} />;
          return (
            <Route
              key={item.path}
              path={item.path}
              element={item.roles ? <RoleRoute allow={item.roles}>{page}</RoleRoute> : page}
            />
          );
        })}
        {/* Not nav items -- reached by clicking a row/button on their parent list, so they aren't
            in NAV_ITEMS. */}
        <Route path="/reports/:reportId/edit" element={<EditWeeklyReport />} />
        <Route
          path="/admin/users/invite"
          element={
            <RoleRoute allow={["ADMIN"]}>
              <InviteUser />
            </RoleRoute>
          }
        />
        <Route
          path="/admin/users/:userId"
          element={
            <RoleRoute allow={["ADMIN"]}>
              <UserDetail />
            </RoleRoute>
          }
        />
        <Route
          path="/admin/users/:userId/edit"
          element={
            <RoleRoute allow={["ADMIN"]}>
              <EditUser />
            </RoleRoute>
          }
        />
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
      </Route>
    </Routes>
  );
}

export default App;
