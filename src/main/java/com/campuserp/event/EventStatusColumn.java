package com.campuserp.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Hibernate creates campus_event.status as a database ENUM listing only the statuses
 * that existed at the time, and ddl-auto=update never changes it afterwards. Plain
 * text lets a new status (like POSTPONED) be saved on databases made before it.
 */
@Component
class EventStatusColumn implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EventStatusColumn.class);

    private final JdbcTemplate jdbc;

    EventStatusColumn(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbc.execute("ALTER TABLE campus_event MODIFY status VARCHAR(20) NOT NULL");
        } catch (DataAccessException e) {
            log.warn("Couldn't change campus_event.status to text: {}", e.getMessage());
        }
    }
}
