import axios from "axios";

// The single axios instance every service module in this app must go through -- no component or
// hook should import axios directly. Auth is cookie-based: withCredentials sends the httpOnly
// access_token/refresh_token cookies on same-origin requests (proxied in dev via vite.config.js),
// and xsrfCookieName/xsrfHeaderName make axios read the non-httpOnly XSRF-TOKEN cookie itself and
// echo it back as X-XSRF-TOKEN on state-changing requests -- no hand-written CSRF interceptor
// needed, axios's built-in double-submit support does exactly what the backend expects.
const api = axios.create({
  baseURL: "/api",
  withCredentials: true,
  xsrfCookieName: "XSRF-TOKEN",
  xsrfHeaderName: "X-XSRF-TOKEN",
});

// Set once from main.jsx after the QueryClient is created, so a failed refresh can wipe every
// cached query (nothing authenticated should survive in memory once the session is dead).
let queryClient = null;
export function setQueryClient(client) {
  queryClient = client;
}

// Requests that must never trigger a refresh-and-retry: /auth/refresh itself (its own 401 means
// refresh failed, not "go refresh"), /auth/login (a wrong password is a real 401, not an expired
// session), and /auth/me (the query useCurrentUser uses to detect "not logged in" -- refreshing
// off its 401 would turn "logged out" into a network call loop on every render).
const NEVER_REFRESH = ["/auth/refresh", "/auth/login", "/auth/me"];

function isExemptFromRefresh(url) {
  return NEVER_REFRESH.some((path) => url?.includes(path));
}

// Single-flight refresh: every 401 that arrives while a refresh is already in progress just awaits
// the SAME promise instead of firing its own POST /auth/refresh. This isn't an optimization -- it's
// required for correctness. The backend rotates refresh tokens and treats a reused (already-
// rotated) one as theft, revoking the entire token family. If N requests 401 at once and each
// fires its own refresh call, only the first one presents a still-valid refresh token; calls 2..N
// present a token that's already been rotated out from under them, which reads as theft and gets
// everyone logged out. Holding one shared promise means every concurrent 401 waits for the one
// real refresh and then retries with the new cookies, instead of racing each other into a lockout.
let refreshPromise = null;

function redirectToLogin() {
  queryClient?.clear();
  if (window.location.pathname !== "/login") {
    window.location.href = "/login";
  }
}

const CSRF_HEADER = "X-XSRF-TOKEN";

// Dev-only visibility into whether axios actually attached the CSRF header -- logged from the
// RESPONSE side (response.config / error.config), not a request interceptor. axios attaches
// X-XSRF-TOKEN itself inside resolveConfig(), which runs in the adapter at send time, AFTER every
// registered request interceptor has already run -- a request interceptor would only ever observe
// the header as absent (it hasn't been added yet), which would be a false negative, not a real
// check. response.config/error.config is the fully-resolved config axios actually sent, so it's
// the only point in the pipeline where "was it attached" is answerable. Logs presence only, never
// the token value. Gated behind import.meta.env.DEV so it never ships to production.
function logCsrfHeaderInDev(config) {
  if (!import.meta.env.DEV || !config) {
    return;
  }
  const present = config.headers?.has?.(CSRF_HEADER) ?? false;
  console.log(`[api] ${config.method?.toUpperCase()} ${config.url} — X-XSRF-TOKEN present: ${present}`);
}

api.interceptors.response.use(
  (response) => {
    logCsrfHeaderInDev(response.config);
    return response;
  },
  async (error) => {
    logCsrfHeaderInDev(error.config);

    const originalRequest = error.config;
    const status = error.response?.status;

    if (status !== 401 || !originalRequest || isExemptFromRefresh(originalRequest.url)) {
      return Promise.reject(error);
    }

    // Already retried once after a refresh and still 401 -- the new access token is genuinely no
    // good (or refresh itself is failing downstream). Don't loop; fail through to the caller.
    if (originalRequest._retriedAfterRefresh) {
      return Promise.reject(error);
    }
    originalRequest._retriedAfterRefresh = true;

    if (!refreshPromise) {
      refreshPromise = api
        .post("/auth/refresh")
        .catch((refreshError) => {
          redirectToLogin();
          throw refreshError;
        })
        .finally(() => {
          refreshPromise = null;
        });
    }

    await refreshPromise;
    return api(originalRequest);
  }
);

export default api;
