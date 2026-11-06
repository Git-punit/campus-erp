package com.campuserp.project;

import com.campuserp.budget.Budget;
import com.campuserp.budget.BudgetDtos.BudgetSummary;
import com.campuserp.budget.BudgetRepository;
import com.campuserp.budget.BudgetService;
import com.campuserp.common.BusinessRuleException;
import com.campuserp.common.Finder;
import com.campuserp.faculty.FacultyRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api")
@Transactional
public class ProjectController {

    public record MemberInput(@NotNull Long facultyId, @NotNull MemberRole role) {}

    public record ProjectRequest(@NotBlank @Size(max = 250) String title,
                                 @NotBlank @Size(max = 120) String fundingAgency,
                                 @NotNull @DecimalMin("0") BigDecimal sanctionedAmount,
                                 @NotNull LocalDate sanctionDate, LocalDate startDate, LocalDate endDate,
                                 @NotNull ProjectStatus status,
                                 @NotEmpty(message = "add the project team") List<@Valid MemberInput> team) {}

    public record MemberView(Long facultyId, String employeeId, String name, MemberRole role) {}

    /** utilized = approved expenses on the project's budget; balance = sanctioned - utilized. */
    public record ProjectResponse(Long id, String title, String fundingAgency, BigDecimal sanctionedAmount,
                                  LocalDate sanctionDate, LocalDate startDate, LocalDate endDate,
                                  ProjectStatus status, List<MemberView> team, Long budgetId,
                                  BigDecimal utilized, BigDecimal pending, BigDecimal balance, double percentUsed) {}

    private final ResearchProjectRepository projects;
    private final FacultyRepository faculty;
    private final BudgetRepository budgetRepo;
    private final BudgetService budgets;

    public ProjectController(ResearchProjectRepository projects, FacultyRepository faculty,
                             BudgetRepository budgetRepo, BudgetService budgets) {
        this.projects = projects;
        this.faculty = faculty;
        this.budgetRepo = budgetRepo;
        this.budgets = budgets;
    }

    @GetMapping("/projects")
    @Transactional(readOnly = true)
    public List<ProjectResponse> list() {
        return projects.findAllByOrderBySanctionDateDesc().stream().map(this::view).toList();
    }

    @GetMapping("/projects/{id}")
    @Transactional(readOnly = true)
    public ProjectResponse get(@PathVariable Long id) {
        return view(Finder.find(projects, id, "Project"));
    }

    /** Every project the teacher is on, as PI or co-investigator. */
    @GetMapping("/faculty/{id}/projects")
    @Transactional(readOnly = true)
    public List<ProjectResponse> ofFaculty(@PathVariable Long id) {
        Finder.find(faculty, id, "Faculty");
        return projects.findByMember(id).stream().map(this::view).toList();
    }

    /** Creating a project also creates its budget, for the sanctioned amount. */
    @PostMapping("/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@Valid @RequestBody ProjectRequest req) {
        ResearchProject p = projects.save(apply(new ResearchProject(), req));
        budgets.syncProjectBudget(p);
        return view(p);
    }

    @PutMapping("/projects/{id}")
    public ProjectResponse update(@PathVariable Long id, @Valid @RequestBody ProjectRequest req) {
        ResearchProject p = apply(Finder.find(projects, id, "Project"), req);
        projects.flush();
        budgets.syncProjectBudget(p);
        return view(p);
    }

    @DeleteMapping("/projects/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        ResearchProject p = Finder.find(projects, id, "Project");
        budgetRepo.findByProjectId(id).ifPresent(budgets::removeIfUnused);
        projects.delete(p);
    }

    private ResearchProject apply(ResearchProject p, ProjectRequest req) {
        if (req.startDate() != null && req.endDate() != null && req.endDate().isBefore(req.startDate())) {
            throw new BusinessRuleException("The end date can't be before the start date");
        }
        long pis = req.team().stream().filter(m -> m.role() == MemberRole.PI).count();
        if (pis != 1) {
            throw new BusinessRuleException("A project needs exactly one Principal Investigator (PI)");
        }
        Set<Long> seen = new HashSet<>();
        for (MemberInput m : req.team()) {
            if (!seen.add(m.facultyId())) {
                throw new BusinessRuleException("The same teacher is in the team twice");
            }
        }
        p.setTitle(req.title().trim());
        p.setFundingAgency(req.fundingAgency().trim());
        p.setSanctionedAmount(req.sanctionedAmount());
        p.setSanctionDate(req.sanctionDate());
        p.setStartDate(req.startDate());
        p.setEndDate(req.endDate());
        p.setStatus(req.status());
        p.getTeam().clear();
        for (MemberInput m : req.team()) {
            p.getTeam().add(new ProjectMember(p, Finder.find(faculty, m.facultyId(), "Faculty"), m.role()));
        }
        return p;
    }

    private ProjectResponse view(ResearchProject p) {
        List<MemberView> team = p.getTeam().stream()
                .sorted(Comparator.comparing(ProjectMember::getRole))
                .map(m -> new MemberView(m.getFaculty().getId(), m.getFaculty().getEmployeeId(),
                        m.getFaculty().getName(), m.getRole()))
                .toList();
        Budget b = p.getId() == null ? null : budgetRepo.findByProjectId(p.getId()).orElse(null);
        BudgetSummary s = b == null ? null : budgets.summarise(List.of(b)).getFirst();
        BigDecimal used = s == null ? BigDecimal.ZERO : s.utilized();
        BigDecimal pending = s == null ? BigDecimal.ZERO : s.pending();
        return new ProjectResponse(p.getId(), p.getTitle(), p.getFundingAgency(), p.getSanctionedAmount(),
                p.getSanctionDate(), p.getStartDate(), p.getEndDate(), p.getStatus(), team,
                b == null ? null : b.getId(), used, pending, p.getSanctionedAmount().subtract(used),
                s == null ? 0 : s.percentUsed());
    }
}
