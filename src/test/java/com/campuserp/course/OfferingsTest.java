package com.campuserp.course;

import com.campuserp.ApiTest;
import com.campuserp.TestClock;
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

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestClock.class)
class OfferingsTest extends ApiTest {

    Department dept;
    Section secA;
    Section secB;
    AcademicYear year;
    Faculty anil;
    Faculty bina;
    Course maths;
    Course physics;
    Course chem;

    @BeforeEach
    void setUp() {
        dept = fx.department("ASH");
        secA = fx.section("CSE-A", dept);
        secB = fx.section("ME-A", dept);
        year = fx.year2026();
        anil = fx.teacher("T1", "Anil Theory", dept);
        bina = fx.teacher("T2", "Bina Lab", dept);
        maths = fx.course("MA101", dept);
        physics = fx.course("PH101", dept);
        chem = fx.course("CH101", dept);
    }

    String body(Course c, Section s, int sem, String teachers, String slots) {
        return """
                {"courseId":%d,"academicYearId":%d,"semester":%d,"sectionId":%d,
                 "teachers":[%s],"slots":[%s]}""".formatted(c.getId(), year.getId(), sem, s.getId(), teachers, slots);
    }

    String teacher(Faculty f, String role) {
        return "{\"facultyId\":%d,\"role\":\"%s\"}".formatted(f.getId(), role);
    }

    static String slot(String day, String start, String end, String room, String type) {
        return "{\"day\":\"%s\",\"startTime\":\"%s\",\"endTime\":\"%s\",\"room\":\"%s\",\"type\":\"%s\"}"
                .formatted(day, start, end, room, type);
    }

    @Test
    void offeringWithTwoTeachersShowsUpForBoth() throws Exception {
        postAs(staff(), "/api/offerings", body(maths, secA, 1,
                teacher(anil, "THEORY") + "," + teacher(bina, "LAB"),
                slot("MONDAY", "09:00", "10:00", "Room 1", "THEORY") + ","
                        + slot("TUESDAY", "14:00", "16:00", "Lab 1", "LAB")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.warnings", empty()))
                .andExpect(jsonPath("$.offering.teachers", hasSize(2)))
                .andExpect(jsonPath("$.offering.slots", hasSize(2)));

        getAs(viewer(), "/api/faculty/" + anil.getId() + "/courses?year=2026-27")
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].course.code").value("MA101"));
        getAs(viewer(), "/api/faculty/" + bina.getId() + "/courses?year=2026-27")
                .andExpect(jsonPath("$", hasSize(1)));
        getAs(viewer(), "/api/faculty/" + anil.getId() + "/courses?year=2025-26")
                .andExpect(jsonPath("$", hasSize(0)));
        getAs(viewer(), "/api/faculty")
                .andExpect(jsonPath("$[?(@.employeeId=='T1')].courseCount", hasItem(1)));
    }

    @Test
    void clashesAreWarnedAboutButStillSaved() throws Exception {
        postAs(staff(), "/api/offerings", body(maths, secA, 1,
                teacher(anil, "THEORY") + "," + teacher(bina, "LAB"),
                slot("MONDAY", "09:00", "10:00", "Room 1", "THEORY") + ","
                        + slot("TUESDAY", "14:00", "16:00", "Lab 1", "LAB")))
                .andExpect(status().isCreated());

        // same teacher and room, different section: teacher + room clash
        postAs(staff(), "/api/offerings", body(physics, secB, 3, teacher(anil, "THEORY"),
                slot("MONDAY", "09:30", "10:30", "Room 1", "THEORY")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.warnings", hasItem(containsString("Teacher Anil Theory is already teaching"))))
                .andExpect(jsonPath("$.warnings", hasItem(containsString("Room 1 is already booked"))));

        // same section and semester at the same time: section clash. Bina only takes the
        // lab of MA101, so teaching on Monday at 9 is not a teacher clash for her.
        postAs(staff(), "/api/offerings", body(chem, secA, 1, teacher(bina, "THEORY"),
                slot("MONDAY", "09:00", "10:00", "Room 2", "THEORY")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.warnings", hasItem(containsString("Sem 1 CSE-A already has a class"))))
                .andExpect(jsonPath("$.warnings", everyItem(not(containsString("Teacher")))));
    }

    @Test
    void evenSemestersDontClashWithOddOnes() throws Exception {
        postAs(staff(), "/api/offerings", body(maths, secA, 1, teacher(anil, "THEORY"),
                slot("MONDAY", "09:00", "10:00", "Room 1", "THEORY"))).andExpect(status().isCreated());
        postAs(staff(), "/api/offerings", body(physics, secA, 2, teacher(anil, "THEORY"),
                slot("MONDAY", "09:00", "10:00", "Room 1", "THEORY")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.warnings", empty()));
    }

    @Test
    void sameCourseTwiceForOneGroupIsRefused() throws Exception {
        postAs(staff(), "/api/offerings", body(maths, secA, 1, teacher(anil, "THEORY"), ""))
                .andExpect(status().isCreated());
        postAs(staff(), "/api/offerings", body(maths, secA, 1, teacher(bina, "THEORY"), ""))
                .andExpect(status().isConflict());
        // same course for another branch is a separate offering
        postAs(staff(), "/api/offerings", body(maths, secB, 1, teacher(anil, "THEORY"), ""))
                .andExpect(status().isCreated());
    }

    @Test
    void offeringNeedsATeacherAndViewerCantCreate() throws Exception {
        postAs(staff(), "/api/offerings", body(maths, secA, 1, "", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.teachers").exists());
        postAs(viewer(), "/api/offerings", body(maths, secA, 1, teacher(anil, "THEORY"), ""))
                .andExpect(status().isForbidden());
    }
}
