import api from "./api";

export function listProjects({ status, search, page = 0, size = 10 } = {}) {
  return api
    .get("/projects", { params: { status, search, page, size } })
    .then((res) => res.data);
}

export function getProject(projectId) {
  return api.get(`/projects/${projectId}`).then((res) => res.data);
}

export function listProjectMembers(projectId) {
  return api.get(`/projects/${projectId}/members`).then((res) => res.data);
}

// responseType: "blob" -- this is a CSV file, not JSON; letting axios parse it as text would mangle
// it through the default JSON transform path. Returns the raw Blob so the caller can hand it
// straight to a download link.
export function exportProjectsOverview() {
  return api.get("/projects/export", { responseType: "blob" }).then((res) => res.data);
}
