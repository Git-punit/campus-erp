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
and the dashboard.

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
