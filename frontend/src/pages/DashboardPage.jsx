import { useCurrentUser } from "@/hooks/useAuth";
import PageLoader from "@/components/feedback/PageLoader";
import MemberDashboard from "@/pages/reports/MemberDashboard.jsx";
import ManagerDashboard from "@/pages/reports/ManagerDashboard.jsx";

// /dashboard shows different content per role -- a manager/admin needs team-wide review status,
// not their own personal report stats. Keeping the role switch here (rather than two separate
// routes) matches how the Figma file treats "Dashboard" as one nav destination per role.
export default function DashboardPage() {
  const { data: user, isPending } = useCurrentUser();

  if (isPending) return <PageLoader />;

  if (user?.role === "MANAGER" || user?.role === "ADMIN") {
    return <ManagerDashboard />;
  }

  return <MemberDashboard />;
}
