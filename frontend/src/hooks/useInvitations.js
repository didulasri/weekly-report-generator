import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createInvitation, listInvitations, resendInvitation } from "../services/invitationsApi";
import { queryKeys } from "../services/queryKeys";

export function useInvitations(params) {
  return useQuery({
    queryKey: queryKeys.invitations.list(params),
    queryFn: () => listInvitations(params),
  });
}

export function useCreateInvitation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createInvitation,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.invitations.all });
    },
  });
}

export function useResendInvitation() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: resendInvitation,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.invitations.all });
    },
  });
}
