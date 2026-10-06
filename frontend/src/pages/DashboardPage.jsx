import { useCurrentUser } from "@/hooks/useAuth";
import PageLoader from "@/components/feedback/PageLoader";
import MemberDashboard from "@/pages/reports/MemberDashboard.jsx";
import ManagerDashboard from "@/pages/reports/ManagerDashboard.jsx";
import AdminDashboard from "@/pages/dashboard/AdminDashboard.jsx";

// /dashboard shows different content per role -- each role sees a different Figma frame at this
// same nav destination (team-member-dashboard / manager-dashboard / Admin dashboard), not variants
// of one shared layout. Keeping the role switch here (rather than three separate routes) matches
// how the Figma file treats "Dashboard" as one nav destination per role.
export default function DashboardPage() {
  const { data: user, isPending } = useCurrentUser();

  if (isPending) return <PageLoader />;

  if (user?.role === "ADMIN") {
    return <AdminDashboard />;
  }

  if (user?.role === "MANAGER") {
    return <ManagerDashboard />;
  }

  return <MemberDashboard />;
}
