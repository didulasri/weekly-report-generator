import api from "./api";

// Thin wrappers over /api/manager -- MANAGER/ADMIN only.
export function listTeamReports({ status, projectId, userId, startDate, endDate, page = 0, size = 10 } = {}) {
  return api
    .get("/manager/reports", { params: { status, projectId, userId, startDate, endDate, page, size } })
    .then((res) => res.data);
}

export function listTeamMembers({ page = 0, size = 10 } = {}) {
  return api.get("/manager/team-members", { params: { page, size } }).then((res) => res.data);
}

export function getReportForReview(id) {
  return api.get(`/manager/reports/${id}`).then((res) => res.data);
}

export function approveReport(id, comment) {
  return api.post(`/manager/reports/${id}/approve`, { comment }).then((res) => res.data);
}

export function requestChanges(id, comment) {
  return api.post(`/manager/reports/${id}/request-changes`, { comment }).then((res) => res.data);
}
