import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  approveReport,
  getReportForReview,
  listTeamMembers,
  listTeamReports,
  requestChanges,
} from "../services/managerApi";
import { queryKeys } from "../services/queryKeys";

export function useTeamReports(params) {
  return useQuery({
    queryKey: queryKeys.managerReports.list(params),
    queryFn: () => listTeamReports(params),
  });
}

export function useTeamMembers(params) {
  return useQuery({
    queryKey: queryKeys.managerReports.teamMembers(params),
    queryFn: () => listTeamMembers(params),
  });
}

export function useManagerReportDetail(id) {
  return useQuery({
    queryKey: queryKeys.managerReports.detail(id),
    queryFn: () => getReportForReview(id),
    enabled: Boolean(id),
  });
}

export function useApproveReport() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, comment }) => approveReport(id, comment),
    onSuccess: (_data, { id }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.managerReports.detail(id) });
      queryClient.invalidateQueries({ queryKey: queryKeys.managerReports.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.dashboard.all });
    },
  });
}

export function useRequestChanges() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, comment }) => requestChanges(id, comment),
    onSuccess: (_data, { id }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.managerReports.detail(id) });
      queryClient.invalidateQueries({ queryKey: queryKeys.managerReports.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.dashboard.all });
    },
  });
}
