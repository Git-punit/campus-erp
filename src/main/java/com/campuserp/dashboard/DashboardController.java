package com.campuserp.dashboard;

import com.campuserp.budget.BudgetDtos.BudgetSummary;
import com.campuserp.budget.BudgetRepository;
import com.campuserp.budget.BudgetService;
import com.campuserp.budget.BudgetType;
import com.campuserp.budget.ExpenseRepository;
import com.campuserp.budget.ExpenseStatus;
import com.campuserp.calendar.CalendarEntry;
import com.campuserp.calendar.CalendarService;
import com.campuserp.calendar.CalendarService.CalendarView;
import com.campuserp.course.CourseOfferingRepository;
import com.campuserp.event.EventRepository;
import com.campuserp.event.EventStatus;
import com.campuserp.faculty.FacultyRepository;
import com.campuserp.faculty.FacultyStatus;
import com.campuserp.project.ProjectStatus;
import com.campuserp.project.ResearchProjectRepository;
import com.campuserp.setup.AcademicCalendar;
import com.campuserp.student.StudentRepository;
import com.campuserp.student.StudentStatus;
import com.campuserp.training.Acceptance;
import com.campuserp.training.TrainingClientRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** One call that gives the dashboard everything it shows. */
@RestController
@RequestMapping("/api/dashboard")
@Transactional(readOnly = true)
public class DashboardController {

    public record Semester(String academicYear, String half) {}

    public record Money(BigDecimal allocated, BigDecimal utilized, double percentUsed) {}

    public record Projects(long ongoing, BigDecimal sanctioned, BigDecimal utilized, double percentUsed) {}

    public record Pipeline(long clientsReached, long proposals, long accepted, long conducted) {}

    public record Budgets(String financialYear, BigDecimal allocated, BigDecimal utilized, double percentUsed,
                          long overspent) {}

    public record Pending(long count, BigDecimal amount) {}

    public record UpcomingEvent(Long id, String name, String type, LocalDate startDate, LocalDate endDate,
                                String venue, EventStatus status) {}

    public record Dashboard(LocalDate today, Semester semester, String holidayToday,
                            long faculty, long students, long coursesThisSemester,
                            List<CalendarEntry> todaysClasses, Projects projects, Pipeline training,
                            Budgets budgets, Pending pendingApprovals, List<UpcomingEvent> upcomingEvents) {}

    private final AcademicCalendar calendar;
    private final CalendarService calendarService;
    private final FacultyRepository faculty;
    private final StudentRepository students;
    private final CourseOfferingRepository offerings;
    private final ResearchProjectRepository projects;
    private final BudgetRepository budgetRepo;
    private final BudgetService budgets;
    private final ExpenseRepository expenses;
    private final TrainingClientRepository clients;
    private final EventRepository events;

    public DashboardController(AcademicCalendar calendar, CalendarService calendarService, FacultyRepository faculty,
                               StudentRepository students, CourseOfferingRepository offerings,
                               ResearchProjectRepository projects, BudgetRepository budgetRepo, BudgetService budgets,
                               ExpenseRepository expenses, TrainingClientRepository clients, EventRepository events) {
        this.calendar = calendar;
        this.calendarService = calendarService;
        this.faculty = faculty;
        this.students = students;
        this.offerings = offerings;
        this.projects = projects;
        this.budgetRepo = budgetRepo;
        this.budgets = budgets;
        this.expenses = expenses;
        this.clients = clients;
        this.events = events;
    }

    @GetMapping
    public Dashboard get() {
        LocalDate today = calendar.today();

        var current = calendar.current();
        Semester semester = current.map(c -> new Semester(c.year().getName(), c.odd() ? "ODD" : "EVEN")).orElse(null);
        long courses = current.map(c -> offerings.countByAcademicYearIdAndSemesterIn(c.year().getId(),
                c.odd() ? List.of(1, 3, 5, 7, 9, 11) : List.of(2, 4, 6, 8, 10, 12))).orElse(0L);

        CalendarView todayView = calendarService.forEveryone(today, today);
        String holiday = todayView.holidays().isEmpty() ? null : todayView.holidays().getFirst().name();

        // projects: money across ongoing projects
        List<BudgetSummary> projectBudgets = budgets.summarise(
                budgetRepo.search(BudgetType.PROJECT, null, null).stream()
                        .filter(b -> b.getProject().getStatus() == ProjectStatus.ONGOING).toList());
        BigDecimal sanctioned = sum(projectBudgets, BudgetSummary::allocated);
        BigDecimal projectUsed = sum(projectBudgets, BudgetSummary::utilized);

        // budgets for this financial year
        String fy = calendar.currentFinancialYear();
        List<BudgetSummary> fyBudgets = budgets.summarise(budgetRepo.findByFinancialYear(fy));
        BigDecimal allocated = sum(fyBudgets, BudgetSummary::allocated);
        BigDecimal used = sum(fyBudgets, BudgetSummary::utilized);

        List<UpcomingEvent> upcoming = events
                .findByEndDateGreaterThanEqualAndStartDateLessThanEqualAndStatusInOrderByStartDateAsc(
                        today, today.plusDays(60), List.of(EventStatus.PLANNED, EventStatus.ONGOING))
                .stream()
                .map(e -> new UpcomingEvent(e.getId(), e.getName(), e.getType(), e.getStartDate(), e.getEndDate(),
                        e.getVenue(), e.getStatus()))
                .toList();

        return new Dashboard(today, semester, holiday,
                faculty.countByStatus(FacultyStatus.ACTIVE),
                students.countByStatus(StudentStatus.ACTIVE),
                courses,
                todayView.entries(),
                new Projects(projectBudgets.size(), sanctioned, projectUsed,
                        com.campuserp.common.Money.percent(projectUsed, sanctioned)),
                new Pipeline(clients.count(), clients.countByProposalSubmittedTrue(),
                        clients.countByAccepted(Acceptance.YES), clients.countByConductedTrue()),
                new Budgets(fy, allocated, used, com.campuserp.common.Money.percent(used, allocated),
                        fyBudgets.stream().filter(BudgetSummary::overspent).count()),
                new Pending(expenses.countByStatus(ExpenseStatus.PENDING), expenses.totalByStatus(ExpenseStatus.PENDING)),
                upcoming);
    }

    private static BigDecimal sum(List<BudgetSummary> list, java.util.function.Function<BudgetSummary, BigDecimal> f) {
        return list.stream().map(f).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
