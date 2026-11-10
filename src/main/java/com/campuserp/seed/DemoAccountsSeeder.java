package com.campuserp.seed;

import com.campuserp.user.Role;
import com.campuserp.user.User;
import com.campuserp.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates admin / staff / viewer when the user table is empty, so there is
 * always a way to log in on a fresh database. Passwords come from
 * app.seed.*-password (ADMIN_PASSWORD, STAFF_PASSWORD, VIEWER_PASSWORD).
 */
@Component
@Order(1)
public class DemoAccountsSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoAccountsSeeder.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String adminPassword;
    private final String staffPassword;
    private final String viewerPassword;

    public DemoAccountsSeeder(UserRepository users, PasswordEncoder encoder,
                              @Value("${app.seed.admin-password}") String adminPassword,
                              @Value("${app.seed.staff-password}") String staffPassword,
                              @Value("${app.seed.viewer-password}") String viewerPassword) {
        this.users = users;
        this.encoder = encoder;
        this.adminPassword = adminPassword;
        this.staffPassword = staffPassword;
        this.viewerPassword = viewerPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        requirePassword("ADMIN_PASSWORD", adminPassword);
        requirePassword("STAFF_PASSWORD", staffPassword);
        requirePassword("VIEWER_PASSWORD", viewerPassword);
        users.save(new User("admin", encoder.encode(adminPassword), "Administrator", Role.ADMIN));
        users.save(new User("staff", encoder.encode(staffPassword), "Office Staff", Role.STAFF));
        users.save(new User("viewer", encoder.encode(viewerPassword), "Read Only", Role.VIEWER));
        log.info("Empty database: created the admin, staff and viewer accounts");
    }

    /** No built-in passwords outside the dev profile: the first start needs real ones. */
    private static void requirePassword(String variable, String value) {
        if (value == null || value.length() < 6) {
            throw new IllegalStateException("The database has no users yet, so " + variable
                    + " must be set (6+ characters) to create the first accounts");
        }
    }
}
