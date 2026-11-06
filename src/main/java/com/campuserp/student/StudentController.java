package com.campuserp.student;

import com.campuserp.common.BusinessRuleException;
import com.campuserp.common.Finder;
import com.campuserp.common.Ref;
import com.campuserp.course.CourseOffering;
import com.campuserp.course.CourseOfferingRepository;
import com.campuserp.course.OfferingDtos.OfferingView;
import com.campuserp.course.OfferingService;
import com.campuserp.setup.AcademicYear;
import com.campuserp.setup.AcademicYearRepository;
import com.campuserp.setup.Section;
import com.campuserp.setup.SectionRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api")
@Transactional
public class StudentController {

    public record StudentRequest(@NotBlank @Size(max = 30) String rollNo, @NotBlank @Size(max = 120) String name,
                                 @NotNull Long sectionId, @Min(1) @Max(12) int currentSemester,
                                 @Email @Size(max = 120) String email, @Size(max = 30) String phone,
                                 @NotNull StudentStatus status) {}

    public record StudentResponse(Long id, String rollNo, String name, Ref programme, int currentSemester,
                                  Ref section, String email, String phone, StudentStatus status) {
        static StudentResponse from(Student s) {
            return new StudentResponse(s.getId(), s.getRollNo(), s.getName(), s.getProgramme().ref(),
                    s.getCurrentSemester(), s.getSection().ref(), s.getEmail(), s.getPhone(), s.getStatus());
        }
    }

    public record SectionEnrolRequest(@NotNull Long academicYearId, @Min(1) @Max(12) int semester,
                                      @NotNull Long sectionId) {}

    public record SectionEnrolResult(int students, int offerings, int added, int alreadyEnrolled) {}

    public record AddCourseRequest(@NotNull Long offeringId) {}

    private final StudentRepository students;
    private final SectionRepository sections;
    private final AcademicYearRepository years;
    private final CourseOfferingRepository offerings;
    private final EnrolmentRepository enrolments;
    private final OfferingService offeringService;

    public StudentController(StudentRepository students, SectionRepository sections, AcademicYearRepository years,
                             CourseOfferingRepository offerings, EnrolmentRepository enrolments,
                             OfferingService offeringService) {
        this.students = students;
        this.sections = sections;
        this.years = years;
        this.offerings = offerings;
        this.enrolments = enrolments;
        this.offeringService = offeringService;
    }

    // ---------- students

    @GetMapping("/students")
    @Transactional(readOnly = true)
    public List<StudentResponse> list(@RequestParam(required = false) Long sectionId,
                                      @RequestParam(required = false) Integer semester) {
        return students.search(sectionId, semester).stream().map(StudentResponse::from).toList();
    }

    @GetMapping("/students/{id}")
    @Transactional(readOnly = true)
    public StudentResponse get(@PathVariable Long id) {
        return StudentResponse.from(Finder.find(students, id, "Student"));
    }

    @PostMapping("/students")
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse create(@Valid @RequestBody StudentRequest req) {
        return StudentResponse.from(students.save(apply(new Student(), req)));
    }

    @PutMapping("/students/{id}")
    public StudentResponse update(@PathVariable Long id, @Valid @RequestBody StudentRequest req) {
        return StudentResponse.from(apply(Finder.find(students, id, "Student"), req));
    }

    @DeleteMapping("/students/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        Finder.find(students, id, "Student");
        enrolments.deleteByStudentId(id);
        Finder.delete(students, id, "Student");
    }

    private Student apply(Student s, StudentRequest req) {
        String roll = req.rollNo().trim().toUpperCase();
        students.findByRollNo(roll).filter(o -> !o.getId().equals(s.getId())).ifPresent(o -> {
            throw new BusinessRuleException("Roll number " + roll + " is already used by " + o.getName());
        });
        Section section = Finder.find(sections, req.sectionId(), "Section");
        if (req.currentSemester() > section.getProgramme().getSemesters()) {
            throw new BusinessRuleException(section.getProgramme().getCode() + " only has "
                    + section.getProgramme().getSemesters() + " semesters");
        }
        s.setRollNo(roll);
        s.setName(req.name().trim());
        s.setSection(section);
        s.setProgramme(section.getProgramme());
        s.setCurrentSemester(req.currentSemester());
        s.setEmail(blank(req.email()));
        s.setPhone(blank(req.phone()));
        s.setStatus(req.status());
        return s;
    }

    // ---------- a student's courses

    /** Courses the student is enrolled in, with teachers and weekly times. Leave out year for every year. */
    @GetMapping("/students/{id}/courses")
    @Transactional(readOnly = true)
    public List<OfferingView> courses(@PathVariable Long id, @RequestParam(required = false) String year) {
        Finder.find(students, id, "Student");
        return enrolments.findOfferingsOfStudent(id, year == null || year.isBlank() ? null : year).stream()
                .sorted(Comparator.comparing((CourseOffering o) -> o.getAcademicYear().getOddSemStart()).reversed()
                        .thenComparing(o -> o.getCourse().getCode()))
                .map(offeringService::view).toList();
    }

    @PostMapping("/students/{id}/courses")
    @ResponseStatus(HttpStatus.CREATED)
    public OfferingView addCourse(@PathVariable Long id, @Valid @RequestBody AddCourseRequest req) {
        Student s = Finder.find(students, id, "Student");
        CourseOffering o = Finder.find(offerings, req.offeringId(), "Course offering");
        if (s.getStatus() != StudentStatus.ACTIVE) {
            throw new BusinessRuleException(s.getName() + " is not an active student");
        }
        if (enrolments.existsByStudentIdAndOfferingId(id, o.getId())) {
            throw new BusinessRuleException(s.getName() + " is already enrolled in " + o.label());
        }
        enrolments.save(new Enrolment(s, o));
        return offeringService.view(o);
    }

    @DeleteMapping("/students/{id}/courses/{offeringId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeCourse(@PathVariable Long id, @PathVariable Long offeringId) {
        Enrolment e = enrolments.findByStudentIdAndOfferingId(id, offeringId)
                .orElseThrow(() -> new BusinessRuleException("The student isn't enrolled in that course"));
        enrolments.delete(e);
    }

    // ---------- whole section

    /**
     * Enrols every active student in the section and semester into all of that
     * section's offerings for the year. Safe to run again; existing enrolments are skipped.
     */
    @PostMapping("/enrolments/section")
    public SectionEnrolResult enrolSection(@Valid @RequestBody SectionEnrolRequest req) {
        AcademicYear year = Finder.find(years, req.academicYearId(), "Academic year");
        Section section = Finder.find(sections, req.sectionId(), "Section");
        List<Student> group = students.findBySectionIdAndCurrentSemesterAndStatus(section.getId(), req.semester(),
                StudentStatus.ACTIVE);
        List<CourseOffering> courses = offerings.findByAcademicYearNameAndSemesterAndSectionId(year.getName(),
                req.semester(), section.getId());
        if (courses.isEmpty()) {
            throw new BusinessRuleException("No courses are offered to Sem " + req.semester() + " "
                    + section.getCode() + " in " + year.getName() + " yet");
        }
        int added = 0;
        int skipped = 0;
        for (Student s : group) {
            for (CourseOffering o : courses) {
                if (enrolments.existsByStudentIdAndOfferingId(s.getId(), o.getId())) {
                    skipped++;
                } else {
                    enrolments.save(new Enrolment(s, o));
                    added++;
                }
            }
        }
        return new SectionEnrolResult(group.size(), courses.size(), added, skipped);
    }

    /** Students enrolled in one offering. */
    @GetMapping("/offerings/{id}/students")
    @Transactional(readOnly = true)
    public List<StudentResponse> studentsOf(@PathVariable Long id) {
        Finder.find(offerings, id, "Course offering");
        return enrolments.findStudentsOfOffering(id).stream().map(StudentResponse::from).toList();
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
