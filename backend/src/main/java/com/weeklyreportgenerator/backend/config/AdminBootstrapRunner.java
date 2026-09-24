package com.weeklyreportgenerator.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreportgenerator.backend.entity.Role;
import com.weeklyreportgenerator.backend.entity.User;
import com.weeklyreportgenerator.backend.entity.enums.RoleName;
import com.weeklyreportgenerator.backend.repository.RoleRepository;
import com.weeklyreportgenerator.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Since there is no more public registration and admin accounts are created only by another
// admin (via invitation, see the invitations module), a brand new deployment with zero admins
// would otherwise be permanently locked out. This runs once at startup to escape that only when
// both env vars are explicitly set -- it is a deliberate one-time bootstrap step, not a standing
// account-creation path.
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${BOOTSTRAP_ADMIN_EMAIL:}")
    private String bootstrapAdminEmail;

    @Value("${BOOTSTRAP_ADMIN_PASSWORD:}")
    private String bootstrapAdminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.countByRoleNameAndActiveTrue(RoleName.ADMIN) > 0) {
            log.info("AdminBootstrapRunner: an active admin already exists, nothing to do.");
            return;
        }

        if (bootstrapAdminEmail.isBlank() || bootstrapAdminPassword.isBlank()) {
            log.info("AdminBootstrapRunner: no active admin found, and BOOTSTRAP_ADMIN_EMAIL / "
                    + "BOOTSTRAP_ADMIN_PASSWORD are not both set -- skipping bootstrap.");
            return;
        }

        if (userRepository.existsByEmailIgnoreCase(bootstrapAdminEmail)) {
            log.warn("AdminBootstrapRunner: BOOTSTRAP_ADMIN_EMAIL is already in use by an existing "
                    + "(inactive or non-admin) account -- skipping bootstrap. Deactivate/reassign it "
                    + "manually if a fresh bootstrap admin is needed.");
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException("ADMIN role is not seeded"));

        User admin = User.builder()
                .name("Bootstrap Admin")
                .email(bootstrapAdminEmail)
                .password(passwordEncoder.encode(bootstrapAdminPassword))
                .role(adminRole)
                .active(true)
                .build();
        userRepository.save(admin);

        // Never log the password -- only that bootstrap happened and for which account.
        log.info("AdminBootstrapRunner: created bootstrap admin account for {}", bootstrapAdminEmail);
    }
}
