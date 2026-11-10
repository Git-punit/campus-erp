package com.campuserp.auth;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Locks a username for a while after too many wrong passwords, so passwords
 * can't be guessed by trying fast. Counted per username, not per IP address.
 * shortcut: kept in this server's memory, so it resets on restart and isn't shared;
 * move it to the database if the app ever runs on more than one server.
 */
@Component
public class LoginAttempts {

    static final int MAX_FAILURES = 5;
    static final Duration LOCK = Duration.ofMinutes(15);

    private record Entry(int failures, Instant lockedUntil) {}

    private final Map<String, Entry> byUser = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttempts(Clock clock) {
        this.clock = clock;
    }

    /** Minutes left on the lock, or 0 if this username may try now. */
    public long minutesLocked(String username) {
        Entry e = byUser.get(key(username));
        if (e == null || e.lockedUntil() == null) {
            return 0;
        }
        long seconds = Duration.between(clock.instant(), e.lockedUntil()).getSeconds();
        return seconds <= 0 ? 0 : (seconds + 59) / 60;
    }

    public void failed(String username) {
        Instant now = clock.instant();
        if (byUser.size() > 10_000) {
            // many made-up usernames: forget the ones that aren't locked
            byUser.values().removeIf(e -> e.lockedUntil() == null || e.lockedUntil().isBefore(now));
        }
        byUser.compute(key(username), (k, e) -> {
            boolean lockOver = e != null && e.lockedUntil() != null && !e.lockedUntil().isAfter(now);
            int failures = (e == null || lockOver ? 0 : e.failures()) + 1;
            return new Entry(failures, failures >= MAX_FAILURES ? now.plus(LOCK) : null);
        });
    }

    public void succeeded(String username) {
        byUser.remove(key(username));
    }

    private static String key(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
