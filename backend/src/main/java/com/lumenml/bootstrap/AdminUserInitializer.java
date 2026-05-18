package com.lumenml.bootstrap;

import com.lumenml.domain.User;
import com.lumenml.domain.UserRole;
import com.lumenml.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!worker")
@RequiredArgsConstructor
public class AdminUserInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String email = "admin@lumenml.dev";
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        User admin = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Admin123!"))
                .role(UserRole.ADMIN)
                .enabled(true)
                .build();
        userRepository.save(admin);
        log.info("Seeded admin user {}", email);
    }
}
