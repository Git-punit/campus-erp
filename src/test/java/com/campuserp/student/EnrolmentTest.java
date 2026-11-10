package com.campuserp.student;

import com.campuserp.ApiTest;
import com.campuserp.TestClock;
import com.campuserp.course.Course;
import com.campuserp.faculty.Faculty;
import com.campuserp.setup.AcademicYear;
import com.campuserp.setup.Department;
import com.campuserp.setup.Section;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestClock.class)
class EnrolmentTest extends ApiTest {

    Section sec;
    AcademicYear year;
    Student s1;
    Student s2;
    Student dropped;
    Student firstYear;
    Long maths;
    Long physics;
    Long firstYearCourse;

    @BeforeEach
    void setUp() throws Exception {
        Department d = fx.department("CSE");
        sec = fx.section("CSE-A", d);
        year = fx.year2026();
        Faculty t = fx.teacher("T1", "Teacher One", d);
        Course c1 = fx.course("CS201", d);
        Course c2 = fx.course("CS203", d);
        Course c3 = fx.course("CS101", d);
        s1 = fx.student("R1", sec, 3, StudentStatus.ACTIVE);
        s2 = fx.student("R2", sec, 3, StudentStatus.ACTIVE);
        dropped = fx.student("R3", sec, 3, StudentStatus.DROPPED);
        firstYear = fx.student("R4", sec, 1, StudentStatus.ACTIVE);
        maths = offering(c1, 3, t);
        physics = offering(c2, 3, t);
        firstYearCourse = offering(c3, 1, t);
    }

    Long offering(Course c, int sem, Faculty t) throws Exception {
        return id(postAs(staff(), "/api/offerings", """
                {"courseId":%d,"academicYearId":%d,"semester":%d,"sectionId":%d,
                 "teachers":[{"facultyId":%d,"role":"THEORY"}],"slots":[]}"""
                .formatted(c.getId(), year.getId(), sem, sec.getId(), t.getId()))
                .andExpect(status().isCreated()), "$.offering.id");
    }

    @Test
    void wholeSectionIsEnrolledOnceAndOnlyActiveStudents() throws Exception {
        String req = "{\"academicYearId\":%d,\"semester\":3,\"sectionId\":%d}".formatted(year.getId(), sec.getId());
        postAs(staff(), "/api/enrolments/section", req)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.students").value(2))
                .andExpect(jsonPath("$.offerings").value(2))
                .andExpect(jsonPath("$.added").value(4))
                .andExpect(jsonPath("$.alreadyEnrolled").value(0));

        // running it again changes nothing
        postAs(staff(), "/api/enrolments/section", req)
                .andExpect(jsonPath("$.added").value(0))
                .andExpect(jsonPath("$.alreadyEnrolled").value(4));

        getAs(viewer(), "/api/students/" + s1.getId() + "/courses?year=2026-27")
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].teachers[0].name").value("Teacher One"));
        getAs(viewer(), "/api/students/" + dropped.getId() + "/courses").andExpect(jsonPath("$", hasSize(0)));
        getAs(viewer(), "/api/students/" + firstYear.getId() + "/courses").andExpect(jsonPath("$", hasSize(0)));
        getAs(viewer(), "/api/offerings/" + maths + "/students").andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void sectionWithNoOfferingsIsRefused() throws Exception {
        postAs(staff(), "/api/enrolments/section",
                "{\"academicYearId\":%d,\"semester\":5,\"sectionId\":%d}".formatted(year.getId(), sec.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    void singleStudentCanAddAndRemoveACourse() throws Exception {
        String url = "/api/students/" + firstYear.getId() + "/courses";
        postAs(staff(), url, "{\"offeringId\":" + physics + "}").andExpect(status().isCreated());
        postAs(staff(), url, "{\"offeringId\":" + physics + "}").andExpect(status().isConflict());
        getAs(viewer(), url).andExpect(jsonPath("$", hasSize(1)));

        deleteAs(staff(), url + "/" + physics).andExpect(status().isNoContent());
        getAs(viewer(), url).andExpect(jsonPath("$", hasSize(0)));

        postAs(viewer(), url, "{\"offeringId\":" + physics + "}").andExpect(status().isForbidden());
    }

    @Test
    void inactiveStudentCantBeEnrolled() throws Exception {
        postAs(staff(), "/api/students/" + dropped.getId() + "/courses", "{\"offeringId\":" + maths + "}")
                .andExpect(status().isConflict());
    }

    @Test
    void offeringWithStudentsCantBeDeleted() throws Exception {
        postAs(staff(), "/api/students/" + s1.getId() + "/courses", "{\"offeringId\":" + maths + "}")
                .andExpect(status().isCreated());
        deleteAs(staff(), "/api/offerings/" + maths).andExpect(status().isConflict());
        deleteAs(staff(), "/api/offerings/" + firstYearCourse).andExpect(status().isNoContent());
    }

    @Test
    void offeringListShowsEachOfferingsOwnEnrolmentCount() throws Exception {
        postAs(staff(), "/api/students/" + s1.getId() + "/courses", "{\"offeringId\":" + maths + "}");
        postAs(staff(), "/api/students/" + s2.getId() + "/courses", "{\"offeringId\":" + maths + "}");
        postAs(staff(), "/api/students/" + s1.getId() + "/courses", "{\"offeringId\":" + physics + "}");

        // counted for the whole list in one query; each offering must still get its own number
        getAs(viewer(), "/api/offerings?sectionId=" + sec.getId())
                .andExpect(jsonPath("$[?(@.id == %d)].enrolled".formatted(maths)).value(2))
                .andExpect(jsonPath("$[?(@.id == %d)].enrolled".formatted(physics)).value(1))
                .andExpect(jsonPath("$[?(@.id == %d)].enrolled".formatted(firstYearCourse)).value(0));
    }
}
