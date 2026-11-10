package com.campuserp.auth;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptsTest {

    /** A clock the test can move forward. */
    static class MovingClock extends Clock {
        Instant now = Instant.parse("2026-11-18T04:30:00Z");

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @Test
    void locksAfterFiveFailuresAndUnlocksAfterFifteenMinutes() {
        MovingClock clock = new MovingClock();
        LoginAttempts attempts = new LoginAttempts(clock);

        for (int i = 0; i < 4; i++) {
            attempts.failed("Admin");
        }
        assertThat(attempts.minutesLocked("admin")).isZero();
        attempts.failed(" admin ");
        assertThat(attempts.minutesLocked("ADMIN")).isEqualTo(15);

        clock.now = clock.now.plus(Duration.ofMinutes(14));
        assertThat(attempts.minutesLocked("admin")).isEqualTo(1);
        clock.now = clock.now.plus(Duration.ofMinutes(1));
        assertThat(attempts.minutesLocked("admin")).isZero();

        // after the lock runs out, one more mistake doesn't lock again straight away
        attempts.failed("admin");
        assertThat(attempts.minutesLocked("admin")).isZero();
    }

    @Test
    void aSuccessfulLoginClearsTheCount() {
        LoginAttempts attempts = new LoginAttempts(new MovingClock());
        for (int i = 0; i < 4; i++) {
            attempts.failed("staff");
        }
        attempts.succeeded("staff");
        attempts.failed("staff");
        assertThat(attempts.minutesLocked("staff")).isZero();
    }
}
