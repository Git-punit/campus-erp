package com.campuserp.course;

import com.campuserp.common.BusinessRuleException;
import com.campuserp.common.Finder;
import com.campuserp.common.Ref;
import com.campuserp.setup.DepartmentRepository;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@Transactional
public class CourseController {

    public record CourseRequest(@NotBlank @Size(max = 20) String code, @NotBlank @Size(max = 150) String name,
                                @Min(0) @Max(30) int credits, @NotNull Long departmentId) {}

    public record CourseResponse(Long id, String code, String name, int credits, Ref department) {
        static CourseResponse from(Course c) {
            return new CourseResponse(c.getId(), c.getCode(), c.getName(), c.getCredits(), c.getDepartment().ref());
        }
    }

    private final CourseRepository courses;
    private final DepartmentRepository departments;

    public CourseController(CourseRepository courses, DepartmentRepository departments) {
        this.courses = courses;
        this.departments = departments;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<CourseResponse> list() {
        return courses.findAllByOrderByCodeAsc().stream().map(CourseResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse create(@Valid @RequestBody CourseRequest req) {
        return CourseResponse.from(courses.save(apply(new Course(), req)));
    }

    @PutMapping("/{id}")
    public CourseResponse update(@PathVariable Long id, @Valid @RequestBody CourseRequest req) {
        return CourseResponse.from(apply(Finder.find(courses, id, "Course"), req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        Finder.delete(courses, id, "Course");
    }

    private Course apply(Course c, CourseRequest req) {
        String code = req.code().trim().toUpperCase();
        courses.findByCode(code).filter(o -> !o.getId().equals(c.getId())).ifPresent(o -> {
            throw new BusinessRuleException("Course code " + code + " already exists");
        });
        c.setCode(code);
        c.setName(req.name().trim());
        c.setCredits(req.credits());
        c.setDepartment(Finder.find(departments, req.departmentId(), "Department"));
        return c;
    }
}
