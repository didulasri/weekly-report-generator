// Key factory so query invalidation is never a hand-typed array someone can typo or drift out of
// sync. Each domain exposes an `all` root (for invalidating everything in that domain) plus more
// specific keys built off it.
export const queryKeys = {
  auth: {
    all: ["auth"],
    currentUser: () => [...queryKeys.auth.all, "currentUser"],
  },
};
