package com.campuserp.setup;

import com.campuserp.common.BusinessRuleException;
import com.campuserp.common.Finder;
import com.campuserp.common.Ref;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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

import java.time.LocalDate;
import java.util.List;

/** Departments, programmes, sections, academic years and holidays. */
@RestController
@RequestMapping("/api")
@Transactional
public class SetupController {

    // ---------- request / response shapes

    public record DepartmentRequest(@NotBlank @Size(max = 20) String code, @NotBlank @Size(max = 120) String name) {}

    public record DepartmentResponse(Long id, String code, String name) {
        static DepartmentResponse from(Department d) {
            return new DepartmentResponse(d.getId(), d.getCode(), d.getName());
        }
    }

    public record ProgrammeRequest(@NotBlank @Size(max = 30) String code, @NotBlank @Size(max = 150) String name,
                                   @NotNull Long departmentId, @Min(1) @Max(12) int semesters) {}

    public record ProgrammeResponse(Long id, String code, String name, Ref department, int semesters) {
        static ProgrammeResponse from(Programme p) {
            return new ProgrammeResponse(p.getId(), p.getCode(), p.getName(), p.getDepartment().ref(), p.getSemesters());
        }
    }

    public record SectionRequest(@NotBlank @Size(max = 30) String code, @NotNull Long programmeId) {}

    public record SectionResponse(Long id, String code, Ref programme) {
        static SectionResponse from(Section s) {
            return new SectionResponse(s.getId(), s.getCode(), s.getProgramme().ref());
        }
    }

    public record AcademicYearRequest(
            @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}", message = "use the form 2026-27") String name,
            @NotNull LocalDate oddSemStart, @NotNull LocalDate oddSemEnd,
            @NotNull LocalDate evenSemStart, @NotNull LocalDate evenSemEnd) {}

    public record AcademicYearResponse(Long id, String name, LocalDate oddSemStart, LocalDate oddSemEnd,
                                       LocalDate evenSemStart, LocalDate evenSemEnd, boolean current) {}

    public record HolidayRequest(@NotNull LocalDate date, @NotBlank @Size(max = 120) String name) {}

    public record HolidayResponse(Long id, LocalDate date, String name) {
        static HolidayResponse from(Holiday h) {
            return new HolidayResponse(h.getId(), h.getDate(), h.getName());
        }
    }

    private final DepartmentRepository departments;
    private final ProgrammeRepository programmes;
    private final SectionRepository sections;
    private final AcademicYearRepository years;
    private final HolidayRepository holidays;
    private final AcademicCalendar calendar;

    public SetupController(DepartmentRepository departments, ProgrammeRepository programmes,
                           SectionRepository sections, AcademicYearRepository years,
                           HolidayRepository holidays, AcademicCalendar calendar) {
        this.departments = departments;
        this.programmes = programmes;
        this.sections = sections;
        this.years = years;
        this.holidays = holidays;
        this.calendar = calendar;
    }

    // ---------- departments

    @GetMapping("/departments")
    @Transactional(readOnly = true)
    public List<DepartmentResponse> departments() {
        return departments.findAllByOrderByCodeAsc().stream().map(DepartmentResponse::from).toList();
    }

    @PostMapping("/departments")
    @ResponseStatus(HttpStatus.CREATED)
    public DepartmentResponse createDepartment(@Valid @RequestBody DepartmentRequest req) {
        return DepartmentResponse.from(departments.save(apply(new Department(), req)));
    }

    @PutMapping("/departments/{id}")
    public DepartmentResponse updateDepartment(@PathVariable Long id, @Valid @RequestBody DepartmentRequest req) {
        return DepartmentResponse.from(apply(Finder.find(departments, id, "Department"), req));
    }

    @DeleteMapping("/departments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDepartment(@PathVariable Long id) {
        Finder.delete(departments, id, "Department");
    }

    private Department apply(Department d, DepartmentRequest req) {
        String code = req.code().trim().toUpperCase();
        departments.findByCode(code).filter(o -> !o.getId().equals(d.getId())).ifPresent(o -> {
            throw new BusinessRuleException("Department code " + code + " already exists");
        });
        d.setCode(code);
        d.setName(req.name().trim());
        return d;
    }

    // ---------- programmes

    @GetMapping("/programmes")
    @Transactional(readOnly = true)
    public List<ProgrammeResponse> programmes() {
        return programmes.findAllByOrderByCodeAsc().stream().map(ProgrammeResponse::from).toList();
    }

    @PostMapping("/programmes")
    @ResponseStatus(HttpStatus.CREATED)
    public ProgrammeResponse createProgramme(@Valid @RequestBody ProgrammeRequest req) {
        return ProgrammeResponse.from(programmes.save(apply(new Programme(), req)));
    }

    @PutMapping("/programmes/{id}")
    public ProgrammeResponse updateProgramme(@PathVariable Long id, @Valid @RequestBody ProgrammeRequest req) {
        return ProgrammeResponse.from(apply(Finder.find(programmes, id, "Programme"), req));
    }

    @DeleteMapping("/programmes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProgramme(@PathVariable Long id) {
        Finder.delete(programmes, id, "Programme");
    }

    private Programme apply(Programme p, ProgrammeRequest req) {
        String code = req.code().trim().toUpperCase();
        programmes.findByCode(code).filter(o -> !o.getId().equals(p.getId())).ifPresent(o -> {
            throw new BusinessRuleException("Programme code " + code + " already exists");
        });
        p.setCode(code);
        p.setName(req.name().trim());
        p.setDepartment(Finder.find(departments, req.departmentId(), "Department"));
        p.setSemesters(req.semesters());
        return p;
    }

    // ---------- sections

    @GetMapping("/sections")
    @Transactional(readOnly = true)
    public List<SectionResponse> sections() {
        return sections.findAllByOrderByCodeAsc().stream().map(SectionResponse::from).toList();
    }

    @PostMapping("/sections")
    @ResponseStatus(HttpStatus.CREATED)
    public SectionResponse createSection(@Valid @RequestBody SectionRequest req) {
        return SectionResponse.from(sections.save(apply(new Section(), req)));
    }

    @PutMapping("/sections/{id}")
    public SectionResponse updateSection(@PathVariable Long id, @Valid @RequestBody SectionRequest req) {
        return SectionResponse.from(apply(Finder.find(sections, id, "Section"), req));
    }

    @DeleteMapping("/sections/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSection(@PathVariable Long id) {
        Finder.delete(sections, id, "Section");
    }

    private Section apply(Section s, SectionRequest req) {
        String code = req.code().trim().toUpperCase();
        sections.findByCode(code).filter(o -> !o.getId().equals(s.getId())).ifPresent(o -> {
            throw new BusinessRuleException("Section " + code + " already exists");
        });
        s.setCode(code);
        s.setProgramme(Finder.find(programmes, req.programmeId(), "Programme"));
        return s;
    }

    // ---------- academic years

    @GetMapping("/academic-years")
    @Transactional(readOnly = true)
    public List<AcademicYearResponse> academicYears() {
        String current = calendar.currentYearName().orElse(null);
        return years.findAllByOrderByOddSemStartDesc().stream()
                .map(y -> new AcademicYearResponse(y.getId(), y.getName(), y.getOddSemStart(), y.getOddSemEnd(),
                        y.getEvenSemStart(), y.getEvenSemEnd(), y.getName().equals(current)))
                .toList();
    }

    @PostMapping("/academic-years")
    @ResponseStatus(HttpStatus.CREATED)
    public AcademicYearResponse createAcademicYear(@Valid @RequestBody AcademicYearRequest req) {
        AcademicYear y = years.save(apply(new AcademicYear(), req));
        return academicYears().stream().filter(r -> r.id().equals(y.getId())).findFirst().orElseThrow();
    }

    @PutMapping("/academic-years/{id}")
    public AcademicYearResponse updateAcademicYear(@PathVariable Long id, @Valid @RequestBody AcademicYearRequest req) {
        apply(Finder.find(years, id, "Academic year"), req);
        return academicYears().stream().filter(r -> r.id().equals(id)).findFirst().orElseThrow();
    }

    @DeleteMapping("/academic-years/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAcademicYear(@PathVariable Long id) {
        Finder.delete(years, id, "Academic year");
    }

    private AcademicYear apply(AcademicYear y, AcademicYearRequest req) {
        years.findByName(req.name()).filter(o -> !o.getId().equals(y.getId())).ifPresent(o -> {
            throw new BusinessRuleException("Academic year " + req.name() + " already exists");
        });
        if (!req.oddSemStart().isBefore(req.oddSemEnd())
                || !req.oddSemEnd().isBefore(req.evenSemStart())
                || !req.evenSemStart().isBefore(req.evenSemEnd())) {
            throw new BusinessRuleException("Dates must go in order: odd start, odd end, even start, even end");
        }
        y.setName(req.name());
        y.setOddSemStart(req.oddSemStart());
        y.setOddSemEnd(req.oddSemEnd());
        y.setEvenSemStart(req.evenSemStart());
        y.setEvenSemEnd(req.evenSemEnd());
        return y;
    }

    // ---------- holidays

    @GetMapping("/holidays")
    @Transactional(readOnly = true)
    public List<HolidayResponse> holidays() {
        return holidays.findAllByOrderByDateAsc().stream().map(HolidayResponse::from).toList();
    }

    @PostMapping("/holidays")
    @ResponseStatus(HttpStatus.CREATED)
    public HolidayResponse createHoliday(@Valid @RequestBody HolidayRequest req) {
        if (holidays.existsByDate(req.date())) {
            throw new BusinessRuleException("There is already a holiday on " + req.date());
        }
        Holiday h = new Holiday();
        h.setDate(req.date());
        h.setName(req.name().trim());
        return HolidayResponse.from(holidays.save(h));
    }

    @PutMapping("/holidays/{id}")
    public HolidayResponse updateHoliday(@PathVariable Long id, @Valid @RequestBody HolidayRequest req) {
        Holiday h = Finder.find(holidays, id, "Holiday");
        h.setDate(req.date());
        h.setName(req.name().trim());
        return HolidayResponse.from(h);
    }

    @DeleteMapping("/holidays/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHoliday(@PathVariable Long id) {
        Finder.delete(holidays, id, "Holiday");
    }
}
