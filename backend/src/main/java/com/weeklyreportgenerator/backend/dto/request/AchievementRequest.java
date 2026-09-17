package com.weeklyreportgenerator.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AchievementRequest {

    @NotBlank(message = "title is required")
    private String title;

    private String description;

    private boolean keyAchievement;
}
