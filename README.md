# campus-erp

ERP for running a college campus. It keeps track of faculty, students,
courses, research projects, training programs, events and budgets, and has a
dashboard on top so you can see what's going on without opening every page.

Backend is Spring Boot 4.1 on Java 25 (JPA, Spring Security + JWT, MySQL).
Frontend is React 19 + TypeScript with Vite, in the `frontend` folder.

Fee management, attendance and library were in an older version and have been
dropped for now.

## 1. Overall system architecture

```mermaid
flowchart TB
    USERS["Campus ERP Users<br/>Admin | Faculty | Students"]

    subgraph FE["Frontend – React + TypeScript"]
        APP["React Application"]
        ROUTES["Application Routes"]
        PAGES["Pages"]
        COMPONENTS["Reusable Components"]
        API["API Client"]
        AUTH["Authentication Context"]
        APP --> ROUTES
        ROUTES --> PAGES
        PAGES --> COMPONENTS
        PAGES --> API
        AUTH --> API
    end

    subgraph BE["Backend – Spring Boot"]
        FILTER["JWT Authentication Filter"]
        SECURITY["Spring Security"]
        CONTROLLERS["REST Controllers"]
        SERVICES["Business Services"]
        REPOSITORIES["JPA Repositories"]
        FILTER --> SECURITY
        SECURITY --> CONTROLLERS
        CONTROLLERS --> SERVICES
        SERVICES --> REPOSITORIES
    end

    subgraph MODULES["Business Modules"]
        STUDENT["Student"]
        FACULTY["Faculty"]
        COURSE["Course"]
        CALENDAR["Calendar"]
        TRAINING["Training"]
        PROJECT["Research Project"]
        BUDGET["Budget"]
        EVENT["Events"]
        SETUP["Academic Setup"]
        USER["User Management"]
        DASH["Dashboard"]
    end

    DB[("MySQL Database")]

    USERS --> APP
    API <-->|"REST / JSON"| FILTER
    SERVICES --> MODULES
    REPOSITORIES --> DB
```

## 2. Complete project directory structure

Generated frontend bundles, `node_modules`, and Maven build output are omitted
from this source tree because they are generated locally and are ignored by
Git.

```text
campus-erp/
├── .mvn/
│   └── wrapper/
│       └── maven-wrapper.properties
├── .vscode/
│   └── tasks.json
├── frontend/
│   ├── index.html
│   ├── package.json
│   ├── package-lock.json
│   ├── tsconfig.json
│   ├── vite.config.ts
│   └── src/
│       ├── App.tsx
│       ├── api.ts
│       ├── auth.tsx
│       ├── main.tsx
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
│   │   │   ├── auth/
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── JwtAuthFilter.java
│   │   │   │   └── JwtService.java
│   │   │   ├── budget/
│   │   │   │   ├── Budget.java
│   │   │   │   ├── BudgetController.java
│   │   │   │   ├── BudgetDtos.java
│   │   │   │   ├── BudgetRepository.java
│   │   │   │   ├── BudgetService.java
│   │   │   │   ├── BudgetType.java
│   │   │   │   ├── Expense.java
│   │   │   │   ├── ExpenseCategory.java
│   │   │   │   ├── ExpenseRepository.java
│   │   │   │   ├── ExpenseService.java
│   │   │   │   └── ExpenseStatus.java
│   │   │   ├── calendar/
│   │   │   │   ├── CalendarController.java
│   │   │   │   ├── CalendarEntry.java
│   │   │   │   └── CalendarService.java
│   │   │   ├── common/
│   │   │   │   ├── ApiError.java
│   │   │   │   ├── BaseEntity.java
│   │   │   │   ├── BusinessRuleException.java
│   │   │   │   ├── Finder.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── HealthController.java
│   │   │   │   ├── Money.java
│   │   │   │   ├── NotFoundException.java
│   │   │   │   └── Ref.java
│   │   │   ├── config/
│   │   │   │   ├── ClockConfig.java
│   │   │   │   └── SecurityConfig.java
│   │   │   ├── course/
│   │   │   │   ├── ClassException.java
│   │   │   │   ├── ClassExceptionRepository.java
│   │   │   │   ├── Course.java
│   │   │   │   ├── CourseController.java
│   │   │   │   ├── CourseOffering.java
│   │   │   │   ├── CourseOfferingRepository.java
│   │   │   │   ├── CourseRepository.java
│   │   │   │   ├── ExceptionType.java
│   │   │   │   ├── OfferingController.java
│   │   │   │   ├── OfferingDtos.java
│   │   │   │   ├── OfferingService.java
│   │   │   │   ├── OfferingTeacher.java
│   │   │   │   ├── OfferingTeacherRepository.java
│   │   │   │   ├── TeachingRole.java
│   │   │   │   └── TimetableSlot.java
│   │   │   ├── dashboard/
│   │   │   │   └── DashboardController.java
│   │   │   ├── event/
│   │   │   │   ├── Event.java
│   │   │   │   ├── EventController.java
│   │   │   │   ├── EventRepository.java
│   │   │   │   └── EventStatus.java
│   │   │   ├── faculty/
│   │   │   │   ├── Faculty.java
│   │   │   │   ├── FacultyController.java
│   │   │   │   ├── FacultyRepository.java
│   │   │   │   └── FacultyStatus.java
│   │   │   ├── project/
│   │   │   │   ├── MemberRole.java
│   │   │   │   ├── ProjectController.java
│   │   │   │   ├── ProjectMember.java
│   │   │   │   ├── ProjectMemberRepository.java
│   │   │   │   ├── ProjectStatus.java
│   │   │   │   ├── ResearchProject.java
│   │   │   │   └── ResearchProjectRepository.java
│   │   │   ├── seed/
│   │   │   │   ├── Csv.java
│   │   │   │   ├── DemoAccountsSeeder.java
│   │   │   │   └── DemoDataSeeder.java
│   │   │   ├── setup/
│   │   │   │   ├── AcademicCalendar.java
│   │   │   │   ├── AcademicYear.java
│   │   │   │   ├── AcademicYearRepository.java
│   │   │   │   ├── Department.java
│   │   │   │   ├── DepartmentRepository.java
│   │   │   │   ├── Holiday.java
│   │   │   │   ├── HolidayRepository.java
│   │   │   │   ├── Programme.java
│   │   │   │   ├── ProgrammeRepository.java
│   │   │   │   ├── Section.java
│   │   │   │   ├── SectionRepository.java
│   │   │   │   └── SetupController.java
│   │   │   ├── student/
│   │   │   │   ├── Enrolment.java
│   │   │   │   ├── EnrolmentRepository.java
│   │   │   │   ├── Student.java
│   │   │   │   ├── StudentController.java
│   │   │   │   ├── StudentRepository.java
│   │   │   │   └── StudentStatus.java
│   │   │   ├── training/
│   │   │   │   ├── Acceptance.java
│   │   │   │   ├── AcceptanceChange.java
│   │   │   │   ├── AcceptanceChangeRepository.java
│   │   │   │   ├── TrainingClient.java
│   │   │   │   ├── TrainingClientRepository.java
│   │   │   │   ├── TrainingController.java
│   │   │   │   ├── TrainingDtos.java
│   │   │   │   ├── TrainingProgram.java
│   │   │   │   ├── TrainingProgramRepository.java
│   │   │   │   └── TrainingService.java
│   │   │   └── user/
│   │   │       ├── Role.java
│   │   │       ├── User.java
│   │   │       ├── UserController.java
│   │   │       ├── UserRepository.java
│   │   │       ├── UserRequests.java
│   │   │       ├── UserResponse.java
│   │   │       └── UserService.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── sample-data/
│   │           ├── academic_years.csv
│   │           ├── class_exceptions.csv
│   │           ├── course_offerings.csv
│   │           ├── courses.csv
│   │           ├── department_budgets.csv
│   │           ├── departments.csv
│   │           ├── enrolments.csv
│   │           ├── events.csv
│   │           ├── faculty.csv
│   │           ├── holidays.csv
│   │           ├── offering_teachers.csv
│   │           ├── programmes.csv
│   │           ├── project_team.csv
│   │           ├── research_projects.csv
│   │           ├── sections.csv
│   │           ├── students.csv
│   │           ├── training_acceptance_history.csv
│   │           ├── training_clients.csv
│   │           ├── training_programs.csv
│   │           ├── timetable_slots.csv
│   │           └── README.md
│   └── test/
│       ├── java/com/campuserp/
│       │   ├── ApiTest.java
│       │   ├── CampusErpApplicationTests.java
│       │   ├── Fixtures.java
│       │   ├── TestClock.java
│       │   ├── TestLogins.java
│       │   ├── budget/BudgetAndExpenseTest.java
│       │   ├── calendar/CalendarTest.java
│       │   ├── common/HealthAndErrorsTest.java
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
├── mvnw
├── mvnw.cmd
├── package-lock.json
└── pom.xml
```

## 3. Frontend architecture

The frontend follows a component-based React architecture.

```mermaid
flowchart TD
    MAIN["main.tsx"]
    APP["App.tsx"]
    AUTH["auth.tsx"]
    LAYOUT["Layout.tsx"]

    MAIN --> APP
    APP --> AUTH
    AUTH --> LAYOUT

    LAYOUT --> PAGES["Pages"]
    PAGES --> DASH["Dashboard"]
    PAGES --> STUDENT["Students"]
    PAGES --> FACULTY["Faculty"]
    PAGES --> COURSE["Courses"]
    PAGES --> TRAINING["Training"]
    PAGES --> PROJECT["Projects"]
    PAGES --> BUDGET["Budgets"]
    PAGES --> EVENT["Events"]
    PAGES --> CALENDAR["Calendar"]
    PAGES --> SETUP["Setup"]
    PAGES --> USERS["Users"]

    PAGES --> COMPONENTS["Reusable Components"]
    COMPONENTS --> CRUD["CrudSection"]
    COMPONENTS --> CALVIEW["CalendarView"]
    COMPONENTS --> EXPENSE["ExpenseTable"]
    COMPONENTS --> UI["UI Components"]

    PAGES --> LIB["Utility Library"]
    LIB --> TYPES["types.ts"]
    LIB --> FORMAT["format.ts"]
    LIB --> LOAD["useLoad.ts"]

    PAGES --> API["api.ts"]
    API --> SERVER["Spring Boot REST API"]
```

## 4. Frontend component and page structure

```mermaid
flowchart TB
    A["Frontend Application"]

    A --> B["Authentication"]
    A --> C["Layout"]
    A --> D["Pages"]
    A --> E["Shared Components"]
    A --> F["Utilities"]
    A --> G["API Integration"]

    B --> B1["auth.tsx"]
    B1 --> B2["LoginPage.tsx"]

    C --> C1["Layout.tsx"]

    D --> D1["DashboardPage"]
    D --> D2["Student Pages"]
    D --> D3["Faculty Pages"]
    D --> D4["CoursesPage"]
    D --> D5["Training Pages"]
    D --> D6["ProjectsPage"]
    D --> D7["Budget Pages"]
    D --> D8["EventsPage"]
    D --> D9["CalendarPage"]
    D --> D10["SetupPage"]
    D --> D11["UsersPage"]

    E --> E1["CrudSection"]
    E --> E2["CalendarView"]
    E --> E3["ExpenseTable"]
    E --> E4["ui.tsx"]

    F --> F1["types.ts"]
    F --> F2["format.ts"]
    F --> F3["useLoad.ts"]

    G --> G1["api.ts"]
    G1 --> G2["HTTP Requests"]
```

## 5. Backend architecture

```mermaid
flowchart TB
    A["CampusErpApplication.java"]

    A --> B["Configuration"]
    A --> C["Authentication"]
    A --> D["Business Modules"]
    A --> E["Common Infrastructure"]
    A --> F["Data Initialization"]

    B --> B1["SecurityConfig"]
    B --> B2["ClockConfig"]

    C --> C1["AuthController"]
    C --> C2["JwtService"]
    C --> C3["JwtAuthFilter"]

    D --> D1["Controllers"]
    D1 --> D2["Services"]
    D2 --> D3["Repositories"]
    D3 --> D4["Entities"]

    E --> E1["BaseEntity"]
    E --> E2["Finder"]
    E --> E3["Money"]
    E --> E4["Exception Handler"]
    E --> E5["API Error"]

    F --> F1["DemoAccountsSeeder"]
    F --> F2["DemoDataSeeder"]
    F --> F3["CSV Utility"]

    D4 --> DB[("MySQL")]
```

## 6. Backend package/module structure

```mermaid
flowchart TD
    ROOT["com.campuserp"]

    ROOT --> AUTH["auth"]
    ROOT --> CONFIG["config"]
    ROOT --> COMMON["common"]
    ROOT --> USER["user"]
    ROOT --> STUDENT["student"]
    ROOT --> FACULTY["faculty"]
    ROOT --> COURSE["course"]
    ROOT --> CALENDAR["calendar"]
    ROOT --> TRAINING["training"]
    ROOT --> PROJECT["project"]
    ROOT --> BUDGET["budget"]
    ROOT --> EVENT["event"]
    ROOT --> SETUP["setup"]
    ROOT --> DASH["dashboard"]
    ROOT --> SEED["seed"]

    AUTH --> A1["AuthController"]
    AUTH --> A2["JwtService"]
    AUTH --> A3["JwtAuthFilter"]

    CONFIG --> C1["SecurityConfig"]
    CONFIG --> C2["ClockConfig"]

    COMMON --> CM1["BaseEntity"]
    COMMON --> CM2["GlobalExceptionHandler"]
    COMMON --> CM3["Finder / Ref / Money"]

    USER --> U1["UserController"]
    USER --> U2["UserService"]
    USER --> U3["UserRepository"]
    USER --> U4["User / Role"]

    STUDENT --> S1["StudentController"]
    STUDENT --> S2["StudentRepository"]
    STUDENT --> S3["EnrolmentRepository"]
    STUDENT --> S4["Student / Enrolment"]

    COURSE --> CR1["CourseController"]
    COURSE --> CR2["OfferingController"]
    COURSE --> CR3["OfferingService"]
    COURSE --> CR4["Course Repositories"]

    CALENDAR --> CA1["CalendarController"]
    CALENDAR --> CA2["CalendarService"]
    CALENDAR --> CA3["CalendarEntry"]

    TRAINING --> T1["TrainingController"]
    TRAINING --> T2["TrainingService"]
    TRAINING --> T3["Training Repositories"]

    PROJECT --> P1["ProjectController"]
    PROJECT --> P2["ResearchProjectRepository"]
    PROJECT --> P3["ProjectMemberRepository"]

    BUDGET --> BU1["BudgetController"]
    BUDGET --> BU2["BudgetService"]
    BUDGET --> BU3["ExpenseService"]
    BUDGET --> BU4["Repositories"]

    EVENT --> EV1["EventController"]
    EVENT --> EV2["EventRepository"]

    SETUP --> SE1["SetupController"]
    SETUP --> SE2["Academic Setup Repositories"]

    DASH --> D1["DashboardController"]

    SEED --> SD1["DemoDataSeeder"]
    SEED --> SD2["DemoAccountsSeeder"]
```

## 7. Authentication and authorization flow

This diagram explains the JWT-based security architecture.

```mermaid
sequenceDiagram
    actor User
    participant FE as React Frontend
    participant AC as AuthController
    participant JS as JwtService
    participant SF as JwtAuthFilter
    participant SC as Spring Security
    participant API as REST API

    User->>FE: Enter login credentials
    FE->>AC: Authentication request
    AC->>JS: Generate JWT
    JS-->>AC: JWT token
    AC-->>FE: Authentication response
    FE->>FE: Store authentication state

    User->>FE: Request protected resource
    FE->>SF: Request with Bearer token
    SF->>JS: Validate JWT
    JS-->>SF: Token validation result
    SF->>SC: Set authentication context
    SC->>API: Authorize request
    API-->>FE: Response
    FE-->>User: Display requested data
```

## 8. REST API request lifecycle

```mermaid
flowchart TD
    A["User Action"]
    B["React Page"]
    C["api.ts"]
    D["HTTP Request"]
    E["JWT Filter"]
    F["Spring Security"]
    G["REST Controller"]
    H["Business Service"]
    I["JPA Repository"]
    J[("MySQL Database")]

    A --> B --> C --> D
    D --> E --> F --> G
    G --> H --> I --> J

    J --> I
    I --> H
    H --> G
    G --> D
    D --> C
    C --> B
```

## 9. Academic management module

This diagram shows how the main academic areas fit together.

```mermaid
flowchart TB
    A["Academic Management"]

    A --> B["Departments"]
    A --> C["Programmes"]
    A --> D["Academic Years"]
    A --> E["Sections"]
    A --> F["Courses"]
    A --> G["Course Offerings"]
    A --> H["Enrolments"]
    A --> I["Timetable Slots"]
    A --> J["Holidays"]

    B --> C
    C --> E
    D --> G
    F --> G
    G --> H
    G --> I
    D --> J
```

## 10. Student and enrolment module

```mermaid
flowchart TD
    A["Student Management"]

    A --> B["StudentController"]
    B --> C["StudentRepository"]
    B --> D["EnrolmentRepository"]

    C --> E["Student Entity"]
    D --> F["Enrolment Entity"]

    E --> G["Student Information"]
    E --> H["Student Status"]
    F --> I["Course Enrolment"]
    F --> J["Academic Records"]

    I --> K["Course Offering"]
    K --> L["Course"]
```

## 11. Course and timetable module

```mermaid
flowchart TD
    A["Course Management"]

    A --> B["CourseController"]
    A --> C["OfferingController"]

    B --> D["Course Repository"]
    C --> E["OfferingService"]

    E --> F["CourseOfferingRepository"]
    E --> G["OfferingTeacherRepository"]
    E --> H["ClassExceptionRepository"]

    D --> I["Course"]
    F --> J["CourseOffering"]
    G --> K["OfferingTeacher"]
    H --> L["ClassException"]

    J --> M["TimetableSlot"]
    K --> N["Faculty / Teaching Role"]
    L --> O["Exception Type"]
```

## 12. Faculty management module

```mermaid
flowchart TD
    A["Faculty Management"]
    A --> B["FacultyController"]
    B --> C["FacultyRepository"]

    C --> D["Faculty Entity"]
    D --> E["Faculty Status"]

    D --> F["Teaching Assignments"]
    F --> G["Course Offering"]
    G --> H["Course"]
    G --> I["Timetable"]
```

## 13. Training management module

```mermaid
flowchart TD
    A["Training Management"]

    A --> B["TrainingController"]
    B --> C["TrainingService"]

    C --> D["TrainingProgramRepository"]
    C --> E["TrainingClientRepository"]
    C --> F["AcceptanceChangeRepository"]

    D --> G["TrainingProgram"]
    E --> H["TrainingClient"]
    F --> I["AcceptanceChange"]

    G --> J["Acceptance"]
    J --> K["Training Acceptance History"]
```

## 14. Research project management module

```mermaid
flowchart TD
    A["Research Project Management"]

    A --> B["ProjectController"]
    B --> C["ResearchProjectRepository"]
    B --> D["ProjectMemberRepository"]

    C --> E["ResearchProject"]
    D --> F["ProjectMember"]

    E --> G["Project Status"]
    F --> H["Member Role"]
    F --> I["Project Team"]
```

## 15. Budget and expense management module

```mermaid
flowchart TD
    A["Budget Management"]

    A --> B["BudgetController"]
    B --> C["BudgetService"]
    B --> D["ExpenseService"]

    C --> E["BudgetRepository"]
    D --> F["ExpenseRepository"]

    E --> G["Budget Entity"]
    F --> H["Expense Entity"]

    G --> I["Budget Type"]
    H --> J["Expense Category"]
    H --> K["Expense Status"]

    G --> L["Department Budget"]
    L --> H
```

## 16. Events and calendar module

```mermaid
flowchart TD
    A["Events & Calendar"]

    A --> B["Event Management"]
    A --> C["Calendar Management"]

    B --> D["EventController"]
    D --> E["EventRepository"]
    E --> F["Event Entity"]
    F --> G["Event Status"]

    C --> H["CalendarController"]
    H --> I["CalendarService"]
    I --> J["CalendarEntry"]

    K["Academic Setup"] --> L["Holidays"]
    K --> M["Academic Calendar"]
    L --> J
    M --> J
```

## 17. Dashboard and reporting module

```mermaid
flowchart TD
    A["Dashboard Page"]
    A --> B["DashboardController"]

    B --> C["Student Data"]
    B --> D["Faculty Data"]
    B --> E["Course Data"]
    B --> F["Training Data"]
    B --> G["Project Data"]
    B --> H["Budget Data"]
    B --> I["Event Data"]

    C --> J["Dashboard Metrics"]
    D --> J
    E --> J
    F --> J
    G --> J
    H --> J
    I --> J

    J --> K["Dashboard API Response"]
    K --> L["React Dashboard"]
```

## 18. Academic setup module

```mermaid
flowchart TD
    A["Academic Setup"]

    A --> B["SetupController"]

    B --> C["DepartmentRepository"]
    B --> D["ProgrammeRepository"]
    B --> E["SectionRepository"]
    B --> F["AcademicYearRepository"]
    B --> G["HolidayRepository"]

    C --> H["Department"]
    D --> I["Programme"]
    E --> J["Section"]
    F --> K["AcademicYear"]
    G --> L["Holiday"]

    H --> I
    I --> J
    K --> M["Academic Scheduling"]
    L --> M
```

## 19. Database entity relationship overview

The following is a conceptual domain relationship diagram based on the entity
classes in this project. It is not a substitute for the exact MySQL
foreign-key schema.

```mermaid
erDiagram
    USER ||--o| STUDENT : associated_with
    USER ||--o| FACULTY : associated_with

    DEPARTMENT ||--o{ STUDENT : contains
    DEPARTMENT ||--o{ FACULTY : contains
    DEPARTMENT ||--o{ PROGRAMME : offers

    PROGRAMME ||--o{ SECTION : organizes
    ACADEMIC_YEAR ||--o{ COURSE_OFFERING : schedules

    COURSE ||--o{ COURSE_OFFERING : offered_as
    COURSE_OFFERING ||--o{ ENROLMENT : receives
    STUDENT ||--o{ ENROLMENT : registers

    COURSE_OFFERING ||--o{ OFFERING_TEACHER : assigned
    FACULTY ||--o{ OFFERING_TEACHER : teaches

    COURSE_OFFERING ||--o{ TIMETABLE_SLOT : schedules
    COURSE_OFFERING ||--o{ CLASS_EXCEPTION : modifies

    RESEARCH_PROJECT ||--o{ PROJECT_MEMBER : includes
    TRAINING_PROGRAM ||--o{ ACCEPTANCE : tracks
    TRAINING_CLIENT ||--o{ TRAINING_PROGRAM : provides

    DEPARTMENT ||--o{ BUDGET : allocated
    BUDGET ||--o{ EXPENSE : records

    EVENT ||--o{ CALENDAR_ENTRY : represented_by
```

## 20. Application startup and sample data initialization

```mermaid
flowchart TD
    A["Application Startup"]
    A --> B["CampusErpApplication.java"]
    B --> C["Spring Boot Initialization"]
    C --> D["Load application.properties"]
    D --> E["Configure Database"]
    E --> F["Configure Spring Security"]
    F --> G["Initialize JPA"]
    G --> H["Seeder Components"]

    H --> I["DemoAccountsSeeder"]
    H --> J["DemoDataSeeder"]

    J --> K["CSV Utility"]
    K --> L["Read sample-data CSV files"]

    L --> M["Students"]
    L --> N["Faculty"]
    L --> O["Courses"]
    L --> P["Departments"]
    L --> Q["Training Programs"]
    L --> R["Research Projects"]
    L --> S["Budgets & Expenses"]
    L --> T["Events & Timetable"]

    I --> U[("Database")]
    M --> U
    N --> U
    O --> U
    P --> U
    Q --> U
    R --> U
    S --> U
    T --> U
```

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

Needs Java 25+, Node 20+ and MySQL 8.

Set your MySQL login (the `campus_erp` database gets created on first run):

```bash
export DB_USERNAME=root
export DB_PASSWORD='your-mysql-password'
```

Backend, on port 8080:

```bash
./mvnw spring-boot:run
```

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

Don't deploy this with the default passwords or JWT secret. Change the
passwords from the Users page and set `JWT_SECRET` (at least 32 chars).

### One jar

`npm run build` puts the React build into `src/main/resources/static/`, so
Spring Boot serves both the UI and the API:

```bash
cd frontend && npm run build && cd .. && ./mvnw package
```

```bash
java -jar target/campus-erp-0.0.1-SNAPSHOT.jar
```

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

## Config

| Variable | Default | What it's for |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/campus_erp?createDatabaseIfNotExist=true` | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / empty | MySQL login |
| `JWT_SECRET` | dev value | signs login tokens, change it outside dev |
| `SEED_DEMO_DATA` | `true` | load sample data into an empty db |
| `SERVER_PORT` | `8080` | HTTP port |

Other business settings (like the financial year starting in April) are under
`app.*` in `application.properties`.
