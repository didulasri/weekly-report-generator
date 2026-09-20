package com.weeklyreportgenerator.backend.dto.request;

import com.weeklyreportgenerator.backend.entity.enums.RoleName;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoleRequest {

    @NotNull(message = "Role is required")
    private RoleName role;
}
