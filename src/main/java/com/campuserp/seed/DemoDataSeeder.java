package com.campuserp.seed;

import com.campuserp.budget.Budget;
import com.campuserp.budget.BudgetRepository;
import com.campuserp.budget.BudgetService;
import com.campuserp.budget.BudgetType;
import com.campuserp.budget.Expense;
import com.campuserp.budget.ExpenseCategory;
import com.campuserp.budget.ExpenseRepository;
import com.campuserp.budget.ExpenseStatus;
import com.campuserp.course.ClassException;
import com.campuserp.course.ClassExceptionRepository;
import com.campuserp.course.Course;
import com.campuserp.course.CourseOffering;
import com.campuserp.course.CourseOfferingRepository;
import com.campuserp.course.CourseRepository;
import com.campuserp.course.ExceptionType;
import com.campuserp.course.OfferingTeacher;
import com.campuserp.course.TeachingRole;
import com.campuserp.course.TimetableSlot;
import com.campuserp.event.Event;
import com.campuserp.event.EventRepository;
import com.campuserp.event.EventStatus;
import com.campuserp.faculty.Faculty;
import com.campuserp.faculty.FacultyRepository;
import com.campuserp.faculty.FacultyStatus;
import com.campuserp.project.MemberRole;
import com.campuserp.project.ProjectMember;
import com.campuserp.project.ProjectStatus;
import com.campuserp.project.ResearchProject;
import com.campuserp.project.ResearchProjectRepository;
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
import com.campuserp.student.Enrolment;
import com.campuserp.student.EnrolmentRepository;
import com.campuserp.student.Student;
import com.campuserp.student.StudentRepository;
import com.campuserp.student.StudentStatus;
import com.campuserp.training.Acceptance;
import com.campuserp.training.AcceptanceChange;
import com.campuserp.training.AcceptanceChangeRepository;
import com.campuserp.training.TrainingClient;
import com.campuserp.training.TrainingClientRepository;
import com.campuserp.training.TrainingProgram;
import com.campuserp.training.TrainingProgramRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Loads src/main/resources/sample-data/*.csv into an empty database.
 * Turned off with SEED_DEMO_DATA=false. Rows point at each other by code;
 * this keeps code -> saved record maps while it loads.
 */
@Component
@Order(2)
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final boolean enabled;
    private final DepartmentRepository departments;
    private final ProgrammeRepository programmes;
    private final SectionRepository sections;
    private final AcademicYearRepository years;
    private final HolidayRepository holidays;
    private final FacultyRepository faculty;
    private final CourseRepository courses;
    private final CourseOfferingRepository offerings;
    private final StudentRepository students;
    private final EnrolmentRepository enrolments;
    private final ClassExceptionRepository classExceptions;
    private final ResearchProjectRepository projects;
    private final TrainingProgramRepository programs;
    private final TrainingClientRepository clients;
    private final AcceptanceChangeRepository acceptanceChanges;
    private final EventRepository events;
    private final BudgetRepository budgets;
    private final BudgetService budgetService;
    private final ExpenseRepository expenses;

    public DemoDataSeeder(@Value("${app.seed.demo-data:true}") boolean enabled,
                          DepartmentRepository departments, ProgrammeRepository programmes, SectionRepository sections,
                          AcademicYearRepository years, HolidayRepository holidays, FacultyRepository faculty,
                          CourseRepository courses, CourseOfferingRepository offerings, StudentRepository students,
                          EnrolmentRepository enrolments, ClassExceptionRepository classExceptions,
                          ResearchProjectRepository projects, TrainingProgramRepository programs,
                          TrainingClientRepository clients, AcceptanceChangeRepository acceptanceChanges,
                          EventRepository events, BudgetRepository budgets, BudgetService budgetService,
                          ExpenseRepository expenses) {
        this.enabled = enabled;
        this.departments = departments;
        this.programmes = programmes;
        this.sections = sections;
        this.years = years;
        this.holidays = holidays;
        this.faculty = faculty;
        this.courses = courses;
        this.offerings = offerings;
        this.students = students;
        this.enrolments = enrolments;
        this.classExceptions = classExceptions;
        this.projects = projects;
        this.programs = programs;
        this.clients = clients;
        this.acceptanceChanges = acceptanceChanges;
        this.events = events;
        this.budgets = budgets;
        this.budgetService = budgetService;
        this.expenses = expenses;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled || departments.count() > 0) {
            return;
        }
        Map<String, Department> dept = load("departments.csv", r -> {
            Department d = new Department(r.get("code"), r.get("name"));
            return Map.entry(d.getCode(), departments.save(d));
        });
        Map<String, Programme> prog = load("programmes.csv", r -> {
            Programme p = new Programme();
            p.setCode(r.get("code"));
            p.setName(r.get("name"));
            p.setDepartment(dept.get(r.get("department_code")));
            p.setSemesters(Integer.parseInt(r.get("semesters")));
            return Map.entry(p.getCode(), programmes.save(p));
        });
        Map<String, Section> sec = load("sections.csv", r -> {
            Section s = new Section();
            s.setCode(r.get("code"));
            s.setProgramme(prog.get(r.get("programme_code")));
            return Map.entry(s.getCode(), sections.save(s));
        });
        Map<String, AcademicYear> year = load("academic_years.csv", r -> {
            AcademicYear y = new AcademicYear();
            y.setName(r.get("name"));
            y.setOddSemStart(date(r.get("odd_sem_start")));
            y.setOddSemEnd(date(r.get("odd_sem_end")));
            y.setEvenSemStart(date(r.get("even_sem_start")));
            y.setEvenSemEnd(date(r.get("even_sem_end")));
            return Map.entry(y.getName(), years.save(y));
        });
        for (var r : Csv.read("holidays.csv")) {
            Holiday h = new Holiday();
            h.setDate(date(r.get("date")));
            h.setName(r.get("name"));
            holidays.save(h);
        }

        Map<String, Faculty> fac = load("faculty.csv", r -> {
            Faculty f = new Faculty();
            f.setEmployeeId(r.get("employee_id"));
            f.setName(r.get("name"));
            f.setDepartment(dept.get(r.get("department_code")));
            f.setDesignation(r.get("designation"));
            f.setEmail(r.get("email"));
            f.setPhone(r.get("phone"));
            f.setStatus(FacultyStatus.valueOf(r.get("status")));
            return Map.entry(f.getEmployeeId(), faculty.save(f));
        });
        Map<String, Course> course = load("courses.csv", r -> {
            Course c = new Course();
            c.setCode(r.get("code"));
            c.setName(r.get("name"));
            c.setCredits(Integer.parseInt(r.get("credits")));
            c.setDepartment(dept.get(r.get("department_code")));
            return Map.entry(c.getCode(), courses.save(c));
        });

        // offerings, their teachers and weekly slots
        Map<String, CourseOffering> off = load("course_offerings.csv", r -> {
            CourseOffering o = new CourseOffering();
            o.setCourse(course.get(r.get("course_code")));
            o.setAcademicYear(year.get(r.get("academic_year")));
            o.setSemester(Integer.parseInt(r.get("semester")));
            o.setSection(sec.get(r.get("section_code")));
            return Map.entry(r.get("offering_ref"), o);
        });
        for (var r : Csv.read("offering_teachers.csv")) {
            CourseOffering o = off.get(r.get("offering_ref"));
            o.getTeachers().add(new OfferingTeacher(o, fac.get(r.get("employee_id")), TeachingRole.valueOf(r.get("role"))));
        }
        for (var r : Csv.read("timetable_slots.csv")) {
            CourseOffering o = off.get(r.get("offering_ref"));
            TimetableSlot s = new TimetableSlot();
            s.setOffering(o);
            s.setDay(DayOfWeek.valueOf(r.get("day")));
            s.setStartTime(LocalTime.parse(r.get("start_time")));
            s.setEndTime(LocalTime.parse(r.get("end_time")));
            s.setRoom(r.get("room"));
            boolean hasLabTeacher = o.getTeachers().stream().anyMatch(t -> t.getRole() == TeachingRole.LAB);
            s.setType(hasLabTeacher && s.getRoom().contains("Lab") ? TeachingRole.LAB : TeachingRole.THEORY);
            o.getSlots().add(s);
        }
        off.values().forEach(offerings::save);

        Map<String, Student> stu = load("students.csv", r -> {
            Student s = new Student();
            s.setRollNo(r.get("roll_no"));
            s.setName(r.get("name"));
            s.setProgramme(prog.get(r.get("programme_code")));
            s.setCurrentSemester(Integer.parseInt(r.get("current_semester")));
            s.setSection(sec.get(r.get("section_code")));
            s.setEmail(r.get("email"));
            s.setPhone(r.get("phone"));
            s.setStatus(StudentStatus.valueOf(r.get("status")));
            return Map.entry(s.getRollNo(), students.save(s));
        });
        for (var r : Csv.read("enrolments.csv")) {
            enrolments.save(new Enrolment(stu.get(r.get("roll_no")), off.get(r.get("offering_ref"))));
        }
        for (var r : Csv.read("class_exceptions.csv")) {
            ClassException e = new ClassException();
            e.setOffering(off.get(r.get("offering_ref")));
            e.setDate(date(r.get("date")));
            e.setType(ExceptionType.valueOf(r.get("type")));
            e.setStartTime(LocalTime.parse(r.get("start_time")));
            e.setEndTime(LocalTime.parse(r.get("end_time")));
            e.setRoom(r.get("room"));
            e.setReason(r.get("reason"));
            classExceptions.save(e);
        }

        // money: department budgets first, then the records that bring their own budgets
        Map<String, Budget> budgetByRef = new HashMap<>();
        for (var r : Csv.read("department_budgets.csv")) {
            Budget b = new Budget();
            Department d = dept.get(r.get("department_code"));
            b.setType(BudgetType.DEPARTMENT);
            b.setDepartment(d);
            b.setFinancialYear(r.get("financial_year"));
            b.setName(d.getName() + " " + r.get("financial_year"));
            b.setAllocated(money(r.get("allocated_amount")));
            budgetByRef.put("DEPARTMENT:" + d.getCode() + ":" + r.get("financial_year"), budgets.save(b));
        }

        Map<String, ResearchProject> proj = load("research_projects.csv", r -> {
            ResearchProject p = new ResearchProject();
            p.setTitle(r.get("title"));
            p.setFundingAgency(r.get("funding_agency"));
            p.setSanctionedAmount(money(r.get("sanctioned_amount")));
            p.setSanctionDate(date(r.get("sanction_date")));
            p.setStartDate(date(r.get("start_date")));
            p.setEndDate(date(r.get("end_date")));
            p.setStatus(ProjectStatus.valueOf(r.get("status")));
            return Map.entry(r.get("code"), p);
        });
        for (var r : Csv.read("project_team.csv")) {
            ResearchProject p = proj.get(r.get("project_code"));
            p.getTeam().add(new ProjectMember(p, fac.get(r.get("employee_id")), MemberRole.valueOf(r.get("role"))));
        }
        proj.forEach((code, p) -> {
            projects.save(p);
            budgetByRef.put("PROJECT:" + code, budgetService.syncProjectBudget(p));
        });

        Map<String, TrainingProgram> prg = load("training_programs.csv", r -> {
            TrainingProgram p = new TrainingProgram();
            p.setName(r.get("name"));
            p.setDescription(r.get("description"));
            p.setDurationDays(Integer.parseInt(r.get("duration_days")));
            p.setCoordinator(fac.get(r.get("coordinator_employee_id")));
            return Map.entry(r.get("code"), programs.save(p));
        });
        Map<String, TrainingClient> cli = load("training_clients.csv", r -> {
            TrainingClient c = new TrainingClient();
            c.setProgram(prg.get(r.get("program_code")));
            c.setClientName(r.get("client_name"));
            c.setContactPerson(r.get("contact_person"));
            c.setPhone(r.get("phone"));
            c.setEmail(r.get("email"));
            c.setDateContacted(date(r.get("date_contacted")));
            c.setProposalSubmitted("YES".equals(r.get("proposal_submitted")));
            c.setProposalDate(date(r.get("proposal_date")));
            c.setAccepted(r.get("accepted").isBlank() ? null : Acceptance.valueOf(r.get("accepted")));
            c.setAcceptedDate(date(r.get("accepted_date")));
            c.setAcceptedRemarks(blank(r.get("accepted_remarks")));
            c.setConducted("YES".equals(r.get("conducted")));
            c.setConductedFrom(date(r.get("conducted_from")));
            c.setConductedTo(date(r.get("conducted_to")));
            c.setBudgetRequired(money(r.get("budget_required")));
            c.setFeeCharged(money(r.get("fee_charged")));
            return Map.entry(r.get("program_code") + ":" + r.get("client_name"), clients.save(c));
        });
        for (var r : Csv.read("training_acceptance_history.csv")) {
            TrainingClient c = cli.get(r.get("program_code") + ":" + r.get("client_name"));
            acceptanceChanges.save(new AcceptanceChange(c, date(r.get("date")), Acceptance.valueOf(r.get("accepted")),
                    blank(r.get("remarks")), "staff"));
        }
        cli.forEach((key, c) -> {
            if (c.getAccepted() == Acceptance.YES) {
                budgetByRef.put("TRAINING:" + key, budgetService.syncTrainingBudget(c));
            }
        });

        for (var r : Csv.read("events.csv")) {
            Event e = new Event();
            e.setName(r.get("name"));
            e.setType(r.get("type"));
            e.setStartDate(date(r.get("start_date")));
            e.setEndDate(date(r.get("end_date")));
            e.setVenue(r.get("venue"));
            e.setStatus(EventStatus.valueOf(r.get("status")));
            events.save(e);
            budgetService.syncEventBudget(e, money(r.get("budget_amount")));
            budgets.findByEventId(e.getId()).ifPresent(b -> budgetByRef.put("EVENT:" + r.get("code"), b));
        }

        for (var r : Csv.read("expenses.csv")) {
            Budget b = budgetByRef.get(r.get("budget_type") + ":" + r.get("budget_ref"));
            if (b == null) {
                throw new IllegalStateException("expenses.csv: no budget " + r.get("budget_type") + " " + r.get("budget_ref"));
            }
            Expense e = new Expense();
            e.setBudget(b);
            e.setDate(date(r.get("date")));
            e.setAmount(money(r.get("amount")));
            e.setCategory(ExpenseCategory.valueOf(r.get("category")));
            e.setDescription(r.get("description"));
            e.setStatus(ExpenseStatus.valueOf(r.get("status")));
            e.setRaisedBy(r.get("raised_by"));
            e.setDecidedBy(blank(r.get("decided_by")));
            if (e.getDecidedBy() != null) {
                e.setDecidedAt(e.getDate().plusDays(2).atTime(11, 0).atZone(ZoneId.systemDefault()).toInstant());
            }
            e.setDecisionRemark(blank(r.get("decision_remark")));
            expenses.save(e);
        }
        log.info("Loaded sample data: {} faculty, {} students, {} course offerings, {} projects",
                fac.size(), stu.size(), off.size(), proj.size());
    }

    private static <T> Map<String, T> load(String file, Function<Map<String, String>, Map.Entry<String, T>> make) {
        Map<String, T> out = new HashMap<>();
        for (Map<String, String> row : Csv.read(file)) {
            var e = make.apply(row);
            out.put(e.getKey(), e.getValue());
        }
        return out;
    }

    private static LocalDate date(String s) {
        return s == null || s.isBlank() ? null : LocalDate.parse(s);
    }

    private static BigDecimal money(String s) {
        return s == null || s.isBlank() ? null : new BigDecimal(s);
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
