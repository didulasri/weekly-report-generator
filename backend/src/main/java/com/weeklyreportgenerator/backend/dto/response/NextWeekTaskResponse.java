package com.weeklyreportgenerator.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NextWeekTaskResponse {

    private Long id;
    private String taskName;
    private String description;
    private String priority;
}
