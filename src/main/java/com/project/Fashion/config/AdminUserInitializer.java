package com.project.Fashion.config;

import com.project.Fashion.model.User;
import com.project.Fashion.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class AdminUserInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Define default admin credentials (consider moving to application.properties for configurability)
    private static final String ADMIN_EMAIL = "admin@fashion.com";
    private static final String ADMIN_PASSWORD = "Password123";
    private static final String ADMIN_FIRST_NAME = "Admin";
    private static final String ADMIN_LAST_NAME = "User";
    private static final String ADMIN_ROLE = "ADMIN";

    @Autowired
    public AdminUserInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional // Good practice for database operations
    public void run(String... args) throws Exception {
        // Check if any admin user already exists
        List<User> adminUsers = userRepository.findByRole(ADMIN_ROLE);

        if (adminUsers.isEmpty()) {
            // No admin user found, let's create one
            if (userRepository.findByEmail(ADMIN_EMAIL).isPresent()) {
                logger.warn("Default admin email {} already exists but no user has ADMIN role. " +
                        "This might indicate a misconfiguration. " +
                        "Not creating a new admin with this email.", ADMIN_EMAIL);
                // You might want to find this user and update their role to ADMIN,
                // or handle this scenario as an error.
                // For now, just log and don't create if email exists but role isn't ADMIN.
            } else {
                User adminUser = new User();
                adminUser.setEmail(ADMIN_EMAIL);
                adminUser.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
                adminUser.setFirstName(ADMIN_FIRST_NAME);
                adminUser.setLastName(ADMIN_LAST_NAME);
                adminUser.setRole(ADMIN_ROLE.toUpperCase()); // Ensure role is stored in uppercase

                userRepository.save(adminUser);
                logger.info("Default ADMIN user created successfully with email: {}", ADMIN_EMAIL);
                logger.warn("IMPORTANT: The default admin password is '{}'. Please change it immediately after first login for security reasons.", ADMIN_PASSWORD);
            }
        } else {
            logger.info("Admin user(s) already exist in the database. Default admin creation skipped.");
            adminUsers.forEach(admin -> logger.info("Found existing admin: {}", admin.getEmail()));
        }
    }
}
