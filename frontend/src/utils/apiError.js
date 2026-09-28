const FALLBACK_MESSAGE = "Something went wrong. Please try again.";
const NETWORK_ERROR_MESSAGE = "Can't reach the server. Check your connection and try again.";

// Extracts a single displayable message from any axios error, matching the backend's error shape
// (docs/api-design.md Section 10): { timestamp, status, error, message, path, errors? }.
export function getApiErrorMessage(error) {
  if (!error) {
    return FALLBACK_MESSAGE;
  }

  // Axios only sets error.response when the server actually replied; no response means the
  // request never completed (backend down, network dropped, CORS, timeout, ...).
  if (!error.response) {
    return NETWORK_ERROR_MESSAGE;
  }

  const message = error.response.data?.message;
  return typeof message === "string" && message.length > 0 ? message : FALLBACK_MESSAGE;
}

// ReportSubmissionValidationException carries `errors: string[]` alongside the single `message` --
// useful for rendering every field problem at once rather than just the first. Falls back to the
// single message so callers can use this even for errors that only ever carry one.
export function getApiErrorList(error) {
  const errors = error?.response?.data?.errors;
  if (Array.isArray(errors) && errors.length > 0) {
    return errors;
  }
  return [getApiErrorMessage(error)];
}

// Best-effort split of a single-field validation message into { field, message }, for a form that
// wants to call react-hook-form's setError(field, {...}) instead of only showing a banner.
// GlobalExceptionHandler#handleValidation builds its message as "fieldName: problem description"
// for the first failing field -- there's no structured field map in the response, so this is a
// heuristic on that one known convention, not a general contract. Returns null when the message
// doesn't look like that shape, so callers must always have a banner fallback too.
export function getApiFieldError(error) {
  const message = error?.response?.data?.message;
  if (typeof message !== "string") {
    return null;
  }

  const separatorIndex = message.indexOf(":");
  if (separatorIndex <= 0) {
    return null;
  }

  const field = message.slice(0, separatorIndex).trim();
  const detail = message.slice(separatorIndex + 1).trim();
  if (!field || !detail || /\s/.test(field)) {
    return null;
  }

  return { field, message: detail };
}
