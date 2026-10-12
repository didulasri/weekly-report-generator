import { useQuery } from "@tanstack/react-query";
import { getActivityFeed, getStatusSummary, getSummary, getWorkload } from "../services/dashboardApi";
import { queryKeys } from "../services/queryKeys";

export function useDashboardSummary(params) {
  return useQuery({
    queryKey: queryKeys.dashboard.summary(params),
    queryFn: () => getSummary(params),
  });
}

export function useDashboardStatusSummary(params) {
  return useQuery({
    queryKey: queryKeys.dashboard.statusSummary(params),
    queryFn: () => getStatusSummary(params),
  });
}

export function useActivityFeed(params) {
  return useQuery({
    queryKey: queryKeys.dashboard.activityFeed(params),
    queryFn: () => getActivityFeed(params),
  });
}

export function useWorkload(params) {
  return useQuery({
    queryKey: queryKeys.dashboard.workload(params),
    queryFn: () => getWorkload(params),
  });
}
