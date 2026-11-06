package com.campuserp.config;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

/**
 * Runs as soon as the settings are read, before anything connects to the
 * database, so a server started with the public default JWT secret stops with
 * a clear message instead of a connection error. Registered in
 * META-INF/spring.factories. The seed passwords are checked later by
 * {@link DevDefaultsGuard}, because that needs the database.
 */
public class JwtSecretCheck implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        check(event.getEnvironment());
    }

    static void check(Environment env) {
        String dbUrl = env.getProperty("spring.datasource.url", "");
        String secret = env.getProperty("app.jwt.secret", "");
        boolean allow = env.getProperty("app.allow-dev-defaults", Boolean.class, false);
        if (DevDefaultsGuard.DEFAULT_JWT_SECRET.equals(secret) && !DevDefaultsGuard.isLocal(dbUrl) && !allow) {
            throw DevDefaultsGuard.refuse("JWT_SECRET");
        }
    }
}
