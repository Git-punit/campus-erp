package com.campuserp.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static com.campuserp.config.DevDefaultsGuard.DEFAULT_JWT_SECRET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DevDefaultsGuardTest {

    static final Map<String, String> DEFAULT_PW = Map.of(
            "ADMIN_PASSWORD", "admin123", "STAFF_PASSWORD", "staff123", "VIEWER_PASSWORD", "viewer123");
    static final Map<String, String> OWN_PW = Map.of(
            "ADMIN_PASSWORD", "a-real-one", "STAFF_PASSWORD", "another", "VIEWER_PASSWORD", "third");

    @Test
    void flagsDefaultSecretAndPasswords() {
        assertThat(DevDefaultsGuard.problems(DEFAULT_JWT_SECRET, DEFAULT_PW, true))
                .containsExactly("JWT_SECRET", "ADMIN_PASSWORD", "STAFF_PASSWORD", "VIEWER_PASSWORD");
    }

    @Test
    void passwordsDontMatterOnceAccountsExist() {
        assertThat(DevDefaultsGuard.problems(DEFAULT_JWT_SECRET, DEFAULT_PW, false)).containsExactly("JWT_SECRET");
        assertThat(DevDefaultsGuard.problems("my-own-secret-that-is-long-enough-123456", DEFAULT_PW, false)).isEmpty();
    }

    @Test
    void ownValuesPass() {
        assertThat(DevDefaultsGuard.problems("my-own-secret-that-is-long-enough-123456", OWN_PW, true)).isEmpty();
    }

    @Test
    void knowsWhichDatabasesAreLocal() {
        assertThat(DevDefaultsGuard.isLocal("jdbc:mysql://localhost:3306/campus_erp")).isTrue();
        assertThat(DevDefaultsGuard.isLocal("jdbc:mysql://127.0.0.1/campus_erp")).isTrue();
        assertThat(DevDefaultsGuard.isLocal("jdbc:h2:mem:test")).isTrue();
        assertThat(DevDefaultsGuard.isLocal("jdbc:mysql://mysql.railway.internal:3306/railway")).isFalse();
        assertThat(DevDefaultsGuard.isLocal("jdbc:mysql://db.example.com/campus")).isFalse();
    }

    @Test
    void defaultSecretStopsAServerBeforeTheDatabase() {
        MockEnvironment server = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:mysql://db.example.com:3306/campus")
                .withProperty("app.jwt.secret", DEFAULT_JWT_SECRET);
        assertThatThrownBy(() -> JwtSecretCheck.check(server))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");

        assertThatCode(() -> JwtSecretCheck.check(server.withProperty("app.allow-dev-defaults", "true")))
                .doesNotThrowAnyException();

        MockEnvironment laptop = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/campus_erp")
                .withProperty("app.jwt.secret", DEFAULT_JWT_SECRET);
        assertThatCode(() -> JwtSecretCheck.check(laptop)).doesNotThrowAnyException();
    }
}
