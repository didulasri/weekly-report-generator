// Matches AcceptInvitationRequest / ResetPasswordRequest's @Pattern on the backend exactly: at
// least 8 characters, containing at least one letter and one digit. This is for screens that
// CREATE or CHANGE a password (accept-invitation, reset-password) -- Login deliberately does NOT
// use this. LoginRequest on the backend only requires a non-blank password (@NotBlank, no
// @Pattern), since an existing account's password must still work even if it predates this rule
// or the policy changes later. Applying strength validation to the login form would reject a
// legitimate password the backend would happily accept.
export const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d).{8,}$/;

export const PASSWORD_RULE_MESSAGE =
  "Password must be at least 8 characters long and contain at least one letter and one digit";

export function passwordStrengthRules() {
  return {
    required: "Password is required",
    pattern: {
      value: PASSWORD_PATTERN,
      message: PASSWORD_RULE_MESSAGE,
    },
  };
}

// Matches LoginRequest/ForgotPasswordRequest's @Email + @NotBlank -- a plain shape check, not a
// full RFC 5322 validator (the backend is the real authority; this just catches obvious typos
// before a round trip).
export const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function emailRules() {
  return {
    required: "Email is required",
    pattern: {
      value: EMAIL_PATTERN,
      message: "Enter a valid email address",
    },
  };
}
