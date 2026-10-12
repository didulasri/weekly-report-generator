// Key factory so query invalidation is never a hand-typed array someone can typo or drift out of
// sync. Each domain exposes an `all` root (for invalidating everything in that domain) plus more
// specific keys built off it.
export const queryKeys = {
  auth: {
    all: ["auth"],
    currentUser: () => [...queryKeys.auth.all, "currentUser"],
  },
  invitations: {
    all: ["invitations"],
    list: (params) => [...queryKeys.invitations.all, "list", params ?? {}],
    validate: (token) => [...queryKeys.invitations.all, "validate", token],
  },
  users: {
    all: ["users"],
    list: (params) => [...queryKeys.users.all, "list", params ?? {}],
    detail: (id) => [...queryKeys.users.all, "detail", id],
  },
  projects: {
    all: ["projects"],
    list: (params) => [...queryKeys.projects.all, "list", params ?? {}],
    detail: (id) => [...queryKeys.projects.all, "detail", id],
    members: (id) => [...queryKeys.projects.all, "members", id],
  },
  reports: {
    all: ["reports"],
    mine: (params) => [...queryKeys.reports.all, "mine", params ?? {}],
    detail: (id) => [...queryKeys.reports.all, "detail", id],
  },
  managerReports: {
    all: ["managerReports"],
    list: (params) => [...queryKeys.managerReports.all, "list", params ?? {}],
    teamMembers: (params) => [...queryKeys.managerReports.all, "teamMembers", params ?? {}],
    detail: (id) => [...queryKeys.managerReports.all, "detail", id],
  },
  dashboard: {
    all: ["dashboard"],
    summary: (params) => [...queryKeys.dashboard.all, "summary", params ?? {}],
    statusSummary: (params) => [...queryKeys.dashboard.all, "statusSummary", params ?? {}],
    activityFeed: (params) => [...queryKeys.dashboard.all, "activityFeed", params ?? {}],
    workload: (params) => [...queryKeys.dashboard.all, "workload", params ?? {}],
  },
};
