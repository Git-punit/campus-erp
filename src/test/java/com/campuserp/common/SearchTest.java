package com.campuserp.common;

import com.campuserp.ApiTest;
import com.campuserp.setup.Department;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SearchTest extends ApiTest {

    @Test
    void findsRecordsByNameOrCodeForAnyLoggedInUser() throws Exception {
        Department cse = fx.department("CSE");
        var rao = fx.teacher("F901", "Dr. Meera Rao", cse);
        fx.course("CS901", cse);

        // any part of the name, any case; a viewer can search too
        getAs(viewer(), "/api/search?q=meera").andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("Faculty"))
                .andExpect(jsonPath("$[0].title").value("Dr. Meera Rao"))
                .andExpect(jsonPath("$[0].link").value("/faculty/" + rao.getId()));
        // by code
        getAs(viewer(), "/api/search?q=cs901").andExpect(jsonPath("$[0].type").value("Course"));
        // one letter is too short; % is searched as a plain character, not "match everything"
        getAs(viewer(), "/api/search?q=m").andExpect(jsonPath("$", hasSize(0)));
        getAs(viewer(), "/api/search?q=%25%25").andExpect(jsonPath("$", hasSize(0)));
        // not logged in
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/search?q=meera"))
                .andExpect(status().isUnauthorized());
    }
}
