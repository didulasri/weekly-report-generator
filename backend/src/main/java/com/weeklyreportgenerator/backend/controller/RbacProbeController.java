package com.weeklyreportgenerator.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Temporary, test-only endpoint to prove role-based access control works end to end
 * before manager-scoped endpoints exist (see api-design.md section 10). Remove once
 * a real MANAGER-only endpoint is implemented.
 */
@RestController
@RequestMapping("/api/test")
public class RbacProbeController {

    @GetMapping("/manager-only")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> managerOnly() {
        return ResponseEntity.ok().build();
    }
}
