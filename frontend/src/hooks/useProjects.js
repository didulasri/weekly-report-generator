import { useMutation, useQueries, useQuery } from "@tanstack/react-query";
import { exportProjectsOverview, getProject, listProjectMembers, listProjects } from "../services/projectsApi";
import { queryKeys } from "../services/queryKeys";

export function useProjects(params) {
  return useQuery({
    queryKey: queryKeys.projects.list(params),
    queryFn: () => listProjects(params),
  });
}

export function useProject(projectId) {
  return useQuery({
    queryKey: queryKeys.projects.detail(projectId),
    queryFn: () => getProject(projectId),
    enabled: Boolean(projectId),
  });
}

export function useProjectMembers(projectId) {
  return useQuery({
    queryKey: queryKeys.projects.members(projectId),
    queryFn: () => listProjectMembers(projectId),
    enabled: Boolean(projectId),
  });
}

// One combined query per project id -- used where a screen needs every active project's roster at
// once (e.g. building a userId -> project-names map), rather than one screen-level request each.
export function useProjectsMembers(projectIds) {
  return useQueries({
    queries: (projectIds ?? []).map((id) => ({
      queryKey: queryKeys.projects.members(id),
      queryFn: () => listProjectMembers(id),
      enabled: Boolean(id),
    })),
  });
}

export function useExportProjectsOverview() {
  return useMutation({
    mutationFn: exportProjectsOverview,
  });
}
