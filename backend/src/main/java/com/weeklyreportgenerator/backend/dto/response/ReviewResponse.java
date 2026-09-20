package com.weeklyreportgenerator.backend.dto.response;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {

    private Long id;
    private String action;
    private String comment;
    private Integer versionNumber;
    private Instant reviewedAt;
    private String reviewerName;
    private boolean acknowledged;
}
