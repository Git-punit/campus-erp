package com.campuserp.course;

import com.campuserp.common.BusinessRuleException;
import com.campuserp.common.Finder;
import com.campuserp.course.OfferingDtos.ExceptionRequest;
import com.campuserp.course.OfferingDtos.OfferingRequest;
import com.campuserp.course.OfferingDtos.OfferingView;
import com.campuserp.course.OfferingDtos.SaveResult;
import com.campuserp.course.OfferingDtos.SlotInput;
import com.campuserp.course.OfferingDtos.SlotView;
import com.campuserp.course.OfferingDtos.TeacherInput;
import com.campuserp.course.OfferingDtos.TeacherView;
import com.campuserp.faculty.Faculty;
import com.campuserp.faculty.FacultyRepository;
import com.campuserp.setup.AcademicYear;
import com.campuserp.setup.AcademicYearRepository;
import com.campuserp.setup.HolidayRepository;
import com.campuserp.setup.Section;
import com.campuserp.setup.SectionRepository;
import com.campuserp.student.EnrolmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class OfferingService {

    private final CourseOfferingRepository offerings;
    private final CourseRepository courses;
    private final AcademicYearRepository years;
    private final SectionRepository sections;
    private final FacultyRepository faculty;
    private final ClassExceptionRepository exceptions;
    private final HolidayRepository holidays;
    private final EnrolmentRepository enrolments;

    public OfferingService(CourseOfferingRepository offerings, CourseRepository courses, AcademicYearRepository years,
                           SectionRepository sections, FacultyRepository faculty, ClassExceptionRepository exceptions,
                           HolidayRepository holidays, EnrolmentRepository enrolments) {
        this.offerings = offerings;
        this.courses = courses;
        this.years = years;
        this.sections = sections;
        this.faculty = faculty;
        this.exceptions = exceptions;
        this.holidays = holidays;
        this.enrolments = enrolments;
    }

    // ---------- reading

    @Transactional(readOnly = true)
    public List<OfferingView> search(String year, Integer semester, Long sectionId) {
        return offerings.search(blank(year), semester, sectionId).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public List<OfferingView> taughtBy(Long facultyId, String year) {
        Finder.find(faculty, facultyId, "Faculty");
        return offerings.findByTeacher(facultyId, blank(year)).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public OfferingView get(Long id) {
        return view(Finder.find(offerings, id, "Course offering"));
    }

    // ---------- writing

    public SaveResult create(OfferingRequest req) {
        CourseOffering o = new CourseOffering();
        apply(o, req);
        offerings.save(o);
        return new SaveResult(view(o), clashes(o));
    }

    public SaveResult update(Long id, OfferingRequest req) {
        CourseOffering o = Finder.find(offerings, id, "Course offering");
        apply(o, req);
        offerings.flush();
        return new SaveResult(view(o), clashes(o));
    }

    public void delete(Long id) {
        CourseOffering o = Finder.find(offerings, id, "Course offering");
        if (enrolments.countByOfferingId(id) > 0) {
            throw new BusinessRuleException("Students are enrolled in " + o.label() + ". Remove them first.");
        }
        exceptions.deleteByOfferingId(id);
        offerings.delete(o);
        offerings.flush();
    }

    private void apply(CourseOffering o, OfferingRequest req) {
        Course course = Finder.find(courses, req.courseId(), "Course");
        AcademicYear year = Finder.find(years, req.academicYearId(), "Academic year");
        Section section = Finder.find(sections, req.sectionId(), "Section");
        if (req.semester() > section.getProgramme().getSemesters()) {
            throw new BusinessRuleException(section.getProgramme().getCode() + " only has "
                    + section.getProgramme().getSemesters() + " semesters");
        }
        offerings.findByCourseIdAndAcademicYearIdAndSemesterAndSectionId(course.getId(), year.getId(),
                        req.semester(), section.getId())
                .filter(other -> !other.getId().equals(o.getId()))
                .ifPresent(other -> {
                    throw new BusinessRuleException(course.getCode() + " is already offered to Sem " + req.semester()
                            + " " + section.getCode() + " in " + year.getName());
                });
        o.setCourse(course);
        o.setAcademicYear(year);
        o.setSemester(req.semester());
        o.setSection(section);

        // teachers: replace the list
        Set<String> seen = new HashSet<>();
        o.getTeachers().clear();
        for (TeacherInput t : req.teachers()) {
            TeachingRole role = t.role() == null ? TeachingRole.THEORY : t.role();
            if (!seen.add(t.facultyId() + ":" + role)) {
                continue;
            }
            Faculty f = Finder.find(faculty, t.facultyId(), "Faculty");
            o.getTeachers().add(new OfferingTeacher(o, f, role));
        }

        // timetable: replace the list
        o.getSlots().clear();
        if (req.slots() != null) {
            for (SlotInput s : req.slots()) {
                if (!s.startTime().isBefore(s.endTime())) {
                    throw new BusinessRuleException("A class must end after it starts (" + s.day() + " " + s.startTime() + ")");
                }
                TimetableSlot slot = new TimetableSlot();
                slot.setOffering(o);
                slot.setDay(s.day());
                slot.setStartTime(s.startTime());
                slot.setEndTime(s.endTime());
                slot.setRoom(s.room().trim());
                slot.setType(s.type() == null ? TeachingRole.THEORY : s.type());
                o.getSlots().add(slot);
            }
        }
    }

    /**
     * Warnings for a teacher or room booked twice at the same time, or a section
     * with two classes at once. Only offerings in the same academic year and the
     * same half (odd or even semesters) run at the same time.
     */
    @Transactional(readOnly = true)
    public List<String> clashes(CourseOffering o) {
        Set<String> warnings = new LinkedHashSet<>();
        List<TimetableSlot> mine = o.getSlots();
        for (int i = 0; i < mine.size(); i++) {
            for (int j = i + 1; j < mine.size(); j++) {
                if (mine.get(i).overlaps(mine.get(j))) {
                    warnings.add("Two of this offering's own classes overlap on " + mine.get(i).when());
                }
            }
        }
        List<CourseOffering> others = offerings.findRunningTogether(o.getAcademicYear().getId(), o.getSemester() % 2);
        for (CourseOffering other : others) {
            if (other.getId().equals(o.getId())) {
                continue;
            }
            for (TimetableSlot a : mine) {
                for (TimetableSlot b : other.getSlots()) {
                    if (!a.overlaps(b)) {
                        continue;
                    }
                    String at = " on " + b.when() + " (" + other.label() + ")";
                    for (OfferingTeacher ta : o.teachersFor(a.getType())) {
                        for (OfferingTeacher tb : other.teachersFor(b.getType())) {
                            if (ta.getFaculty().getId().equals(tb.getFaculty().getId())) {
                                warnings.add("Teacher " + ta.getFaculty().getName() + " is already teaching" + at);
                            }
                        }
                    }
                    if (a.getRoom().equalsIgnoreCase(b.getRoom())) {
                        warnings.add(a.getRoom() + " is already booked" + at);
                    }
                    if (other.getSection().getId().equals(o.getSection().getId())
                            && other.getSemester() == o.getSemester()) {
                        warnings.add("Sem " + o.getSemester() + " " + o.getSection().getCode()
                                + " already has a class" + at);
                    }
                }
            }
        }
        return new ArrayList<>(warnings);
    }

    // ---------- cancelled and extra classes

    @Transactional(readOnly = true)
    public List<OfferingDtos.ExceptionView> exceptions(Long offeringId) {
        Finder.find(offerings, offeringId, "Course offering");
        return exceptions.findByOfferingIdOrderByDateAsc(offeringId).stream()
                .map(OfferingDtos.ExceptionView::from).toList();
    }

    public OfferingDtos.ExceptionView addException(Long offeringId, ExceptionRequest req) {
        CourseOffering o = Finder.find(offerings, offeringId, "Course offering");
        AcademicYear y = o.getAcademicYear();
        if (req.date().isBefore(y.semesterStart(o.getSemester())) || req.date().isAfter(y.semesterEnd(o.getSemester()))) {
            throw new BusinessRuleException("That date is outside the semester ("
                    + y.semesterStart(o.getSemester()) + " to " + y.semesterEnd(o.getSemester()) + ")");
        }
        ClassException e = new ClassException();
        e.setOffering(o);
        e.setDate(req.date());
        e.setType(req.type());
        e.setReason(req.reason() == null ? null : req.reason().trim());
        if (req.type() == ExceptionType.CANCELLED) {
            if (holidays.existsByDate(req.date())) {
                throw new BusinessRuleException(req.date() + " is a holiday, there are no classes to cancel");
            }
            TimetableSlot slot = o.getSlots().stream()
                    .filter(s -> s.getDay() == req.date().getDayOfWeek() && s.getStartTime().equals(req.startTime()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessRuleException("There is no " + o.getCourse().getCode()
                            + " class on " + req.date().getDayOfWeek() + " at " + req.startTime()));
            e.setStartTime(slot.getStartTime());
            e.setEndTime(slot.getEndTime());
            e.setRoom(slot.getRoom());
        } else {
            if (req.endTime() == null || !req.startTime().isBefore(req.endTime())) {
                throw new BusinessRuleException("An extra class needs an end time after its start time");
            }
            if (req.room() == null || req.room().isBlank()) {
                throw new BusinessRuleException("An extra class needs a room");
            }
            e.setStartTime(req.startTime());
            e.setEndTime(req.endTime());
            e.setRoom(req.room().trim());
        }
        return OfferingDtos.ExceptionView.from(exceptions.save(e));
    }

    public void deleteException(Long id) {
        Finder.delete(exceptions, id, "Class change");
    }

    // ---------- mapping

    public OfferingView view(CourseOffering o) {
        List<TeacherView> teachers = o.getTeachers().stream()
                .map(t -> new TeacherView(t.getFaculty().getId(), t.getFaculty().getEmployeeId(),
                        t.getFaculty().getName(), t.getRole()))
                .toList();
        List<SlotView> slots = o.getSlots().stream()
                .sorted(java.util.Comparator.comparing(TimetableSlot::getDay).thenComparing(TimetableSlot::getStartTime))
                .map(s -> new SlotView(s.getId(), s.getDay(), s.getStartTime(), s.getEndTime(), s.getRoom(), s.getType()))
                .toList();
        return new OfferingView(o.getId(), o.getCourse().ref(), o.getCourse().getCredits(), o.getAcademicYear().ref(),
                o.getSemester(), o.getSection().ref(), o.getSection().getProgramme().ref(), teachers, slots,
                o.getId() == null ? 0 : enrolments.countByOfferingId(o.getId()));
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
