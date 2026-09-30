import { FileText, FolderGit2, LayoutDashboard, PlusCircle, Users } from "lucide-react";

// Single source of truth for the sidebar nav AND the protected route table (App.jsx) -- both
// import this so a path can never drift between "what's linked" and "what's routed". `roles`
// omitted means every authenticated role sees it (My Reports / New Report); otherwise it's the
// exact set from the task spec:
//   TEAM_MEMBER   My Reports, New Report
//   MANAGER       + Team Reports, Dashboard
//   ADMIN         + Users, Projects
export const NAV_ITEMS = [
  { label: "My Reports", path: "/reports", icon: FileText },
  { label: "New Report", path: "/reports/new", icon: PlusCircle },
  { label: "Team Reports", path: "/team", icon: Users, roles: ["MANAGER", "ADMIN"] },
  { label: "Dashboard", path: "/dashboard", icon: LayoutDashboard, roles: ["MANAGER", "ADMIN"] },
  { label: "Users", path: "/admin/users", icon: Users, roles: ["ADMIN"] },
  { label: "Projects", path: "/admin/projects", icon: FolderGit2, roles: ["ADMIN"] },
];

export function navItemsForRole(role) {
  return NAV_ITEMS.filter((item) => !item.roles || item.roles.includes(role));
}
