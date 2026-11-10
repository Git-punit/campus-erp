package com.campuserp.calendar;

import com.campuserp.common.BusinessRuleException;
import com.campuserp.common.Finder;
import com.campuserp.course.ClassException;
import com.campuserp.course.ClassExceptionRepository;
import com.campuserp.course.CourseOffering;
import com.campuserp.course.CourseOfferingRepository;
import com.campuserp.course.ExceptionType;
import com.campuserp.course.OfferingTeacher;
import com.campuserp.course.TimetableSlot;
import com.campuserp.faculty.FacultyRepository;
import com.campuserp.setup.AcademicYear;
import com.campuserp.setup.Holiday;
import com.campuserp.setup.HolidayRepository;
import com.campuserp.student.EnrolmentRepository;
import com.campuserp.student.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Builds calendars from enrolments and timetables. Nothing is stored: every
 * date is worked out from the weekly slots, the semester dates, holidays and
 * cancelled / extra classes.
 */
@Service
@Transactional(readOnly = true)
public class CalendarService {

    public record HolidayView(LocalDate date, String name) {}

    public record CalendarView(LocalDate from, LocalDate to, List<CalendarEntry> entries, List<HolidayView> holidays) {}

    private static final int MAX_DAYS = 120;

    private final EnrolmentRepository enrolments;
    private final CourseOfferingRepository offerings;
    private final ClassExceptionRepository exceptions;
    private final HolidayRepository holidays;
    private final StudentRepository students;
    private final FacultyRepository faculty;

    public CalendarService(EnrolmentRepository enrolments, CourseOfferingRepository offerings,
                           ClassExceptionRepository exceptions, HolidayRepository holidays,
                           StudentRepository students, FacultyRepository faculty) {
        this.enrolments = enrolments;
        this.offerings = offerings;
        this.exceptions = exceptions;
        this.holidays = holidays;
        this.students = students;
        this.faculty = faculty;
    }

    public CalendarView forStudent(Long studentId, LocalDate from, LocalDate to) {
        Finder.find(students, studentId, "Student");
        return build(enrolments.findOfferingsOfStudent(studentId, null), slot -> true, from, to);
    }

    /** A teacher only sees the classes they take (e.g. the lab teacher sees lab slots). */
    public CalendarView forFaculty(Long facultyId, LocalDate from, LocalDate to) {
        Finder.find(faculty, facultyId, "Faculty");
        List<CourseOffering> taught = offerings.findByTeacher(facultyId, null);
        Predicate<TimetableSlot> mine = slot -> slot.getOffering().teachersFor(slot.getType()).stream()
                .anyMatch(t -> t.getFaculty().getId().equals(facultyId));
        return build(taught, mine, from, to);
    }

    /** Everything on campus, used for "today's classes" on the dashboard. */
    public CalendarView forEveryone(LocalDate from, LocalDate to) {
        return build(offerings.findInYearsOverlapping(from, to), slot -> true, from, to);
    }

    CalendarView build(List<CourseOffering> offeringList, Predicate<TimetableSlot> include, LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new BusinessRuleException("'to' must not be before 'from'");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_DAYS) {
            throw new BusinessRuleException("Ask for at most " + MAX_DAYS + " days at a time");
        }
        List<Holiday> holidayList = holidays.findByDateBetweenOrderByDateAsc(from, to);
        Set<LocalDate> holidayDates = holidayList.stream().map(Holiday::getDate).collect(Collectors.toSet());

        List<Long> ids = offeringList.stream().map(CourseOffering::getId).toList();
        Map<Long, List<ClassException>> changes = ids.isEmpty() ? Map.of()
                : exceptions.findByOfferingIdInAndDateBetween(ids, from, to).stream()
                .collect(Collectors.groupingBy(e -> e.getOffering().getId()));

        List<CalendarEntry> out = new ArrayList<>();
        for (CourseOffering o : offeringList) {
            AcademicYear y = o.getAcademicYear();
            LocalDate start = max(from, y.semesterStart(o.getSemester()));
            LocalDate end = min(to, y.semesterEnd(o.getSemester()));
            List<ClassException> mine = changes.getOrDefault(o.getId(), List.of());

            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                if (holidayDates.contains(d)) {
                    continue;
                }
                for (TimetableSlot s : o.getSlots()) {
                    if (s.getDay() != d.getDayOfWeek() || !include.test(s)) {
                        continue;
                    }
                    LocalDate date = d;
                    ClassException cancelled = mine.stream()
                            .filter(e -> e.getType() == ExceptionType.CANCELLED && e.getDate().equals(date)
                                    && e.getStartTime().equals(s.getStartTime()))
                            .findFirst().orElse(null);
                    out.add(entry(o, d, s.getStartTime(), s.getEndTime(), s.getRoom(), teachersFor(o, s),
                            cancelled == null ? "CLASS" : "CANCELLED",
                            cancelled == null ? null : cancelled.getReason()));
                }
            }
            for (ClassException e : mine) {
                if (e.getType() == ExceptionType.EXTRA) {
                    out.add(entry(o, e.getDate(), e.getStartTime(), e.getEndTime(), e.getRoom(),
                            names(o.getTeachers()), "EXTRA", e.getReason()));
                }
            }
        }
        out.sort(Comparator.comparing(CalendarEntry::date).thenComparing(CalendarEntry::startTime));
        List<HolidayView> hv = holidayList.stream().map(h -> new HolidayView(h.getDate(), h.getName())).toList();
        return new CalendarView(from, to, out, hv);
    }

    private static List<String> teachersFor(CourseOffering o, TimetableSlot s) {
        return names(o.teachersFor(s.getType()));
    }

    private static List<String> names(List<OfferingTeacher> ts) {
        return ts.stream().map(t -> t.getFaculty().getName()).distinct().toList();
    }

    private static CalendarEntry entry(CourseOffering o, LocalDate d, java.time.LocalTime start, java.time.LocalTime end,
                                       String room, List<String> teachers, String status, String note) {
        return new CalendarEntry(d, start, end, o.getId(), o.getCourse().ref(), o.getSection().ref(), o.getSemester(),
                room, teachers, status, note);
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }
}
