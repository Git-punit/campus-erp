package com.campuserp;

import com.campuserp.course.Course;
import com.campuserp.course.CourseRepository;
import com.campuserp.faculty.Faculty;
import com.campuserp.faculty.FacultyRepository;
import com.campuserp.setup.AcademicYear;
import com.campuserp.setup.AcademicYearRepository;
import com.campuserp.setup.Department;
import com.campuserp.setup.DepartmentRepository;
import com.campuserp.setup.Holiday;
import com.campuserp.setup.HolidayRepository;
import com.campuserp.setup.Programme;
import com.campuserp.setup.ProgrammeRepository;
import com.campuserp.setup.Section;
import com.campuserp.setup.SectionRepository;
import com.campuserp.student.Student;
import com.campuserp.student.StudentRepository;
import com.campuserp.student.StudentStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** Small helpers for building test records straight through the repositories. */
@Component
public class Fixtures {

    private final DepartmentRepository departments;
    private final ProgrammeRepository programmes;
    private final SectionRepository sections;
    private final AcademicYearRepository years;
    private final HolidayRepository holidays;
    private final FacultyRepository faculty;
    private final CourseRepository courses;
    private final StudentRepository students;

    public Fixtures(DepartmentRepository departments, ProgrammeRepository programmes, SectionRepository sections,
                    AcademicYearRepository years, HolidayRepository holidays, FacultyRepository faculty,
                    CourseRepository courses, StudentRepository students) {
        this.departments = departments;
        this.programmes = programmes;
        this.sections = sections;
        this.years = years;
        this.holidays = holidays;
        this.faculty = faculty;
        this.courses = courses;
        this.students = students;
    }

    public Department department(String code) {
        return departments.save(new Department(code, code + " department"));
    }

    public Section section(String code, Department d) {
        Programme p = new Programme();
        p.setCode("P-" + code);
        p.setName("Programme " + code);
        p.setDepartment(d);
        p.setSemesters(8);
        programmes.save(p);
        Section s = new Section();
        s.setCode(code);
        s.setProgramme(p);
        return sections.save(s);
    }

    /** 2026-27: odd semester 20 Jul - 4 Dec 2026, even 4 Jan - 14 May 2027. */
    public AcademicYear year2026() {
        return year("2026-27", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 12, 4),
                LocalDate.of(2027, 1, 4), LocalDate.of(2027, 5, 14));
    }

    public AcademicYear year(String name, LocalDate oddStart, LocalDate oddEnd, LocalDate evenStart, LocalDate evenEnd) {
        AcademicYear y = new AcademicYear();
        y.setName(name);
        y.setOddSemStart(oddStart);
        y.setOddSemEnd(oddEnd);
        y.setEvenSemStart(evenStart);
        y.setEvenSemEnd(evenEnd);
        return years.save(y);
    }

    public Holiday holiday(LocalDate date, String name) {
        Holiday h = new Holiday();
        h.setDate(date);
        h.setName(name);
        return holidays.save(h);
    }

    public Faculty teacher(String empId, String name, Department d) {
        Faculty f = new Faculty();
        f.setEmployeeId(empId);
        f.setName(name);
        f.setDepartment(d);
        return faculty.save(f);
    }

    public Course course(String code, Department d) {
        Course c = new Course();
        c.setCode(code);
        c.setName("Course " + code);
        c.setCredits(4);
        c.setDepartment(d);
        return courses.save(c);
    }

    public Student student(String roll, Section s, int semester, StudentStatus status) {
        Student st = new Student();
        st.setRollNo(roll);
        st.setName("Student " + roll);
        st.setSection(s);
        st.setProgramme(s.getProgramme());
        st.setCurrentSemester(semester);
        st.setStatus(status);
        return students.save(st);
    }
}
