import api from "./api";

// Thin wrappers over /api/dashboard -- MANAGER/ADMIN only (the backend enforces this; these calls
// 403 for a TEAM_MEMBER). summary/status-summary are deliberately week-scoped (weekStart +
// projectId), not a date range -- see DashboardService's own note on why.

export function getSummary({ weekStart, projectId } = {}) {
  return api.get("/dashboard/summary", { params: { weekStart, projectId } }).then((res) => res.data);
}

export function getStatusSummary({ weekStart, projectId } = {}) {
  return api
    .get("/dashboard/status-summary", { params: { weekStart, projectId } })
    .then((res) => res.data);
}

export function getActivityFeed({ page = 0, size = 5 } = {}) {
  return api.get("/dashboard/activity-feed", { params: { page, size } }).then((res) => res.data);
}

// Range-based (defaults to the current Monday-Sunday week when startDate/endDate are omitted),
// unlike summary/status-summary above.
export function getWorkload({ startDate, endDate, projectId } = {}) {
  return api
    .get("/dashboard/workload", { params: { startDate, endDate, projectId } })
    .then((res) => res.data);
}
