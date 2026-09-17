package com.weeklyreportgenerator.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockerResponse {

    private Long id;
    private String title;
    private String description;
    private String impact;
    private String status;
    private boolean keyIssue;
}
