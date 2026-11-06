# campus-erp

ERP for running a college campus. It keeps track of faculty, students,
courses, research projects, training programs, events and budgets, and has a
dashboard on top so you can see what's going on without opening every page.

Backend is Spring Boot 4.1 on Java 25 (JPA, Spring Security + JWT, MySQL).
Frontend is React 19 + TypeScript with Vite, in the `frontend` folder.

Fee management, attendance and library were in an older version and have been
dropped for now.

## Modules

- **Dashboard** - faculty and student counts, courses running this semester,
  today's classes, project funds used, training pipeline, budget usage,
  expenses waiting for approval, upcoming events
- **Faculty** - staff records, which courses each teacher teaches per
  semester, and which research projects they're on
- **Courses** - the course catalogue, plus offerings (which course runs in
  which semester/section, who teaches it, and when)
- **Students** - profile, programme, semester, section, enrolments and a class
  calendar
- **Research projects** - funded projects, team, sanctioned amount and spend
- **Training programs** - programs we pitch to outside clients, from first
  contact to delivery, with fee vs cost
- **Events** - campus events with dates, venue and status
- **Budgets** - department / event / training / project budgets, expense
  requests and admin approval
- **Academic setup** - departments, programmes, sections, academic years,
  semester dates, holidays
- **Users** - admin only

Roughly: a course gets offered in a semester for a section, teachers are
assigned to the offering, students enrol in it, and the calendar is built
from those offerings minus holidays. Projects, accepted training proposals,
events and departments each get a budget, and approved expenses are charged
against those budgets.

## Faculty

Faculty record: employee ID, name, department, designation, email, phone and
status (Active / On leave / Left).

The faculty list has a Courses count and a Projects count for each teacher.
Click either one to see the actual list. For example, a maths teacher's
courses might look like:

| Course | Semester | Section | Year |
|---|---|---|---|
| Maths I | 1 | CSE-A | 2026-27 |
| Maths II | 2 | CSE-A | 2026-27 |
| Maths I | 3 | Civil-A | 2026-27 |
| Maths I | 4 | Mech-A | 2026-27 |

A few things to know:

- The same course running for two branches is two separate offerings.
- An offering can have more than one teacher (e.g. theory + lab), and it
  shows up for each of them.
- Old academic years aren't deleted. The list defaults to the current year
  and you can filter back.

The projects list shows funding agency, amount, utilized and balance for each
project (see Research projects below for how those are calculated).

## Courses

A course is just code, name, credits and department.

An offering is one run of a course:

- course, academic year, semester (1-8), programme, section
- one or more teachers, optionally tagged Theory / Lab / Tutorial
- weekly timetable slots (day, start, end, room)

When you save a timetable it warns you if a teacher or room is double booked,
or if a section has two classes at the same time.

## Students

Student record: roll number, name, programme, current semester, section,
email, phone, status.

Enrolment can be done for a whole section at once (e.g. put all of Sem 3
CSE-A into its offerings) or one student at a time.

### Calendar

Nobody types in the calendar. It's generated from the student's enrolments
and the timetables of those offerings:

- weekly classes repeat from semester start to semester end
- holidays from Academic setup are skipped
- a single class can be cancelled for one date, with a reason
- extra one-off classes can be added for a date/time/room
- day, week and month views

Teachers get the same calendar for the classes they teach.

## Research projects

Each project has a title, funding agency (DST, SERB, AICTE etc.), sanctioned
amount, sanction/start/end dates, status (Ongoing / Completed / Closed) and a
team: one PI plus any co-investigators. The project shows up under every team
member.

We assume the full sanctioned amount comes in at once.

- Utilized = sum of approved expenses
- Balance = sanctioned - utilized
- the progress bar goes red past 100%

Expenses go through the normal approval flow in Budgets. Pending ones are
shown but not counted.

## Training programs

A program has a name, description, duration and a coordinator (one of the
teachers). The same program can be pitched to lots of clients, and each
client gets its own record:

- client name, contact person, phone, email
- date contacted
- proposal submitted (yes/no + date)
- accepted (yes / no / needs revision, with date and remarks)
- conducted (yes/no + dates)
- budget required
- fee charged

The steps have to happen in order: you can't mark it accepted before the
proposal is submitted, or conducted before it's accepted. "Needs revision"
means the proposal went back for changes. Every change to the accepted field
is kept as history.

Once a client accepts, a training budget is created automatically for the
budget required amount. Actual cost is whatever's been approved against that
budget, and profit/loss is fee charged minus actual cost.

The program list shows how many clients were contacted, how many proposals
went out, how many were accepted and how many were conducted.

## Events

Name, type, start/end dates, venue and status (Planned / Ongoing / Completed /
Cancelled). An event can have its own budget.

## Budgets

Any money spent goes through here, against exactly one budget.

| Type | Created | Allocation |
|---|---|---|
| Department | manually, one per department per financial year (Apr-Mar) | yearly amount |
| Event | from the event | whatever's entered |
| Training | automatically when a client accepts | budget required |
| Research project | automatically when the project is added | sanctioned amount |

Expense flow:

1. Staff raise a request: budget, date, amount, category (Equipment / Travel /
   Consumables / Honorarium / Other), description.
2. It sits as Pending until an admin approves or rejects it with a remark.
3. Only approved expenses count as utilized.
4. If approving would push the budget over its allocation, the admin gets a
   warning first.

Each budget shows allocated, utilized, pending and balance, with a usage bar
and an overspend flag. You can filter by type, department and financial year.

## Academic setup

Departments, programmes (like B.Tech CSE, with number of semesters), sections
(CSE-A etc.), academic years with odd/even semester dates, and holidays.

## Roles

| Role | Access |
|---|---|
| ADMIN | everything, including users and expense approval |
| STAFF | create/edit/delete in all modules, raise expenses |
| VIEWER | read only |

Faculty and students don't have logins. Admin/staff open their pages instead.

## Running it locally

Needs Java 25 and Node 20+. MySQL 8 is optional for trying it out.

**Quickest: no MySQL.** The `dev` profile uses a built-in database stored in
`data/` in the project folder (kept between runs; delete the folder to start
over from the sample data). Backend, on port 8080:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

**With MySQL.** Set your MySQL login (the `campus_erp` database gets created on
first run). Use your real MySQL password; if it's wrong the backend stops
straight away with `Access denied for user 'root'`.

```bash
export DB_PASSWORD='your-mysql-password'
```

```bash
./mvnw spring-boot:run
```

Wait for `Started CampusErpApplication` in the backend terminal. You can check
it's up at http://localhost:8080/api/health.

Frontend dev server, on port 5173 (proxies `/api` to 8080):

```bash
cd frontend && npm install && npm run dev
```

Then go to http://localhost:5173.

### Demo data

If the database is empty on startup it creates three users (`admin`, `staff`,
`viewer`) and fills everything with made-up sample data. The passwords are
the `app.seed.*-password` entries in `src/main/resources/application.properties`,
or you can set `ADMIN_PASSWORD`, `STAFF_PASSWORD` and `VIEWER_PASSWORD`.
`SEED_DEMO_DATA=false` turns the sample data off.

The sample records are CSV files in `src/main/resources/sample-data/`. Its
README lists what's in each file and the cases built in on purpose (an
overspent project, a training run at a loss, Diwali inside the semester...).

The default passwords and JWT secret are public (they're in this repo), so the
app won't use them on a server. If the database isn't on your own machine
(localhost, 127.0.0.1 or the in-memory test database) and `JWT_SECRET` is
still the default, it stops at startup and says what to set. The same goes for
`ADMIN_PASSWORD`, `STAFF_PASSWORD` and `VIEWER_PASSWORD` while the accounts
haven't been created yet. Locally it just logs a warning. For a throwaway
demo you can turn the check off with `ALLOW_DEV_DEFAULTS=true`.

After the first start, change the passwords from the Users page.

### One jar

`npm run build` puts the React build into `src/main/resources/static/`, so
Spring Boot serves both the UI and the API:

```bash
cd frontend && npm run build && cd .. && ./mvnw package
```

```bash
java -jar target/campus-erp-0.0.1-SNAPSHOT.jar
```

Refreshing a page or opening a link like `/faculty/3` works here too: any
address that isn't `/api/...` or a file gets the React app, which then shows
the right page.

## Tests

```bash
./mvnw test
```

Tests use in-memory H2, so you don't need MySQL for them. They cover login
and roles (including not being able to remove the last admin), validation,
offerings with multiple teachers and clash warnings, enrolment, the calendar
(weekly classes, holidays, cancelled/extra classes), training step order and
budget creation, expense approval/rejection and the utilized/balance numbers,
and the dashboard. One test loads the full sample data and checks the
numbers quoted in this README, and that the sample timetable has no clashes.

GitHub Actions (`.github/workflows/ci.yml`) runs these tests and the frontend
type-check and build on every push to `main` and on every pull request.

## Config

| Variable | Default | What it's for |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/campus_erp?createDatabaseIfNotExist=true` | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / empty | MySQL login |
| `JWT_SECRET` | dev value | signs login tokens; required (32+ chars) when the database isn't local |
| `ADMIN_PASSWORD` / `STAFF_PASSWORD` / `VIEWER_PASSWORD` | dev values | passwords for the accounts created on an empty database |
| `SEED_DEMO_DATA` | `true` | load sample data into an empty db |
| `SERVER_PORT` | `8080` | HTTP port |
| `ALLOW_DEV_DEFAULTS` | `false` | let a non-local deployment start with the public defaults anyway |

Other business settings (like the financial year starting in April) are under
`app.*` in `application.properties`.

## Architecture

Diagrams of how the code is put together. They're written in Mermaid, which
GitHub draws as pictures.

### 1. Overall system

Faculty and students don't log in. Admin, staff and viewers open their pages.

```mermaid
flowchart TB
    USERS["Users<br/>Admin | Staff | Viewer"]

    subgraph FE["Frontend - React + TypeScript (Vite)"]
        ROUTES["Routes (App.tsx)"]
        AUTH["Login state (auth.tsx)"]
        PAGES["Pages"]
        COMPONENTS["Shared components"]
        API["API client (api.ts)"]
        ROUTES --> PAGES
        PAGES --> COMPONENTS
        PAGES --> API
        AUTH --> API
    end

    subgraph BE["Backend - Spring Boot"]
        FILTER["JWT filter"]
        SECURITY["Spring Security<br/>role rules"]
        CONTROLLERS["REST controllers"]
        SERVICES["Services"]
        REPOSITORIES["JPA repositories"]
        ERRORS["GlobalExceptionHandler"]
        FILTER --> SECURITY
        SECURITY --> CONTROLLERS
        CONTROLLERS --> SERVICES
        CONTROLLERS --> REPOSITORIES
        SERVICES --> REPOSITORIES
        CONTROLLERS -. errors .-> ERRORS
    end

    DB[("MySQL")]

    USERS --> ROUTES
    API <-->|"REST / JSON + Bearer token"| FILTER
    REPOSITORIES --> DB
```

### 2. Project directory structure

Build output (`frontend/node_modules`, `target/`, `src/main/resources/static/`)
and local editor settings are left out.

```text
campus-erp/
├── .github/workflows/ci.yml
├── .mvn/wrapper/maven-wrapper.properties
├── frontend/
│   ├── index.html
│   ├── package.json
│   ├── package-lock.json
│   ├── tsconfig.json
│   ├── vite.config.ts
│   └── src/
│       ├── main.tsx
│       ├── App.tsx
│       ├── auth.tsx
│       ├── api.ts
│       ├── styles.css
│       ├── components/
│       │   ├── CalendarView.tsx
│       │   ├── CrudSection.tsx
│       │   ├── ExpenseTable.tsx
│       │   └── ui.tsx
│       ├── layout/
│       │   └── Layout.tsx
│       ├── lib/
│       │   ├── format.ts
│       │   ├── types.ts
│       │   └── useLoad.ts
│       └── pages/
│           ├── BudgetDetailPage.tsx
│           ├── BudgetsPage.tsx
│           ├── CalendarPage.tsx
│           ├── CoursesPage.tsx
│           ├── DashboardPage.tsx
│           ├── EventsPage.tsx
│           ├── FacultyDetailPage.tsx
│           ├── FacultyPage.tsx
│           ├── LoginPage.tsx
│           ├── PlaceholderPage.tsx
│           ├── ProjectsPage.tsx
│           ├── SetupPage.tsx
│           ├── StudentDetailPage.tsx
│           ├── StudentsPage.tsx
│           ├── TrainingPage.tsx
│           ├── TrainingProgramPage.tsx
│           └── UsersPage.tsx
├── src/
│   ├── main/
│   │   ├── java/com/campuserp/
│   │   │   ├── CampusErpApplication.java
│   │   │   ├── auth/          AuthController, JwtService, JwtAuthFilter
│   │   │   ├── budget/        Budget, Expense, BudgetService, ExpenseService, BudgetController, ...
│   │   │   ├── calendar/      CalendarService, CalendarController, CalendarEntry
│   │   │   ├── common/        BaseEntity, ApiError, GlobalExceptionHandler, Finder, Money, Ref, ...
│   │   │   ├── config/        SecurityConfig, ClockConfig, SpaForwardController,
│   │   │   │                  DevDefaultsGuard, JwtSecretCheck
│   │   │   ├── course/        Course, CourseOffering, OfferingTeacher, TimetableSlot,
│   │   │   │                  ClassException, OfferingService, CourseController, OfferingController, ...
│   │   │   ├── dashboard/     DashboardController
│   │   │   ├── event/         Event, EventController, EventRepository, EventStatus
│   │   │   ├── faculty/       Faculty, FacultyController, FacultyRepository, FacultyStatus
│   │   │   ├── project/       ResearchProject, ProjectMember, ProjectController, ...
│   │   │   ├── seed/          DemoAccountsSeeder, DemoDataSeeder, Csv
│   │   │   ├── setup/         Department, Programme, Section, AcademicYear, Holiday,
│   │   │   │                  AcademicCalendar, SetupController, ...
│   │   │   ├── student/       Student, Enrolment, StudentController, ...
│   │   │   ├── training/      TrainingProgram, TrainingClient, AcceptanceChange, TrainingService, ...
│   │   │   └── user/          User, Role, UserService, UserController, ...
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── META-INF/spring.factories
│   │       └── sample-data/   21 CSV files + README.md
│   └── test/
│       ├── java/com/campuserp/
│       │   ├── ApiTest.java, Fixtures.java, TestClock.java, TestLogins.java
│       │   ├── CampusErpApplicationTests.java
│       │   ├── budget/BudgetAndExpenseTest.java
│       │   ├── calendar/CalendarTest.java
│       │   ├── common/HealthAndErrorsTest.java
│       │   ├── config/DevDefaultsGuardTest.java, SpaForwardTest.java
│       │   ├── course/OfferingsTest.java
│       │   ├── seed/SampleDataTest.java
│       │   ├── student/EnrolmentTest.java
│       │   ├── training/TrainingTest.java
│       │   └── user/AuthAndRolesTest.java
│       └── resources/application.properties
├── .gitattributes
├── .gitignore
├── HELP.md
├── README.md
├── mvnw, mvnw.cmd
└── pom.xml
```

### 3. Frontend startup and routing

`main.tsx` wraps the whole app in the router and the login state. The login
page sits outside the layout; every other page needs a login, and Users also
needs the admin role.

```mermaid
flowchart TD
    MAIN["main.tsx"] --> ROUTER["BrowserRouter"]
    ROUTER --> AUTH["AuthProvider (auth.tsx)"]
    AUTH --> APP["App.tsx - routes"]

    APP --> LOGIN["/login - LoginPage"]
    APP --> REQ["RequireLogin"]
    REQ --> LAYOUT["Layout.tsx - sidebar"]

    LAYOUT --> PAGES["Pages"]
    PAGES --> DASH["Dashboard"]
    PAGES --> FAC["Faculty + teacher page"]
    PAGES --> CRS["Courses"]
    PAGES --> STU["Students + student page"]
    PAGES --> CAL["Calendar"]
    PAGES --> BUD["Budgets + budget page"]
    PAGES --> PRJ["Research projects"]
    PAGES --> TRN["Training + program page"]
    PAGES --> EVT["Events"]
    PAGES --> SET["Academic setup"]
    LAYOUT --> ADMIN["RequireAdmin"] --> USERS["Users"]

    PAGES --> COMPONENTS["components/"]
    PAGES --> LIB["lib/ (types, format, useLoad)"]
    LIB --> API["api.ts"]
    API --> SERVER["Spring Boot REST API"]
```

### 4. Frontend files by role

```mermaid
flowchart TB
    A["Frontend"]

    A --> B["Login"]
    A --> C["Layout"]
    A --> D["Pages"]
    A --> E["Shared components"]
    A --> F["Utilities"]
    A --> G["Talking to the API"]

    B --> B1["auth.tsx - who is logged in, role checks"]
    B --> B2["LoginPage.tsx"]

    C --> C1["Layout.tsx - sidebar, user box"]

    D --> D1["DashboardPage"]
    D --> D2["StudentsPage, StudentDetailPage"]
    D --> D3["FacultyPage, FacultyDetailPage"]
    D --> D4["CoursesPage - catalogue and offerings"]
    D --> D5["TrainingPage, TrainingProgramPage"]
    D --> D6["ProjectsPage"]
    D --> D7["BudgetsPage, BudgetDetailPage"]
    D --> D8["EventsPage"]
    D --> D9["CalendarPage"]
    D --> D10["SetupPage"]
    D --> D11["UsersPage"]

    E --> E1["CrudSection - list + add/edit/delete form"]
    E --> E2["CalendarView - FullCalendar day/week/month"]
    E --> E3["ExpenseTable - approve / reject"]
    E --> E4["ui.tsx - badges, usage bar, tabs, messages"]

    F --> F1["types.ts - API shapes"]
    F --> F2["format.ts - rupees, dates, times"]
    F --> F3["useLoad.ts - load an endpoint"]

    G --> G1["api.ts - fetch, token, error messages"]
```

### 5. Backend layers

Bigger modules (courses, budgets, training, users) have a service class.
Simpler ones (setup, faculty, students, events, projects) call their
repositories straight from the controller.

```mermaid
flowchart TB
    A["CampusErpApplication.java"]

    A --> B["config/"]
    A --> C["auth/"]
    A --> D["Module packages"]
    A --> E["common/"]
    A --> F["seed/"]

    B --> B1["SecurityConfig - who can call what"]
    B --> B2["ClockConfig - one clock for 'today'"]

    C --> C1["AuthController - login, me"]
    C --> C2["JwtService - make and read tokens"]
    C --> C3["JwtAuthFilter - checks the token on each request"]

    D --> D1["Controllers"]
    D1 --> D2["Services (some modules)"]
    D1 --> D3["Repositories"]
    D2 --> D3
    D3 --> D4["Entities"]

    E --> E1["BaseEntity - id + timestamps"]
    E --> E2["GlobalExceptionHandler + ApiError"]
    E --> E3["Finder, Money, Ref"]
    E --> E4["HealthController - /api/health"]

    F --> F1["DemoAccountsSeeder"]
    F --> F2["DemoDataSeeder"]
    F --> F3["Csv"]

    D4 --> DB[("MySQL")]
```

### 6. Backend packages

```mermaid
flowchart TD
    ROOT["com.campuserp"]

    ROOT --> AUTH["auth"]
    ROOT --> CONFIG["config"]
    ROOT --> COMMON["common"]
    ROOT --> USER["user"]
    ROOT --> SETUP["setup"]
    ROOT --> FACULTY["faculty"]
    ROOT --> COURSE["course"]
    ROOT --> STUDENT["student"]
    ROOT --> CALENDAR["calendar"]
    ROOT --> BUDGET["budget"]
    ROOT --> PROJECT["project"]
    ROOT --> TRAINING["training"]
    ROOT --> EVENT["event"]
    ROOT --> DASH["dashboard"]
    ROOT --> SEED["seed"]

    AUTH --> A1["AuthController, JwtService, JwtAuthFilter"]
    CONFIG --> C1["SecurityConfig, ClockConfig,<br/>SpaForwardController, DevDefaultsGuard,<br/>JwtSecretCheck"]
    COMMON --> CM1["BaseEntity, ApiError, GlobalExceptionHandler,<br/>NotFoundException, BusinessRuleException,<br/>Finder, Money, Ref, HealthController"]
    USER --> U1["UserController, UserService,<br/>UserRepository, User, Role"]
    SETUP --> SE1["SetupController, AcademicCalendar,<br/>Department, Programme, Section,<br/>AcademicYear, Holiday + repositories"]
    FACULTY --> F1["FacultyController, FacultyRepository,<br/>Faculty, FacultyStatus"]
    COURSE --> CR1["CourseController, OfferingController,<br/>OfferingService, Course, CourseOffering,<br/>OfferingTeacher, TimetableSlot, ClassException + repositories"]
    STUDENT --> S1["StudentController, Student,<br/>Enrolment + repositories"]
    CALENDAR --> CA1["CalendarController, CalendarService,<br/>CalendarEntry"]
    BUDGET --> BU1["BudgetController, BudgetService,<br/>ExpenseService, Budget, Expense + repositories"]
    PROJECT --> P1["ProjectController, ResearchProject,<br/>ProjectMember + repositories"]
    TRAINING --> T1["TrainingController, TrainingService,<br/>TrainingProgram, TrainingClient,<br/>AcceptanceChange + repositories"]
    EVENT --> EV1["EventController, EventRepository, Event"]
    DASH --> D1["DashboardController"]
    SEED --> SD1["DemoAccountsSeeder, DemoDataSeeder, Csv"]
```

### 7. Login and access checks

The password is checked against the BCrypt hash in the database. After that,
every request carries the token, and the user's role is looked up again each
time, so a role change or disabled account takes effect straight away.

```mermaid
sequenceDiagram
    actor User
    participant FE as React frontend
    participant AC as AuthController
    participant DB as UserRepository
    participant JS as JwtService
    participant SF as JwtAuthFilter
    participant SC as Spring Security
    participant API as Controller

    User->>FE: Username and password
    FE->>AC: POST /api/auth/login
    AC->>DB: Find user (must be enabled)
    AC->>AC: Check password (BCrypt)
    AC->>JS: Create token
    JS-->>AC: JWT
    AC-->>FE: Token + user
    FE->>FE: Keep token in localStorage

    User->>FE: Open a page
    FE->>SF: Request with Authorization: Bearer token
    SF->>JS: Read token
    JS-->>SF: Username (or invalid)
    SF->>DB: Load user and current role
    SF->>SC: Mark request as logged in with that role
    SC->>SC: Check role rules (viewer reads, staff writes, admin users/approvals)
    alt Allowed
        SC->>API: Handle request
        API-->>FE: JSON
    else Not logged in or not allowed
        SC-->>FE: 401 or 403 with an error message
    end
```

### 8. One request from click to database

```mermaid
flowchart TD
    A["User action"] --> B["React page"]
    B --> C["api.ts adds the token"]
    C --> E["JwtAuthFilter"]
    E --> F["Spring Security role rules"]
    F --> G["REST controller<br/>(request is validated)"]
    G --> H["Service, if the module has one"]
    G --> I["JPA repository"]
    H --> I
    I --> J[("MySQL")]

    J -. rows .-> I
    I -. entities .-> G
    G -. JSON .-> C
    C -. data or error message .-> B

    G -. exception .-> X["GlobalExceptionHandler<br/>400 / 404 / 409 as ApiError"]
    X -. JSON .-> C
```

### 9. Academic structure

```mermaid
flowchart TB
    DEPT["Department"] --> PROG["Programme<br/>(e.g. B.Tech CSE, 8 semesters)"]
    PROG --> SEC["Section<br/>(e.g. CSE-A)"]
    DEPT --> COURSE["Course"]

    YEAR["Academic year<br/>odd + even semester dates"] --> OFF["Course offering<br/>course + year + semester + section"]
    COURSE --> OFF
    SEC --> OFF

    SEC --> STU["Student<br/>(programme, semester, section)"]
    STU --> ENR["Enrolment"]
    OFF --> ENR
    OFF --> SLOT["Weekly timetable slots"]
    OFF --> EXC["Cancelled / extra classes"]

    HOL["Holidays<br/>(by date)"] -.-> CAL["Calendar"]
    SLOT --> CAL
    EXC --> CAL
    ENR --> CAL
```

### 10. Students and enrolment

```mermaid
flowchart TD
    A["StudentController"]

    A --> B["StudentRepository"]
    A --> C["EnrolmentRepository"]
    A --> D["CourseOfferingRepository"]

    B --> E["Student<br/>roll no, programme, semester, section, status"]
    C --> F["Enrolment<br/>student + offering"]

    A --> G["Whole section:<br/>enrol every active student of a<br/>section + semester into its offerings"]
    A --> H["Single student:<br/>add or remove one course"]
    G --> F
    H --> F

    F --> K["Course offering"]
    K --> L["Course"]
    K --> M["Teachers and weekly times<br/>(shown on the student page)"]
```

### 11. Courses and timetable

```mermaid
flowchart TD
    A["Course management"]

    A --> B["CourseController - catalogue"]
    A --> C["OfferingController"]

    B --> D["CourseRepository"] --> I["Course"]
    C --> E["OfferingService"]

    E --> F["CourseOfferingRepository"] --> J["CourseOffering"]
    E --> H["ClassExceptionRepository"] --> L["ClassException<br/>CANCELLED or EXTRA"]
    E --> W["Clash check on save:<br/>teacher, room, section"]

    J --> K["OfferingTeacher<br/>faculty + THEORY / LAB / TUTORIAL"]
    J --> M["TimetableSlot<br/>day, start, end, room, type"]
    K --> N["Faculty"]
```

### 12. Faculty

```mermaid
flowchart TD
    A["FacultyController"] --> C["FacultyRepository"] --> D["Faculty<br/>employee ID, department, status"]

    A --> CNT["List shows two counts per teacher"]
    CNT --> F["Courses this year<br/>(OfferingTeacherRepository)"]
    CNT --> P["Projects<br/>(ProjectMemberRepository)"]

    F --> G["Course offerings they teach"]
    G --> H["Course"]
    G --> I["Weekly timetable<br/>(teacher's own calendar)"]
    P --> R["Research projects<br/>as PI or co-investigator"]
```

### 13. Training programs

```mermaid
flowchart TD
    A["TrainingController"] --> B["TrainingService"]

    B --> D["TrainingProgramRepository"] --> G["TrainingProgram<br/>coordinator = a teacher"]
    B --> E["TrainingClientRepository"] --> H["TrainingClient<br/>one per client pitched"]
    B --> F["AcceptanceChangeRepository"] --> I["AcceptanceChange<br/>history of 'accepted'"]

    G --> H
    H --> STEPS["Steps in order:<br/>proposal -> accepted (YES / NO / NEEDS_REVISION) -> conducted"]
    H --> I
    STEPS -->|"accepted = YES"| BUD["BudgetService creates a<br/>training budget (budget required)"]
    BUD --> PL["Profit / loss = fee - approved expenses"]
```

### 14. Research projects

```mermaid
flowchart TD
    A["ProjectController"]

    A --> C["ResearchProjectRepository"] --> E["ResearchProject<br/>agency, sanctioned amount, dates, status"]
    A --> D["ProjectMemberRepository"] --> F["ProjectMember<br/>faculty + PI / CO_INVESTIGATOR"]
    E --> F
    A --> B["BudgetService"]
    B --> G["Project budget<br/>created with the project,<br/>allocated = sanctioned amount"]
    G --> H["Utilized = approved expenses<br/>Balance = sanctioned - utilized"]
```

### 15. Budgets and expenses

```mermaid
flowchart TD
    A["BudgetController"]

    A --> C["BudgetService"] --> E["BudgetRepository"] --> G["Budget<br/>allocated, financial year"]
    A --> D["ExpenseService"] --> F["ExpenseRepository"] --> H["Expense<br/>amount, category, status"]

    G --> T["Type"]
    T --> T1["DEPARTMENT - added by hand"]
    T --> T2["EVENT - from the event"]
    T --> T3["TRAINING - when a client accepts"]
    T --> T4["PROJECT - when a project is created"]

    G --> H
    H --> S["PENDING -> APPROVED or REJECTED<br/>(admin only, overspend needs a confirm)"]
    S --> U["Only APPROVED counts as utilized"]
```

### 16. Events and the calendar

These are separate. Events can have a budget. The calendar only shows
classes, worked out on request; nothing calendar-related is stored.

```mermaid
flowchart TD
    subgraph EV["Events"]
        D["EventController"] --> E["EventRepository"] --> F["Event<br/>type, dates, venue, status"]
        D --> B["BudgetService"] --> BG["Event budget<br/>(only if an amount is entered)"]
    end

    subgraph CA["Calendar"]
        H["CalendarController<br/>/api/calendar/student/{id}<br/>/api/calendar/faculty/{id}"] --> I["CalendarService"]
        I --> J["CalendarEntry list<br/>CLASS / CANCELLED / EXTRA"]
    end

    ENR["Enrolments (student)<br/>or teaching (faculty)"] --> I
    SLOT["Timetable slots"] --> I
    SEM["Semester dates<br/>(academic year)"] --> I
    HOL["Holidays - skipped"] --> I
    EXC["Cancelled / extra classes"] --> I
```

### 17. Dashboard

```mermaid
flowchart TD
    A["DashboardPage"] --> B["GET /api/dashboard<br/>DashboardController"]

    B --> C["Active students"]
    B --> D["Active faculty"]
    B --> E["Courses this semester"]
    B --> T["Today's classes<br/>(CalendarService)"]
    B --> F["Training pipeline"]
    B --> G["Project funds used"]
    B --> H["Budgets this financial year"]
    B --> P["Pending approvals"]
    B --> I["Upcoming events"]

    C & D & E & T & F & G & H & P & I --> K["One JSON response"]
    K --> L["Dashboard cards"]
```

### 18. Academic setup

```mermaid
flowchart TD
    A["SetupController"]

    A --> C["DepartmentRepository"] --> H["Department"]
    A --> D["ProgrammeRepository"] --> I["Programme"]
    A --> E["SectionRepository"] --> J["Section"]
    A --> F["AcademicYearRepository"] --> K["AcademicYear"]
    A --> G["HolidayRepository"] --> L["Holiday"]

    H --> I
    I --> J
    K --> M["AcademicCalendar<br/>current year and semester half,<br/>financial year (April-March)"]
    K --> N["Semester dates for offerings<br/>and the calendar"]
    L --> N
```

### 19. Database tables and links

Every table also has `id`, `created_at` and `updated_at`. App users are
separate from faculty and students.

```mermaid
erDiagram
    DEPARTMENT ||--o{ PROGRAMME : offers
    PROGRAMME ||--o{ SECTION : has
    DEPARTMENT ||--o{ COURSE : owns
    DEPARTMENT ||--o{ FACULTY : employs

    PROGRAMME ||--o{ STUDENT : studies
    SECTION ||--o{ STUDENT : groups

    COURSE ||--o{ COURSE_OFFERING : "runs as"
    ACADEMIC_YEAR ||--o{ COURSE_OFFERING : "in"
    SECTION ||--o{ COURSE_OFFERING : "for"

    COURSE_OFFERING ||--o{ OFFERING_TEACHER : "taught by"
    FACULTY ||--o{ OFFERING_TEACHER : teaches
    COURSE_OFFERING ||--o{ TIMETABLE_SLOT : "meets in"
    COURSE_OFFERING ||--o{ CLASS_EXCEPTION : "changed by"
    COURSE_OFFERING ||--o{ ENROLMENT : has
    STUDENT ||--o{ ENROLMENT : "enrolled in"

    RESEARCH_PROJECT ||--o{ PROJECT_MEMBER : team
    FACULTY ||--o{ PROJECT_MEMBER : "works on"

    FACULTY |o--o{ TRAINING_PROGRAM : coordinates
    TRAINING_PROGRAM ||--o{ TRAINING_CLIENT : "pitched to"
    TRAINING_CLIENT ||--o{ ACCEPTANCE_CHANGE : history

    DEPARTMENT |o--o{ BUDGET : "department budget"
    CAMPUS_EVENT |o--o| BUDGET : "event budget"
    RESEARCH_PROJECT |o--o| BUDGET : "project budget"
    TRAINING_CLIENT |o--o| BUDGET : "training budget"
    BUDGET ||--o{ EXPENSE : records

    APP_USER {
        string username
        string role
    }
    HOLIDAY {
        date date
        string name
    }
```

### 20. Startup and sample data

```mermaid
flowchart TD
    A["CampusErpApplication starts"] --> D["application.properties<br/>+ environment variables"]
    D --> E["Connect to MySQL,<br/>create / update tables (JPA)"]
    E --> F["Security config, JWT secret check (32+ chars)"]
    F --> H["Startup runners"]

    H --> I["1. DemoAccountsSeeder"]
    I --> I2{"User table empty?"}
    I2 -->|yes| I3["Create admin, staff, viewer<br/>(app.seed.*-password)"]

    H --> J["2. DemoDataSeeder"]
    J --> J2{"SEED_DEMO_DATA on<br/>and no departments yet?"}
    J2 -->|yes| K["Csv reads sample-data/*.csv"]
    K --> L["Setup -> faculty -> courses and offerings -><br/>students and enrolments -> class changes -><br/>budgets, projects, training, events -> expenses"]

    I3 --> U[("MySQL")]
    L --> U
```
