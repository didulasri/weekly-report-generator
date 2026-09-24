package com.weeklyreportgenerator.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Deliberately has no "role" field -- the role always comes from the invitation itself, so even
// if a client sends one in the JSON body it is silently dropped during deserialization.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AcceptInvitationRequest {

    @NotBlank(message = "Token is required")
    private String token;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Password is required")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
            message = "Password must be at least 8 characters long and contain at least one letter and one digit")
    private String password;
}
