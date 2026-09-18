package com.weeklyreportgenerator.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Deliberately the only field a manager can supply. There is no tasks/summary/status field here
// for a malicious payload to bind to -- ignoreUnknown makes that explicit rather than accidental.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestChangesRequest {

    @NotBlank(message = "comment is required")
    @Size(min = 10, message = "comment must be at least 10 characters")
    private String comment;
}
