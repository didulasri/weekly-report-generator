import api from "./api";

// Thin wrappers over the admin user-management endpoints -- every one just unwraps response.data,
// no other logic. Hooks and components call these, never api.js directly.

export function listUsers({ role, active, search, page = 0, size = 10 } = {}) {
  return api
    .get("/admin/users", { params: { role, active, search, page, size } })
    .then((res) => res.data);
}

export function getUser(id) {
  return api.get(`/admin/users/${id}`).then((res) => res.data);
}

export function updateUser(id, { name, email }) {
  return api.put(`/admin/users/${id}`, { name, email }).then((res) => res.data);
}

export function changeUserRole(id, role) {
  return api.patch(`/admin/users/${id}/role`, { role }).then((res) => res.data);
}

export function deactivateUser(id) {
  return api.delete(`/admin/users/${id}`).then((res) => res.data);
}

export function activateUser(id) {
  return api.patch(`/admin/users/${id}/activate`).then((res) => res.data);
}

export function sendPasswordReset(id) {
  return api.post(`/admin/users/${id}/send-password-reset`).then((res) => res.data);
}
