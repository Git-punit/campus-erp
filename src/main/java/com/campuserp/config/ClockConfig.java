package com.campuserp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/** One clock for the whole app, so tests can pin "today". */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock(@Value("${app.time-zone:Asia/Kolkata}") String zone) {
        // the campus time zone, not the server's (cloud servers usually run in UTC)
        return Clock.system(ZoneId.of(zone));
    }
}
