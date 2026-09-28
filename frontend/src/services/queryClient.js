import { QueryClient } from "@tanstack/react-query";

// A 4xx is never worth retrying automatically (wrong password, forbidden, not found -- retrying
// won't change the outcome); a network hiccup or 5xx gets a few attempts. Kept as a named function
// so both the default here and any query that needs to override it read the same logic.
function shouldRetry(failureCount, error) {
  const status = error?.response?.status;
  if (status >= 400 && status < 500) {
    return false;
  }
  return failureCount < 3;
}

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: shouldRetry,
      refetchOnWindowFocus: false,
    },
  },
});
