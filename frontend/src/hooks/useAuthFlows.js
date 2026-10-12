import { useMutation, useQuery } from "@tanstack/react-query";
import {
  acceptInvitation,
  forgotPassword,
  resetPassword,
  validateInvitation,
} from "../services/authApi";
import { queryKeys } from "../services/queryKeys";

export function useForgotPassword() {
  return useMutation({
    mutationFn: (email) => forgotPassword(email),
  });
}

export function useResetPassword() {
  return useMutation({
    mutationFn: ({ token, newPassword }) => resetPassword(token, newPassword),
  });
}

// retry: false -- an invalid/expired token will never succeed on retry, so don't spend 3 attempts
// finding that out.
export function useValidateInvitation(token) {
  return useQuery({
    queryKey: queryKeys.invitations.validate(token),
    queryFn: () => validateInvitation(token),
    enabled: Boolean(token),
    retry: false,
  });
}

export function useAcceptInvitation() {
  return useMutation({
    mutationFn: (payload) => acceptInvitation(payload),
  });
}
