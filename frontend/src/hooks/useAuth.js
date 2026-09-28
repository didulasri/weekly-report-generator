import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { getCurrentUser, login, logout } from "../services/authApi";
import { queryKeys } from "../services/queryKeys";

// TODO: no pages/routes exist yet, so the actual landing paths aren't decided -- fill these in
// once the route structure exists. Kept as one function so useLogin only needs to change here.
// eslint-disable-next-line no-unused-vars -- role is the whole point of this stub, just unused yet
export function roleHomePath(role) {
  return "/";
}

// There is no AuthContext holding user state anywhere in this app -- this query IS the auth state.
// success = authenticated (data is the current user), error (401) = not authenticated, pending =
// unknown yet. retry: false so a 401 resolves to "not authenticated" immediately instead of
// retrying a request that will never succeed without a session.
export function useCurrentUser() {
  return useQuery({
    queryKey: queryKeys.auth.currentUser(),
    queryFn: getCurrentUser,
    retry: false,
  });
}

export function useLogin() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  return useMutation({
    mutationFn: ({ email, password }) => login(email, password),
    onSuccess: async (user) => {
      // The login response body IS the current-user shape, so seed the cache directly instead of
      // waiting on a redundant GET /api/auth/me -- invalidate too, so any refetch-on-mount still
      // gets a fresh server read rather than trusting this forever.
      queryClient.setQueryData(queryKeys.auth.currentUser(), user);
      await queryClient.invalidateQueries({ queryKey: queryKeys.auth.currentUser() });
      navigate(roleHomePath(user.role), { replace: true });
    },
  });
}

export function useLogout() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  return useMutation({
    mutationFn: logout,
    onSuccess: () => {
      // Full clear, not just invalidating the current-user key -- nothing fetched under an
      // authenticated session should linger in the cache for whoever logs in next on this device.
      queryClient.clear();
      navigate("/login", { replace: true });
    },
  });
}
