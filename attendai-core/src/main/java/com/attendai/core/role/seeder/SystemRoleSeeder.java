package com.attendai.core.role.seeder;

import com.attendai.core.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Verifies that system roles are present at startup.
 * The actual INSERT is handled by Flyway V6 migration; this runner logs
 * confirmation and can be extended to re-seed if needed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemRoleSeeder implements ApplicationRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(ApplicationArguments args) {
        long systemRoleCount = roleRepository.findAll().stream()
                .filter(r -> r.isSystem() && !r.isDeleted())
                .count();
        log.info("System roles verified at startup: {} system role(s) present", systemRoleCount);
    }
}
