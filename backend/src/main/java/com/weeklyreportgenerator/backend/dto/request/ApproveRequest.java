package com.weeklyreportgenerator.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
public class ApproveRequest {

    private String comment;
}
