import api from "./api";

// Thin wrappers over the auth/invitation endpoints -- every one just unwraps response.data, no
// other logic. Hooks and components call these, never api.js directly.

export function login(email, password) {
  return api.post("/auth/login", { email, password }).then((res) => res.data);
}

export function logout() {
  return api.post("/auth/logout").then((res) => res.data);
}

export function getCurrentUser() {
  return api.get("/auth/me").then((res) => res.data);
}

export function getCsrfToken() {
  return api.get("/auth/csrf").then((res) => res.data);
}

export function forgotPassword(email) {
  return api.post("/auth/forgot-password", { email }).then((res) => res.data);
}

export function resetPassword(token, newPassword) {
  return api.post("/auth/reset-password", { token, newPassword }).then((res) => res.data);
}

export function validateInvitation(token) {
  return api.post("/invitations/validate", { token }).then((res) => res.data);
}

export function acceptInvitation(payload) {
  return api.post("/invitations/accept", payload).then((res) => res.data);
}
