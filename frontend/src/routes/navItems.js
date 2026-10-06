import {
  ClipboardList,
  FileText,
  FolderGit2,
  LayoutDashboard,
  PlusCircle,
  Settings,
  Users,
} from "lucide-react";

// Single source of truth for the sidebar nav AND the protected route table (App.jsx) -- both
// import this so a path can never drift between "what's linked" and "what's routed". `roles`
// scopes an item to specific roles; omitted means every authenticated role sees it.
//
// Each role gets a genuinely different nav, not supersets of one another -- confirmed against
// three separate Figma frames (team-member-dashboard, manager-dashboard/team-reports,
// users-list-admin). Notably, admin's own frame has no "Team Reports"/"Team Members" -- those are
// manager-specific, not "manager + admin" -- and admin gets its own "Users" and "Reports" instead.
// Projects and Settings are the only two destinations every role above TEAM_MEMBER actually shares.
//   TEAM_MEMBER   Dashboard, My Reports, New Report
//   MANAGER       Dashboard, Team Reports, Team Members, Projects, Settings
//   ADMIN         Dashboard, Users, Projects, Reports, Settings
export const NAV_ITEMS = [
  { label: "Dashboard", path: "/dashboard", icon: LayoutDashboard },
  { label: "My Reports", path: "/reports", icon: FileText, roles: ["TEAM_MEMBER"] },
  { label: "New Report", path: "/reports/new", icon: PlusCircle, roles: ["TEAM_MEMBER"] },
  { label: "Team Reports", path: "/team", icon: ClipboardList, roles: ["MANAGER"] },
  { label: "Team Members", path: "/team/members", icon: Users, roles: ["MANAGER"] },
  { label: "Projects", path: "/projects", icon: FolderGit2, roles: ["MANAGER", "ADMIN"] },
  { label: "Users", path: "/admin/users", icon: Users, roles: ["ADMIN"] },
  { label: "Reports", path: "/admin/reports", icon: FileText, roles: ["ADMIN"] },
  { label: "Settings", path: "/settings", icon: Settings, roles: ["MANAGER", "ADMIN"] },
];

export function navItemsForRole(role) {
  return NAV_ITEMS.filter((item) => !item.roles || item.roles.includes(role));
}
