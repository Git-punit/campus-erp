package com.campuserp.seed;

import com.campuserp.ApiTest;
import com.campuserp.TestClock;
import com.campuserp.course.CourseOffering;
import com.campuserp.course.CourseOfferingRepository;
import com.campuserp.course.OfferingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Loads the real sample data and checks the numbers the README and sample-data/README.md promise. */
@SpringBootTest(properties = "app.seed.demo-data=true")
@AutoConfigureMockMvc
@Import(TestClock.class)
class SampleDataTest extends ApiTest {

    @Autowired
    CourseOfferingRepository offerings;

    @Autowired
    OfferingService offeringService;

    @Test
    void teacherAHasFiveCoursesAndFiveProjects() throws Exception {
        getAs(viewer(), "/api/faculty")
                .andExpect(jsonPath("$", hasSize(23)))
                .andExpect(jsonPath("$[?(@.employeeId=='FAC001')].courseCount", contains(5)))
                .andExpect(jsonPath("$[?(@.employeeId=='FAC001')].projectCount", contains(5)));
    }

    @Test
    void projectMoneyMatchesTheReadme() throws Exception {
        getAs(viewer(), "/api/projects")
                .andExpect(jsonPath("$", hasSize(8)))
                .andExpect(jsonPath("$[?(@.title=~/Low-cost soil.*/)].utilized", contains(120000.0)))
                .andExpect(jsonPath("$[?(@.title=~/Low-cost soil.*/)].balance", contains(380000.0)))
                .andExpect(jsonPath("$[?(@.title=~/Edge computing.*/)].balance", contains(0.0)))
                .andExpect(jsonPath("$[?(@.title=~/Solar-assisted.*/)].utilized", contains(0)));
    }

    @Test
    void budgetsFlagOverspendAndRiskyPendingRequests() throws Exception {
        getAs(viewer(), "/api/budgets?financialYear=2026-27&type=DEPARTMENT")
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[?(@.name=~/Mechanical.*/)].pendingWouldOverspend", contains(true)));
        getAs(viewer(), "/api/budgets?type=PROJECT")
                .andExpect(jsonPath("$[?(@.overspent == true)].name", hasItem("Project: Low-power wireless sensor network for bridges")));
    }

    @Test
    void trainingClientRanAtALoss() throws Exception {
        String programs = getAs(viewer(), "/api/training-programs").andReturn().getResponse().getContentAsString();
        List<Integer> ids = com.jayway.jsonpath.JsonPath.read(programs, "$[?(@.name=='Concrete quality testing')].id");
        getAs(viewer(), "/api/training-programs/" + ids.getFirst() + "/clients")
                .andExpect(jsonPath("$[?(@.clientName=='Sunrise Infra Builders')].profit", contains(-7000.0)));
    }

    @Test
    void calendarSkipsDiwaliAndDashboardLoads() throws Exception {
        String students = getAs(viewer(), "/api/students?semester=1").andReturn().getResponse().getContentAsString();
        List<Integer> ids = com.jayway.jsonpath.JsonPath.read(students, "$[?(@.status=='ACTIVE')].id");
        String url = "/api/calendar/student/" + ids.getFirst();
        getAs(viewer(), url + "?from=2026-11-09&to=2026-11-10")
                .andExpect(jsonPath("$.entries", empty()))
                .andExpect(jsonPath("$.holidays", hasSize(2)));
        getAs(viewer(), url + "?from=2026-11-16&to=2026-11-20")
                .andExpect(jsonPath("$.entries", not(empty())));

        getAs(viewer(), "/api/dashboard")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.today").value("2026-11-18"))
                .andExpect(jsonPath("$.semester.academicYear").value("2026-27"))
                .andExpect(jsonPath("$.faculty").value(21))
                .andExpect(jsonPath("$.students").value(94))
                .andExpect(jsonPath("$.coursesThisSemester").value(46))
                .andExpect(jsonPath("$.todaysClasses", not(empty())))
                .andExpect(jsonPath("$.training.clientsReached").value(10))
                .andExpect(jsonPath("$.pendingApprovals.count").value(6));
    }

    @Test
    @Transactional
    void sampleTimetableHasNoClashes() {
        List<String> all = new ArrayList<>();
        for (CourseOffering o : offerings.findAll()) {
            if (o.getAcademicYear().getName().equals("2026-27")) {
                all.addAll(offeringService.clashes(o));
            }
        }
        assertThat(all).isEmpty();
    }
}
