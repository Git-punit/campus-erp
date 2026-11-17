# Graph Report - campus-erp  (2026-11-17)

## Corpus Check
- 182 files · ~50,338 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 32 file(s) not represented in the graph (top: .csv 21, (none) 5, .properties 4)

## Summary
- 1525 nodes · 5714 edges · 53 communities (35 shown, 18 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 425 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `6a36e8e8`
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
- useAuth
- .postAs
- jakarta.servlet.http.HttpServletRequest
- AuthController
- org.junit.jupiter.api.Test
- OfferingService
- SecurityConfig.java
- campus-erp
- CalendarService
- BudgetService
- with
- AcademicCalendar
- BudgetsPage.tsx
- DashboardController
- .getAs
- UserService
- api.ts
- BudgetService.java
- .login
- StudentRepository
- compilerOptions
- UserRepository
- .find
- JwtService
- org.junit.jupiter.api.BeforeEach
- DemoAccountsSeederTest.java
- DemoAccountsSeeder.java
- SignupTest
- Role
- .find
- org.springframework.web.bind.annotation.GetMapping
- OfferingsTest
- org.springframework.test.web.servlet.MockMvc
- CampusErpApplication.java
- Sample data
- com.campuserp:campus-erp

## God Nodes (most connected - your core abstractions)
1. `BaseEntity` - 53 edges
2. `ErrorBox()` - 48 edges
3. `BusinessRuleException` - 45 edges
4. `useLoad()` - 44 edges
5. `SetupController` - 42 edges
6. `useAuth()` - 41 edges
7. `CourseOffering` - 40 edges
8. `Faculty` - 37 edges
9. `Department` - 36 edges
10. `BudgetService` - 36 edges

## Surprising Connections (you probably didn't know these)
- `RequireLogin()` --calls--> `useAuth()`  [EXTRACTED]
  frontend/src/App.tsx → frontend/src/auth.tsx
- `RequireAdmin()` --calls--> `useAuth()`  [EXTRACTED]
  frontend/src/App.tsx → frontend/src/auth.tsx
- `App()` --calls--> `AccountPage()`  [EXTRACTED]
  frontend/src/App.tsx → frontend/src/pages/AccountPage.tsx
- `App()` --calls--> `BudgetDetailPage()`  [EXTRACTED]
  frontend/src/App.tsx → frontend/src/pages/BudgetDetailPage.tsx
- `App()` --calls--> `BudgetsPage()`  [EXTRACTED]
  frontend/src/App.tsx → frontend/src/pages/BudgetsPage.tsx

## Import Cycles
- None detected.

## Communities (53 total, 18 thin omitted)

### Community 0 - "DemoDataSeeder.java"
Cohesion: 0.06
Nodes (48): Budget, Expense, ReceiptFile, BaseEntity, Ref, ClassException, Course, CourseOffering (+40 more)

### Community 1 - "ExpenseService"
Cohesion: 0.12
Nodes (12): BudgetController, DecisionRequest, ExpenseRequest, ExpenseView, ExpenseCategory, CONSUMABLES, EQUIPMENT, HONORARIUM (+4 more)

### Community 2 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.13
Nodes (8): ApiTest, HealthAndErrorsTest, SearchTest, SpaForwardTest, EventStatusTest, TestClock, LoginHistoryTest, ProfileTest

### Community 4 - "FacultyController"
Cohesion: 0.05
Nodes (17): Csv, CsvImporter, Result, FacultyController, FacultyRequest, FacultyResponse, AddCourseRequest, SectionEnrolRequest (+9 more)

### Community 5 - "useLoad"
Cohesion: 0.07
Nodes (55): Column, CrudConfig, CrudSection(), defaultRequest(), enumOptions(), Field(), FieldDef, FieldType (+47 more)

### Community 6 - "OfferingService.java"
Cohesion: 0.12
Nodes (12): OfferingDtos, OfferingRequest, OfferingView, SaveResult, SlotInput, SlotView, TeacherInput, TeacherView (+4 more)

### Community 7 - "ErrorBox"
Cohesion: 0.12
Nodes (37): CalendarView(), Empty(), ErrorBox(), Loading(), date(), day(), isoDate(), label() (+29 more)

### Community 8 - "SetupController"
Cohesion: 0.13
Nodes (12): ProgrammeRepository, AcademicYearRequest, AcademicYearResponse, DepartmentRequest, DepartmentResponse, HolidayRequest, HolidayResponse, ProgrammeRequest (+4 more)

### Community 9 - "App.tsx"
Cohesion: 0.10
Nodes (30): api, setUnauthorizedHandler(), App(), RequireAdmin(), RequireLogin(), AuthContext, AuthProvider(), AuthState (+22 more)

### Community 10 - "org.springframework.data.jpa.repository.JpaRepository"
Cohesion: 0.12
Nodes (4): ReceiptFileRepository, OfferingTeacherRepository, ProjectMemberRepository, LoginEventRepository

### Community 11 - ".view"
Cohesion: 0.11
Nodes (3): AcceptanceChangeRepository, TrainingController, TrainingService

### Community 12 - "package.json"
Cohesion: 0.08
Nodes (32): dependencies, @fullcalendar/core@^6.1.22, @fullcalendar/daygrid@^6.1.22, @fullcalendar/react@^6.1.22, @fullcalendar/timegrid@^6.1.22, react@^19.3.1, react-dom@^19.3.1, react-router@^8.5.1 (+24 more)

### Community 13 - "useAuth"
Cohesion: 0.11
Nodes (27): setToken(), Role, useAuth(), User, Avatar(), ago(), device(), Login (+19 more)

### Community 16 - "AuthController"
Cohesion: 0.11
Nodes (12): AuthController, LoginRequest, LoginResponse, PasswordChange, SignupRequest, UsernameChange, ProfileChange, ProfileController (+4 more)

### Community 17 - "org.junit.jupiter.api.Test"
Cohesion: 0.13
Nodes (3): CampusErpApplicationTests, TrainingTest, AuthAndRolesTest

### Community 18 - "OfferingService"
Cohesion: 0.14
Nodes (5): ClassExceptionRepository, CourseOfferingRepository, OfferingService, SectionRepository, EnrolmentRepository

### Community 20 - "campus-erp"
Cohesion: 0.07
Nodes (26): 1. Overall system, 2. Project directory structure, 3. Frontend startup and routing, 4. Backend layers, 5. One request from click to database, 6. Academic structure, 7. Database tables and links, Academic setup (+18 more)

### Community 21 - "CalendarService"
Cohesion: 0.23
Nodes (5): CalendarController, CalendarEntry, CalendarService, CalendarView, HolidayView

### Community 22 - "BudgetService"
Cohesion: 0.11
Nodes (11): BudgetRepository, BudgetService, ExpenseRepository, ExpenseStatus, APPROVED, PENDING, REJECTED, FacultyStatus (+3 more)

### Community 23 - "with"
Cohesion: 0.09
Nodes (26): name, runs-on, steps, run, defaults, name, runs-on, steps (+18 more)

### Community 24 - "AcademicCalendar"
Cohesion: 0.22
Nodes (3): AcademicCalendar, Current, AcademicYearRepository

### Community 26 - "BudgetsPage.tsx"
Cohesion: 0.14
Nodes (27): ExpenseTable(), approve(), attach(), changed(), reject(), remove(), removeReceipt(), Badge() (+19 more)

### Community 28 - "DashboardController"
Cohesion: 0.20
Nodes (10): Budgets, Dashboard, DashboardController, Money, Pending, Pipeline, Projects, Semester (+2 more)

### Community 29 - ".getAs"
Cohesion: 0.19
Nodes (3): CalendarTest, SampleDataTest, ImportTest

### Community 30 - "UserService"
Cohesion: 0.17
Nodes (3): NotFoundException, UserPhotoRepository, UserService

### Community 31 - "api.ts"
Cohesion: 0.23
Nodes (13): ApiError, cantReach(), getToken(), hostAndPort(), openFile(), portOf(), request(), send() (+5 more)

### Community 33 - "BudgetService.java"
Cohesion: 0.07
Nodes (19): AllocationRequest, BudgetDtos, BudgetSummary, Source, BudgetType, DEPARTMENT, EVENT, PROJECT (+11 more)

### Community 34 - ".login"
Cohesion: 0.22
Nodes (4): Entry, LoginAttempts, LoginAttemptsTest, MovingClock

### Community 35 - "StudentRepository"
Cohesion: 0.17
Nodes (11): CourseRepository, EventRepository, FacultyRepository, ResearchProjectRepository, Hit, SearchController, DemoDataSeeder, DepartmentRepository (+3 more)

### Community 37 - "compilerOptions"
Cohesion: 0.12
Nodes (15): compilerOptions, isolatedModules, jsx, lib, module, moduleResolution, noFallthroughCasesInSwitch, noUnusedLocals (+7 more)

### Community 39 - ".find"
Cohesion: 0.21
Nodes (9): MemberInput, MemberView, ProjectController, ProjectRequest, ProjectResponse, ProjectStatus, CLOSED, COMPLETED (+1 more)

### Community 46 - "Role"
Cohesion: 0.29
Nodes (4): Role, ADMIN, STAFF, VIEWER

### Community 48 - ".find"
Cohesion: 0.14
Nodes (5): DepartmentBudgetRequest, BusinessRuleException, CourseController, CourseRequest, CourseResponse

### Community 50 - "org.springframework.web.bind.annotation.GetMapping"
Cohesion: 0.12
Nodes (6): SpaForwardController, OfferingController, ExceptionRequest, ExceptionView, LoginView, UserController

### Community 54 - "Sample data"
Cohesion: 0.40
Nodes (4): Cases worth knowing about, Files, How the budget references work, Sample data

## Knowledge Gaps
- **45 isolated node(s):** `PLANNED`, `ONGOING`, `COMPLETED`, `CANCELLED`, `POSTPONED` (+40 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 266 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **18 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `BusinessRuleException` connect `.find` to `DemoDataSeeder.java`, `BudgetService.java`, `ExpenseService`, `org.springframework.web.bind.annotation.GetMapping`, `FacultyController`, `OfferingService.java`, `.find`, `SetupController`, `CalendarFeedController.java`, `jakarta.servlet.http.HttpServletRequest`, `AuthController`, `org.springframework.web.bind.annotation.GetMapping`, `CalendarService`, `org.springframework.web.bind.annotation.ResponseStatus`, `UserService`?**
  _High betweenness centrality (0.019) - this node is a cross-community bridge._
- **What connects `PLANNED`, `ONGOING`, `COMPLETED` to the rest of the system?**
  _45 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `DemoDataSeeder.java` be split into smaller, more focused modules?**
  _Cohesion score 0.06273099590450504 - nodes in this community are weakly interconnected._
- **Why does `UserRepository` connect `UserRepository` to `DemoDataSeeder.java`, `.login`, `org.springframework.transaction.annotation.Transactional`, `org.springframework.data.jpa.repository.JpaRepository`, `DemoAccountsSeeder.java`, `DemoAccountsSeederTest.java`, `SignupTest`, `Role`, `AuthController`, `org.junit.jupiter.api.Test`, `SecurityConfig.java`, `UserService`?**
  _High betweenness centrality (0.013) - this node is a cross-community bridge._
- **Should `ExpenseService` be split into smaller, more focused modules?**
  _Cohesion score 0.12280701754385964 - nodes in this community are weakly interconnected._
- **Why does `AuthAndRolesTest` connect `org.junit.jupiter.api.Test` to `org.springframework.transaction.annotation.Transactional`, `org.springframework.test.web.servlet.MockMvc`, `UserRepository`?**
  _High betweenness centrality (0.013) - this node is a cross-community bridge._
- **Should `org.springframework.transaction.annotation.Transactional` be split into smaller, more focused modules?**
  _Cohesion score 0.12583182093163944 - nodes in this community are weakly interconnected._