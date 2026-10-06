import { ROLE_LABELS } from "@/utils/constants";

// Shared static mock data for the Users list and the User Detail screen -- UI-only checkpoint, no
// user service wired yet. `id` is a URL-safe slug derived from email, used by the row-click
// navigation and the detail route's useParams(). Role/status values mirror the real RoleName/
// account-status enums so this slots into real data later without a reshape.
export const ALL_USERS = [
  {
    id: "s-chen",
    name: "Sarah Chen",
    initials: "SC",
    email: "s.chen@company.com",
    role: "TEAM_MEMBER",
    status: "ACTIVE",
    created: "Jan 12, 2024",
    lastLogin: "Oct 24, 2024",
    lastLoginDetail: "Oct 24, 2024 (10m ago)",
    projects: [
      { name: "Acme Core Platform Integration", role: "Developer", status: "ACTIVE" },
      { name: "Global Billing Architecture Redesign", role: "Contributor", status: "ACTIVE" },
      { name: "Vulnerability Remediation Program", role: "Security Lead", status: "ACTIVE" },
    ],
    stats: { totalReports: 12, approvalRate: "100%", avgHours: "38.5h" },
    activity: [
      { title: "Logged into control center", time: "10m ago", detail: "IP: 192.168.1.45 (macOS Chrome)" },
      { title: "Submitted weekly report (Week 42)", time: "2h ago", detail: "Assigned to Global Billing Project" },
      { title: "Report for Week 41 approved", time: "1d ago", detail: "Approved by Admin Jane Doe" },
      { title: "Password changed successfully", time: "3d ago", detail: "Requested via self-service portal" },
      { title: "Assigned to Vulnerability Remediation", time: "5d ago", detail: "Added by Project Admin" },
      { title: "Logged into control center", time: "1w ago", detail: "IP: 192.168.1.45 (macOS Chrome)" },
      { title: "Submitted weekly report (Week 40)", time: "1w ago", detail: "Approved with no revisions" },
      { title: "Account onboarded by Jane Doe", time: "2w ago", detail: "Role initialized as Team Member" },
    ],
  },
  { id: "a-mercer", name: "Alex Mercer", initials: "AM", email: "a.mercer@company.com", role: "TEAM_MEMBER", status: "ACTIVE", created: "Feb 05, 2024", lastLogin: "Oct 24, 2024" },
  { id: "g-hopper", name: "Grace Hopper", initials: "GH", email: "g.hopper@company.com", role: "MANAGER", status: "ACTIVE", created: "Dec 15, 2023", lastLogin: "Oct 23, 2024" },
  { id: "h-rostova", name: "Helena Rostova", initials: "HR", email: "h.rostova@company.com", role: "TEAM_MEMBER", status: "PENDING", created: "Oct 22, 2024", lastLogin: "Never" },
  { id: "m-vance", name: "Marcus Vance", initials: "MV", email: "m.vance@company.com", role: "TEAM_MEMBER", status: "ACTIVE", created: "Mar 10, 2024", lastLogin: "Oct 24, 2024" },
  { id: "l-torvalds", name: "Linus Torvalds", initials: "LT", email: "l.torvalds@company.com", role: "TEAM_MEMBER", status: "ACTIVE", created: "Jan 02, 2024", lastLogin: "Oct 20, 2024" },
  { id: "a-lovelace", name: "Ada Lovelace", initials: "AL", email: "a.lovelace@company.com", role: "TEAM_MEMBER", status: "ACTIVE", created: "Jan 01, 2024", lastLogin: "Oct 22, 2024" },
  { id: "b-gates", name: "Bill Gates", initials: "BG", email: "b.gates@company.com", role: "MANAGER", status: "ACTIVE", created: "Nov 20, 2023", lastLogin: "Oct 19, 2024" },
  { id: "e-watson", name: "Emily Watson", initials: "EW", email: "e.watson@company.com", role: "ADMIN", status: "ACTIVE", created: "Aug 14, 2023", lastLogin: "Oct 24, 2024" },
  { id: "j-smith", name: "John Smith", initials: "JS", email: "j.smith@company.com", role: "TEAM_MEMBER", status: "SUSPENDED", created: "May 18, 2024", lastLogin: "Sep 30, 2024" },
];

export function getUserById(id) {
  return ALL_USERS.find((user) => user.id === id);
}

// Only Sarah Chen has curated project/stats/activity detail matching the Figma example -- every
// other row still needs something sensible to show when clicked, derived from the fields the list
// already has rather than leaving the detail screen blank.
export function userDetail(user) {
  if (user.projects) return user;
  return {
    ...user,
    lastLoginDetail: user.lastLogin,
    projects: [],
    stats: { totalReports: 0, approvalRate: "—", avgHours: "0.0h" },
    activity: [
      { title: "Account onboarded", time: user.created, detail: `Role initialized as ${ROLE_LABELS[user.role] ?? user.role}` },
    ],
  };
}
