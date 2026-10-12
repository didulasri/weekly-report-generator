import api from "./api";

// Thin wrappers over the admin invitation endpoints -- every one just unwraps response.data, no
// other logic. Hooks and components call these, never api.js directly.

export function createInvitation({ email, role }) {
  return api.post("/admin/invitations", { email, role }).then((res) => res.data);
}

export function listInvitations({ status, page = 0, size = 10 } = {}) {
  return api
    .get("/admin/invitations", { params: { status, page, size } })
    .then((res) => res.data);
}

export function resendInvitation(id) {
  return api.post(`/admin/invitations/${id}/resend`).then((res) => res.data);
}

export function revokeInvitation(id) {
  return api.delete(`/admin/invitations/${id}`).then((res) => res.data);
}
