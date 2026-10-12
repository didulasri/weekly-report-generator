import api from "./api";

// GET /api/reports/my is open to any authenticated role -- it's always scoped to the caller's own
// reports server-side, so there's no userId param to pass.
export function listMyReports({ status, projectId, startDate, endDate, page = 0, size = 10 } = {}) {
  return api
    .get("/reports/my", { params: { status, projectId, startDate, endDate, page, size } })
    .then((res) => res.data);
}

export function getReport(id) {
  return api.get(`/reports/${id}`).then((res) => res.data);
}

export function createReport(payload) {
  return api.post("/reports", payload).then((res) => res.data);
}

export function updateReport(id, payload) {
  return api.put(`/reports/${id}`, payload).then((res) => res.data);
}

export function deleteReport(id) {
  return api.delete(`/reports/${id}`).then((res) => res.data);
}

export function submitReport(id) {
  return api.post(`/reports/${id}/submit`).then((res) => res.data);
}

export function acknowledgeReview(reportId, reviewId) {
  return api.post(`/reports/${reportId}/reviews/${reviewId}/acknowledge`).then((res) => res.data);
}
