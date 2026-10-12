import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  acknowledgeReview,
  createReport,
  deleteReport,
  getReport,
  listMyReports,
  submitReport,
  updateReport,
} from "../services/reportsApi";
import { queryKeys } from "../services/queryKeys";

export function useMyReports(params) {
  return useQuery({
    queryKey: queryKeys.reports.mine(params),
    queryFn: () => listMyReports(params),
  });
}

export function useReport(id) {
  return useQuery({
    queryKey: queryKeys.reports.detail(id),
    queryFn: () => getReport(id),
    enabled: Boolean(id),
  });
}

export function useCreateReport() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createReport,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.reports.all });
    },
  });
}

export function useUpdateReport() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, payload }) => updateReport(id, payload),
    onSuccess: (_data, { id }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.reports.detail(id) });
      queryClient.invalidateQueries({ queryKey: queryKeys.reports.all });
    },
  });
}

export function useDeleteReport() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: deleteReport,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.reports.all });
    },
  });
}

export function useSubmitReport() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: submitReport,
    onSuccess: (_data, id) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.reports.detail(id) });
      queryClient.invalidateQueries({ queryKey: queryKeys.reports.all });
    },
  });
}

export function useAcknowledgeReview() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ reportId, reviewId }) => acknowledgeReview(reportId, reviewId),
    onSuccess: (_data, { reportId }) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.reports.detail(reportId) });
    },
  });
}
