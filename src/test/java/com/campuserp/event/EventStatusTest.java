package com.campuserp.event;

import com.campuserp.ApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventStatusTest extends ApiTest {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    EventStatusColumn column;

    @Test
    void postponedSavesEvenOnADatabaseMadeBeforeIt() throws Exception {
        // what an older database looks like: an ENUM without POSTPONED
        jdbc.execute("ALTER TABLE campus_event MODIFY status "
                + "ENUM('PLANNED','ONGOING','COMPLETED','CANCELLED') NOT NULL");
        column.run(null);

        postAs(staff(), "/api/events", """
                {"name":"Tech Fest","type":"festival","startDate":"2026-12-01","endDate":"2026-12-03",
                 "status":"POSTPONED"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("POSTPONED"));
    }
}
