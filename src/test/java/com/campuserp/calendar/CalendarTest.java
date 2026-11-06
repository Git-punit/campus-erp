package com.campuserp.calendar;

import com.campuserp.ApiTest;
import com.campuserp.TestClock;
import com.campuserp.course.Course;
import com.campuserp.faculty.Faculty;
import com.campuserp.setup.AcademicYear;
import com.campuserp.setup.Department;
import com.campuserp.setup.Section;
import com.campuserp.student.Student;
import com.campuserp.student.StudentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * One offering with a theory class every Wednesday 09:00 (Anil) and a lab every
 * Friday 14:00-16:00 (Bina). 25 Nov 2026 (a Wednesday) is a holiday.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestClock.class)
class CalendarTest extends ApiTest {

    Long offering;
    Student student;
    Faculty anil;
    Faculty bina;

    @BeforeEach
    void setUp() throws Exception {
        Department d = fx.department("ASH");
        Section sec = fx.section("CSE-A", d);
        AcademicYear year = fx.year2026();
        fx.holiday(LocalDate.of(2026, 11, 25), "Test holiday");
        anil = fx.teacher("T1", "Anil", d);
        bina = fx.teacher("T2", "Bina", d);
        Course c = fx.course("PH101", d);
        student = fx.student("R1", sec, 1, StudentStatus.ACTIVE);
        offering = id(postAs(staff(), "/api/offerings", """
                {"courseId":%d,"academicYearId":%d,"semester":1,"sectionId":%d,
                 "teachers":[{"facultyId":%d,"role":"THEORY"},{"facultyId":%d,"role":"LAB"}],
                 "slots":[{"day":"WEDNESDAY","startTime":"09:00","endTime":"10:00","room":"Room 1","type":"THEORY"},
                          {"day":"FRIDAY","startTime":"14:00","endTime":"16:00","room":"Lab 1","type":"LAB"}]}"""
                .formatted(c.getId(), year.getId(), sec.getId(), anil.getId(), bina.getId())), "$.offering.id");
        postAs(staff(), "/api/students/" + student.getId() + "/courses", "{\"offeringId\":" + offering + "}")
                .andExpect(status().isCreated());
    }

    String studentCal(String from, String to) {
        return "/api/calendar/student/" + student.getId() + "?from=" + from + "&to=" + to;
    }

    @Test
    void weeklyClassesRepeatAndHolidaysAreSkipped() throws Exception {
        // Mon 16 Nov to Sun 29 Nov: Wed 18, Fri 20, (Wed 25 holiday), Fri 27
        getAs(viewer(), studentCal("2026-11-16", "2026-11-29"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[*].date", contains("2026-11-18", "2026-11-20", "2026-11-27")))
                .andExpect(jsonPath("$.entries[0].teachers", contains("Anil")))
                .andExpect(jsonPath("$.entries[1].teachers", contains("Bina")))
                .andExpect(jsonPath("$.holidays[0].date").value("2026-11-25"));
    }

    @Test
    void classesStopAtTheEndOfTheSemester() throws Exception {
        // semester ends Fri 4 Dec
        getAs(viewer(), studentCal("2026-11-30", "2026-12-13"))
                .andExpect(jsonPath("$.entries[*].date", contains("2026-12-02", "2026-12-04")));
    }

    @Test
    void cancelledAndExtraClassesShowOnTheirDayOnly() throws Exception {
        postAs(staff(), "/api/offerings/" + offering + "/exceptions",
                "{\"date\":\"2026-11-20\",\"type\":\"CANCELLED\",\"startTime\":\"14:00\",\"reason\":\"Lab repair\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.room").value("Lab 1"));
        postAs(staff(), "/api/offerings/" + offering + "/exceptions",
                "{\"date\":\"2026-11-28\",\"type\":\"EXTRA\",\"startTime\":\"10:00\",\"endTime\":\"11:00\",\"room\":\"Room 9\"}")
                .andExpect(status().isCreated());

        getAs(viewer(), studentCal("2026-11-16", "2026-11-29"))
                .andExpect(jsonPath("$.entries", hasSize(4)))
                .andExpect(jsonPath("$.entries[*].status", contains("CLASS", "CANCELLED", "CLASS", "EXTRA")))
                .andExpect(jsonPath("$.entries[1].note").value("Lab repair"))
                .andExpect(jsonPath("$.entries[3].room").value("Room 9"));
    }

    @Test
    void cantCancelAClassThatIsntThereOrIsOnAHoliday() throws Exception {
        String url = "/api/offerings/" + offering + "/exceptions";
        postAs(staff(), url, "{\"date\":\"2026-11-19\",\"type\":\"CANCELLED\",\"startTime\":\"09:00\"}")
                .andExpect(status().isConflict());
        postAs(staff(), url, "{\"date\":\"2026-11-25\",\"type\":\"CANCELLED\",\"startTime\":\"09:00\"}")
                .andExpect(status().isConflict());
        postAs(staff(), url, "{\"date\":\"2027-01-06\",\"type\":\"CANCELLED\",\"startTime\":\"09:00\"}")
                .andExpect(status().isConflict());
    }

    @Test
    void teachersOnlySeeTheClassesTheyTake() throws Exception {
        getAs(viewer(), "/api/calendar/faculty/" + anil.getId() + "?from=2026-11-16&to=2026-11-22")
                .andExpect(jsonPath("$.entries[*].date", contains("2026-11-18")));
        getAs(viewer(), "/api/calendar/faculty/" + bina.getId() + "?from=2026-11-16&to=2026-11-22")
                .andExpect(jsonPath("$.entries[*].date", contains("2026-11-20")));
    }
}
