package com.campuserp.faculty;

import com.campuserp.common.BusinessRuleException;
import com.campuserp.common.Finder;
import com.campuserp.common.Ref;
import com.campuserp.course.OfferingTeacherRepository;
import com.campuserp.project.ProjectMemberRepository;
import com.campuserp.setup.AcademicCalendar;
import com.campuserp.setup.DepartmentRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/faculty")
@Transactional
public class FacultyController {

    public record FacultyRequest(@NotBlank @Size(max = 20) String employeeId, @NotBlank @Size(max = 120) String name,
                                 @NotNull Long departmentId, @Size(max = 60) String designation,
                                 @Email @Size(max = 120) String email, @Size(max = 30) String phone,
                                 @NotNull FacultyStatus status) {}

    /** courseCount = offerings taught in the current academic year; projectCount = projects they're on. */
    public record FacultyResponse(Long id, String employeeId, String name, Ref department, String designation,
                                  String email, String phone, FacultyStatus status,
                                  long courseCount, long projectCount) {}

    private final FacultyRepository faculty;
    private final DepartmentRepository departments;
    private final OfferingTeacherRepository offeringTeachers;
    private final ProjectMemberRepository projectMembers;
    private final AcademicCalendar calendar;

    public FacultyController(FacultyRepository faculty, DepartmentRepository departments,
                             OfferingTeacherRepository offeringTeachers, ProjectMemberRepository projectMembers,
                             AcademicCalendar calendar) {
        this.faculty = faculty;
        this.departments = departments;
        this.offeringTeachers = offeringTeachers;
        this.projectMembers = projectMembers;
        this.calendar = calendar;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<FacultyResponse> list() {
        Map<Long, Long> courses = courseCounts();
        Map<Long, Long> projects = projectCounts();
        return faculty.findAllByOrderByNameAsc().stream()
                .map(f -> toResponse(f, courses.getOrDefault(f.getId(), 0L), projects.getOrDefault(f.getId(), 0L)))
                .toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public FacultyResponse get(@PathVariable Long id) {
        Faculty f = Finder.find(faculty, id, "Faculty");
        return toResponse(f, courseCounts().getOrDefault(id, 0L), projectCounts().getOrDefault(id, 0L));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FacultyResponse create(@Valid @RequestBody FacultyRequest req) {
        Faculty f = faculty.save(apply(new Faculty(), req));
        return toResponse(f, 0, 0);
    }

    @PutMapping("/{id}")
    public FacultyResponse update(@PathVariable Long id, @Valid @RequestBody FacultyRequest req) {
        apply(Finder.find(faculty, id, "Faculty"), req);
        return get(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        Finder.delete(faculty, id, "Faculty");
    }

    private Faculty apply(Faculty f, FacultyRequest req) {
        String emp = req.employeeId().trim().toUpperCase();
        faculty.findByEmployeeId(emp).filter(o -> !o.getId().equals(f.getId())).ifPresent(o -> {
            throw new BusinessRuleException("Employee ID " + emp + " is already used by " + o.getName());
        });
        f.setEmployeeId(emp);
        f.setName(req.name().trim());
        f.setDepartment(Finder.find(departments, req.departmentId(), "Department"));
        f.setDesignation(blankToNull(req.designation()));
        f.setEmail(blankToNull(req.email()));
        f.setPhone(blankToNull(req.phone()));
        f.setStatus(req.status());
        return f;
    }

    private Map<Long, Long> courseCounts() {
        return calendar.currentYearName()
                .map(offeringTeachers::countOfferingsPerFaculty)
                .map(FacultyController::toMap)
                .orElse(Map.of());
    }

    private Map<Long, Long> projectCounts() {
        return toMap(projectMembers.countProjectsPerFaculty());
    }

    private static Map<Long, Long> toMap(List<Object[]> rows) {
        return rows.stream().collect(java.util.stream.Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
    }

    private static FacultyResponse toResponse(Faculty f, long courses, long projects) {
        return new FacultyResponse(f.getId(), f.getEmployeeId(), f.getName(), f.getDepartment().ref(),
                f.getDesignation(), f.getEmail(), f.getPhone(), f.getStatus(), courses, projects);
    }

    static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
