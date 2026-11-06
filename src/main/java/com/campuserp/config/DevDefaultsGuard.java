package com.campuserp.config;

import com.campuserp.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The repo is public, so the default JWT secret and demo passwords in
 * application.properties are known to everyone. They're fine on your own
 * machine, but on a server they'd let anyone log in as admin or forge a token.
 *
 * If the database isn't local and any of those defaults are still in use, the
 * app refuses to start and says which environment variables to set. Set
 * ALLOW_DEV_DEFAULTS=true to skip this on purpose (e.g. a throwaway demo).
 */
@Component
public class DevDefaultsGuard {

    private static final Logger log = LoggerFactory.getLogger(DevDefaultsGuard.class);

    // Keep these in step with the defaults in application.properties
    static final String DEFAULT_JWT_SECRET = "dev-only-secret-do-not-use-in-production-0123456789";
    static final Map<String, String> DEFAULT_PASSWORDS = Map.of(
            "ADMIN_PASSWORD", "admin123",
            "STAFF_PASSWORD", "staff123",
            "VIEWER_PASSWORD", "viewer123");

    public DevDefaultsGuard(@Value("${spring.datasource.url}") String dbUrl,
                            @Value("${app.jwt.secret}") String jwtSecret,
                            @Value("${app.seed.admin-password}") String adminPassword,
                            @Value("${app.seed.staff-password}") String staffPassword,
                            @Value("${app.seed.viewer-password}") String viewerPassword,
                            @Value("${app.allow-dev-defaults:false}") boolean allow,
                            UserRepository users) {
        boolean strict = !isLocal(dbUrl) && !allow;
        // (JwtSecretCheck already stopped a server with the default secret before it got here)
        // passwords only matter while the accounts haven't been created yet
        boolean accountsNotCreated = users.count() == 0;
        List<String> problems = problems(jwtSecret,
                Map.of("ADMIN_PASSWORD", adminPassword, "STAFF_PASSWORD", staffPassword,
                        "VIEWER_PASSWORD", viewerPassword),
                accountsNotCreated);
        if (problems.isEmpty()) {
            return;
        }
        String list = String.join(", ", problems);
        if (!strict) {
            log.warn("Using the public development defaults for {}. Fine locally; set them before deploying.", list);
            return;
        }
        throw refuse(list);
    }

    static IllegalStateException refuse(String list) {
        return new IllegalStateException("Refusing to start: " + list + " still on the public defaults from "
                + "application.properties, and the database is not on this machine. Set those environment "
                + "variables (JWT_SECRET needs 32+ characters), or set ALLOW_DEV_DEFAULTS=true if you really mean it.");
    }

    /** Names of the settings that are still on their public default values. */
    static List<String> problems(String jwtSecret, Map<String, String> seedPasswords, boolean accountsNotCreated) {
        List<String> out = new ArrayList<>();
        if (DEFAULT_JWT_SECRET.equals(jwtSecret)) {
            out.add("JWT_SECRET");
        }
        if (accountsNotCreated) {
            DEFAULT_PASSWORDS.entrySet().stream()
                    .filter(e -> e.getValue().equals(seedPasswords.get(e.getKey())))
                    .map(Map.Entry::getKey)
                    .sorted()
                    .forEach(out::add);
        }
        return out;
    }

    /** localhost, 127.0.0.1, ::1 or an in-memory / file H2 database */
    static boolean isLocal(String dbUrl) {
        String u = dbUrl.toLowerCase();
        return u.startsWith("jdbc:h2:") || u.contains("//localhost") || u.contains("//127.0.0.1")
                || u.contains("//[::1]");
    }
}
