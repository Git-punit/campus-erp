# Sample data

Made-up data for every module, as CSV files. `DemoDataSeeder` loads all of
them when the database is empty (unless `SEED_DEMO_DATA=false`).

Everything here is fictional: people, companies, projects and phone numbers
(`+91 90000 xxxxx`). Emails use `example.edu` / `example.com`. Festival
holiday dates are approximate.

"Today" in this data is early November 2026, so the current semester is the
odd semester of 2026-27 (20 Jul to 4 Dec 2026).

## Files

Rows refer to each other by code (department code, employee ID, roll number,
course code, offering ref), not by database id.

| File | What's in it | Module |
|---|---|---|
| `departments.csv` | 5 departments | Academic setup |
| `programmes.csv` | 4 B.Tech + 1 M.Tech, with number of semesters | Academic setup |
| `sections.csv` | CSE-A, CSE-B, ECE-A, ME-A, CIVIL-A, MTCSE-A | Academic setup |
| `academic_years.csv` | 2025-26 and 2026-27, odd/even semester dates | Academic setup |
| `holidays.csv` | 13 holidays in 2026-27 | Academic setup |
| `faculty.csv` | 23 teachers (one on leave, one who has left) | Faculty / courses |
| `courses.csv` | 30 courses | Faculty / courses |
| `course_offerings.csv` | 46 offerings for 2026-27 plus 8 from 2025-26 | Faculty / courses |
| `offering_teachers.csv` | who teaches each offering, as THEORY / LAB / TUTORIAL | Faculty / courses |
| `students.csv` | 96 students in semesters 1, 3 and 5 (plus M.Tech sem 1) | Students |
| `enrolments.csv` | which student is in which offering | Students |
| `timetable_slots.csv` | weekly slots: day, start, end, room | Calendar |
| `class_exceptions.csv` | 4 cancelled classes and 2 extra classes | Calendar |
| `department_budgets.csv` | one per department per financial year | Budgets |
| `expenses.csv` | 47 expense requests (approved, pending, rejected) | Budgets |
| `research_projects.csv` | 8 funded projects | Research projects |
| `project_team.csv` | PI and co-investigators for each project | Research projects |
| `training_programs.csv` | 4 programs with a coordinator | Training |
| `training_clients.csv` | 10 clients at different steps | Training |
| `training_acceptance_history.csv` | every change to "accepted" | Training |
| `events.csv` | 8 events with a budget amount (0 = no budget) | Events |

## How the budget references work

Each row in `expenses.csv` has a `budget_type` and a `budget_ref`:

| budget_type | budget_ref | Allocated amount comes from |
|---|---|---|
| DEPARTMENT | `CSE:2026-27` | `department_budgets.csv` |
| PROJECT | `RP-01` | sanctioned amount in `research_projects.csv` |
| EVENT | `EV-02` | budget_amount in `events.csv` |
| TRAINING | `TP-01:Northfield Steel Pvt Ltd` | budget_required in `training_clients.csv` (accepted clients only) |

`raised_by` and `decided_by` are usernames from the demo accounts.

## Cases worth knowing about

These are in the data on purpose, so each screen has something to show.

**Faculty and courses**
- FAC001 (maths) teaches 5 courses this year across CSE-A, ECE-A, ME-A and
  CIVIL-A, and is on 5 research projects. This matches the example in the main
  README.
- FAC001 also has 2025-26 offerings, to try the year filter.
- Several offerings have two teachers (theory + lab), e.g. Programming in C.
- OFF-2526-008 was taught by FAC023, who has since left.

**Timetable and calendar**
- No teacher, room or section is double booked, and no student has two
  classes at once.
- Labs are two-hour blocks in a lab room. Lectures are one hour in the
  section's home room.
- One CSE-A sem 5 student is also enrolled in Machine Learning (CS503) with
  the M.Tech group. That's the "single student" enrolment case.
- Cancelled classes fall on real class days in the week of 16 Nov 2026.
  Extra classes are on Saturdays 21 and 28 Nov.
- Diwali (9 and 10 Nov) falls inside the current semester, so those days
  should show no classes.
- One student is DROPPED and one is ON_BREAK. Neither is enrolled in
  anything.

**Research projects** (utilized = approved expenses)
- RP-01: 5,00,000 sanctioned, 1,20,000 used, plus 30,000 pending.
- RP-02: fully used (3,00,000 of 3,00,000).
- RP-05: nothing spent yet.
- RP-06: over budget (4,80,000 of 4,50,000), so the bar should go red.
- RP-03 has a rejected request with a remark.

**Training programs**
- Every step is covered: not yet proposed, proposal sent with no answer,
  needs revision, rejected, accepted but not run yet, and conducted.
- Northfield Steel went "needs revision" then "yes". Both entries are in the
  history.
- Sunrise Infra Builders ran at a loss: fee 40,000, cost 47,000.

**Budgets**
- ME 2026-27 has 40,000 left and an 80,000 pending request, so approving it
  should show the overspend warning.
- TechFest (EV-02) went 15,000 over its budget.
- Department budgets for 2025-26 are there for the financial year filter.
