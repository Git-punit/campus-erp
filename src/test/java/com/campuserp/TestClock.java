package com.campuserp;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/** Pins "today" to Wednesday 18 Nov 2026, inside the 2026-27 odd semester of the sample data. */
@TestConfiguration
public class TestClock {

    public static final LocalDate TODAY = LocalDate.of(2026, 11, 18);

    @Bean
    @Primary
    Clock testClock() {
        ZoneId zone = ZoneId.systemDefault();
        return Clock.fixed(TODAY.atTime(10, 0).atZone(zone).toInstant(), zone);
    }
}
