package com.campuserp.student;

import com.campuserp.ApiTest;
import com.campuserp.TestClock;
import com.campuserp.setup.Department;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestClock.class)
class ImportTest extends ApiTest {

    @BeforeEach
    void setUp() {
        Department d = fx.department("CSE");
        fx.section("CSE-A", d);
    }

    org.springframework.test.web.servlet.ResultActions upload(String token, String url, String csv) throws Exception {
        return mvc.perform(post(url).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType("text/csv").content(csv));
    }

    @Test
    void goodFileImportsEveryRow() throws Exception {
        // column names in any case, a byte-order mark from Excel, quoted commas
        String csv = "﻿Roll No,Name,Section,Semester,Email\n"
                + "26CSE101,Asha Rao,cse-a,1,asha@example.edu\n"
                + "26CSE102,\"Verma, Neel\",CSE-A,1,\n";
        upload(staff(), "/api/students/import", csv)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(2));
        getAs(viewer(), "/api/students").andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].name").value("Verma, Neel"));
    }

    @Test
    void oneBadRowMeansNothingIsSaved() throws Exception {
        String csv = "roll_no,name,section,semester,email\n"
                + "26CSE101,Asha Rao,CSE-A,1,asha@example.edu\n"
                + "26CSE102,,CSE-Z,one,not-an-email\n"
                + "26CSE101,Copy of Asha,CSE-A,1,\n";
        upload(staff(), "/api/students/import", csv)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Nothing was imported")))
                .andExpect(jsonPath("$.message", containsString("Row 2: unknown section 'CSE-Z'")))
                .andExpect(jsonPath("$.message", containsString("Row 3: roll number 26CSE101 appears twice")));
        getAs(viewer(), "/api/students").andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void missingColumnsAndViewerAreRefused() throws Exception {
        upload(staff(), "/api/students/import", "roll_no,name\n1,x\n")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Missing columns: section, semester")));
        upload(viewer(), "/api/students/import", "roll_no,name,section,semester\n1,x,CSE-A,1\n")
                .andExpect(status().isForbidden());
    }

    @Test
    void facultyImportWorksTheSameWay() throws Exception {
        upload(staff(), "/api/faculty/import", "employee_id,name,department,status\nT9,Dr. New,CSE,on leave\n")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(1));
        getAs(viewer(), "/api/faculty").andExpect(jsonPath("$[0].status").value("ON_LEAVE"));
        upload(staff(), "/api/faculty/import", "employee_id,name,department\nT9,Again,CSE\n")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already used")));
    }
}
