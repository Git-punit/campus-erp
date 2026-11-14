# Graph Report - campus-erp  (2026-11-13)

## Corpus Check
- 177 files · ~49,227 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 32 file(s) not represented in the graph (top: .csv 21, (none) 5, .properties 4)

## Summary
- 1477 nodes · 5632 edges · 57 communities (37 shown, 20 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 416 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `14681ae3`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- DemoDataSeeder.java
- ExpenseService
- org.springframework.transaction.annotation.Transactional
- org.springframework.web.bind.annotation.GetMapping
- FacultyController
- useLoad
- OfferingService.java
- ErrorBox
- SetupController
- App.tsx
- org.springframework.data.jpa.repository.JpaRepository
- .view
- package.json
- auth.tsx
- .postAs
- jakarta.servlet.http.HttpServletRequest
- AuthController
- org.junit.jupiter.api.Test
- FacultyRepository
- SecurityConfig.java
- campus-erp
- CalendarService
- StudentController
- with
- BudgetService
- org.springframework.web.bind.annotation.ResponseStatus
- BudgetsPage.tsx
- types.ts
- DashboardController
- .getAs
- UserService
- ui.tsx
- BusinessRuleException
- EventController
- .login
- StudentRepository
- UsersPage.tsx
- compilerOptions
- UserRepository
- .find
- JwtService
- DemoAccountsSeederTest.java
- DemoAccountsSeeder.java
- SignupTest
- Role
- CourseController
- TrainingProgramPage.tsx
- ExceptionView
- OfferingsTest
- org.springframework.test.web.servlet.MockMvc
- CampusErpApplication.java
- Sample data
- SpaForwardController.java
- com.campuserp:campus-erp

## God Nodes (most connected - your core abstractions)
1. `BaseEntity` - 53 edges
2. `BusinessRuleException` - 52 edges
3. `ErrorBox()` - 45 edges
4. `SetupController` - 42 edges
5. `useAuth()` - 41 edges
6. `useLoad()` - 40 edges
7. `CourseOffering` - 40 edges
8. `BudgetService` - 39 edges
9. `Faculty` - 38 edges
10. `Department` - 37 edges

## Surprising Connections (you probably didn't know these)
- `submit()` --calls--> `setToken()`  [EXTRACTED]
  frontend/src/pages/ChangePasswordPage.tsx → frontend/src/api.ts
- `App()` --calls--> `AccountPage()`  [EXTRACTED]
  frontend/src/App.tsx → frontend/src/pages/AccountPage.tsx
- `App()` --calls--> `BudgetDetailPage()`  [EXTRACTED]
  frontend/src/App.tsx → frontend/src/pages/BudgetDetailPage.tsx
- `App()` --calls--> `BudgetsPage()`  [EXTRACTED]
  frontend/src/App.tsx → frontend/src/pages/BudgetsPage.tsx
- `App()` --calls--> `DashboardPage()`  [EXTRACTED]
  frontend/src/App.tsx → frontend/src/pages/DashboardPage.tsx

## Import Cycles
- None detected.

## Communities (57 total, 20 thin omitted)

### Community 0 - "DemoDataSeeder.java"
Cohesion: 0.06
Nodes (54): Budget, Expense, ReceiptFile, BaseEntity, Ref, ClassException, Course, CourseOffering (+46 more)

### Community 1 - "ExpenseService"
Cohesion: 0.07
Nodes (23): BudgetController, AllocationRequest, BudgetDtos, BudgetSummary, DecisionRequest, DepartmentBudgetRequest, ExpenseRequest, ExpenseView (+15 more)

### Community 2 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.14
Nodes (7): ApiTest, HealthAndErrorsTest, SearchTest, SpaForwardTest, TestClock, LoginHistoryTest, ProfileTest

### Community 4 - "FacultyController"
Cohesion: 0.09
Nodes (6): Csv, CsvImporter, Result, FacultyController, FacultyRequest, FacultyResponse

### Community 5 - "useLoad"
Cohesion: 0.13
Nodes (31): Column, CrudConfig, CrudSection(), defaultRequest(), enumOptions(), Field(), FieldDef, FieldType (+23 more)

### Community 6 - "OfferingService.java"
Cohesion: 0.10
Nodes (9): CourseOfferingRepository, OfferingDtos, OfferingRequest, OfferingView, SaveResult, SlotInput, SlotView, TeacherInput (+1 more)

### Community 7 - "ErrorBox"
Cohesion: 0.16
Nodes (24): CalendarView(), Empty(), ErrorBox(), Loading(), day(), isoDate(), label(), rupeeFormat (+16 more)

### Community 8 - "SetupController"
Cohesion: 0.12
Nodes (11): AcademicYearRequest, AcademicYearResponse, DepartmentRequest, DepartmentResponse, HolidayRequest, HolidayResponse, ProgrammeRequest, ProgrammeResponse (+3 more)

### Community 9 - "App.tsx"
Cohesion: 0.12
Nodes (27): App(), RequireAdmin(), RequireLogin(), useAuth(), ThemeToggle(), Crumbs(), isUnder(), Layout() (+19 more)

### Community 11 - ".view"
Cohesion: 0.12
Nodes (7): TrainingController, ClientRequest, ClientView, HistoryView, ProgramRequest, ProgramView, TrainingDtos

### Community 12 - "package.json"
Cohesion: 0.08
Nodes (32): dependencies, @fullcalendar/core@^6.1.22, @fullcalendar/daygrid@^6.1.22, @fullcalendar/react@^6.1.22, @fullcalendar/timegrid@^6.1.22, react@^19.3.1, react-dom@^19.3.1, react-router@^8.5.1 (+24 more)

### Community 13 - "auth.tsx"
Cohesion: 0.14
Nodes (22): api, ApiError, getToken(), openFile(), request(), setToken(), setUnauthorizedHandler(), AuthContext (+14 more)

### Community 14 - ".postAs"
Cohesion: 0.17
Nodes (3): BudgetAndExpenseTest, EnrolmentTest, TrainingTest

### Community 16 - "AuthController"
Cohesion: 0.15
Nodes (8): AuthController, LoginResponse, PasswordChange, SignupRequest, UsernameChange, ProfileChange, ProfileController, UserResponse

### Community 18 - "FacultyRepository"
Cohesion: 0.20
Nodes (12): ClassExceptionRepository, CourseRepository, OfferingService, FacultyRepository, DemoDataSeeder, AcademicYearRepository, DepartmentRepository, HolidayRepository (+4 more)

### Community 20 - "campus-erp"
Cohesion: 0.07
Nodes (27): 1. Overall system, 2. Project directory structure, 3. Frontend startup and routing, 4. Backend layers, 5. Login and access checks, 6. One request from click to database, 7. Academic structure, 8. Database tables and links (+19 more)

### Community 21 - "CalendarService"
Cohesion: 0.15
Nodes (6): CalendarController, CalendarEntry, CalendarFeedController, CalendarService, CalendarView, HolidayView

### Community 22 - "StudentController"
Cohesion: 0.12
Nodes (11): AddCourseRequest, SectionEnrolRequest, SectionEnrolResult, StudentController, StudentRequest, StudentResponse, StudentStatus, ACTIVE (+3 more)

### Community 23 - "with"
Cohesion: 0.09
Nodes (26): name, runs-on, steps, run, defaults, name, runs-on, steps (+18 more)

### Community 24 - "BudgetService"
Cohesion: 0.14
Nodes (6): BudgetRepository, BudgetService, AcademicCalendar, Current, AcceptanceChangeRepository, TrainingService

### Community 26 - "BudgetsPage.tsx"
Cohesion: 0.16
Nodes (21): ExpenseTable(), approve(), attach(), changed(), reject(), remove(), removeReceipt(), Cell (+13 more)

### Community 27 - "types.ts"
Cohesion: 0.11
Nodes (22): WarningBox(), CalendarData, CampusEvent, ClassChange, Course, Day, ExpenseCategory, ExpenseStatus (+14 more)

### Community 28 - "DashboardController"
Cohesion: 0.15
Nodes (15): ExpenseRepository, ExpenseStatus, APPROVED, PENDING, REJECTED, Budgets, Dashboard, DashboardController (+7 more)

### Community 29 - ".getAs"
Cohesion: 0.20
Nodes (3): CalendarTest, SampleDataTest, ImportTest

### Community 30 - "UserService"
Cohesion: 0.13
Nodes (6): NotFoundException, LoginEventRepository, LoginView, UserController, UserPhotoRepository, UserService

### Community 31 - "ui.tsx"
Cohesion: 0.17
Nodes (15): Badge(), Money(), TabDef, UsageBar(), todayIso(), MemberRole, AllocationForm(), BudgetDetailPage() (+7 more)

### Community 32 - "BusinessRuleException"
Cohesion: 0.17
Nodes (3): BusinessRuleException, Finder, Money

### Community 33 - "EventController"
Cohesion: 0.20
Nodes (8): EventController, EventRequest, EventResponse, EventStatus, CANCELLED, COMPLETED, ONGOING, PLANNED

### Community 34 - ".login"
Cohesion: 0.22
Nodes (5): LoginRequest, Entry, LoginAttempts, LoginAttemptsTest, MovingClock

### Community 35 - "StudentRepository"
Cohesion: 0.23
Nodes (6): EventRepository, ResearchProjectRepository, Hit, SearchController, StudentRepository, TrainingProgramRepository

### Community 36 - "UsersPage.tsx"
Cohesion: 0.17
Nodes (12): Role, ago(), device(), EMPTY, FormState, Login, OUTCOME, ROLES (+4 more)

### Community 37 - "compilerOptions"
Cohesion: 0.12
Nodes (15): compilerOptions, isolatedModules, jsx, lib, module, moduleResolution, noFallthroughCasesInSwitch, noUnusedLocals (+7 more)

### Community 39 - ".find"
Cohesion: 0.29
Nodes (5): MemberInput, MemberView, ProjectController, ProjectRequest, ProjectResponse

### Community 46 - "Role"
Cohesion: 0.20
Nodes (7): Role, ADMIN, STAFF, VIEWER, Create, Update, UserRequests

### Community 48 - "CourseController"
Cohesion: 0.42
Nodes (3): CourseController, CourseRequest, CourseResponse

### Community 49 - "TrainingProgramPage.tsx"
Cohesion: 0.31
Nodes (7): Acceptance, TrainingClient, TrainingProgram, ClientForm, TrainingProgramPage(), refresh(), remove()

### Community 54 - "Sample data"
Cohesion: 0.40
Nodes (4): Cases worth knowing about, Files, How the budget references work, Sample data

## Knowledge Gaps
- **44 isolated node(s):** `com.campuserp:campus-erp`, `DEPARTMENT`, `EVENT`, `TRAINING`, `PROJECT` (+39 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 261 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **20 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `BusinessRuleException` connect `BusinessRuleException` to `DemoDataSeeder.java`, `ExpenseService`, `org.springframework.web.bind.annotation.GetMapping`, `FacultyController`, `OfferingService.java`, `SetupController`, `.view`, `jakarta.servlet.http.HttpServletRequest`, `AuthController`, `FacultyRepository`, `CalendarService`, `StudentController`, `BudgetService`, `org.springframework.web.bind.annotation.ResponseStatus`, `UserService`, `EventController`, `.find`, `CourseController`, `ExceptionView`?**
  _High betweenness centrality (0.021) - this node is a cross-community bridge._
- **What connects `com.campuserp:campus-erp`, `DEPARTMENT`, `EVENT` to the rest of the system?**
  _44 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `DemoDataSeeder.java` be split into smaller, more focused modules?**
  _Cohesion score 0.056335540838852095 - nodes in this community are weakly interconnected._
- **Why does `ExpenseService` connect `ExpenseService` to `BusinessRuleException`, `org.springframework.transaction.annotation.Transactional`, `org.springframework.web.bind.annotation.GetMapping`, `BudgetService`, `DashboardController`?**
  _High betweenness centrality (0.019) - this node is a cross-community bridge._
- **Should `ExpenseService` be split into smaller, more focused modules?**
  _Cohesion score 0.06875 - nodes in this community are weakly interconnected._
- **Why does `UserRepository` connect `UserRepository` to `DemoDataSeeder.java`, `org.springframework.transaction.annotation.Transactional`, `org.springframework.web.bind.annotation.GetMapping`, `org.springframework.data.jpa.repository.JpaRepository`, `DemoAccountsSeeder.java`, `DemoAccountsSeederTest.java`, `SignupTest`, `Role`, `AuthController`, `org.junit.jupiter.api.Test`, `SecurityConfig.java`, `UserService`?**
  _High betweenness centrality (0.016) - this node is a cross-community bridge._
- **Should `org.springframework.transaction.annotation.Transactional` be split into smaller, more focused modules?**
  _Cohesion score 0.13556953179594688 - nodes in this community are weakly interconnected._