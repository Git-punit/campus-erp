package com.campuserp.search;

import com.campuserp.course.CourseRepository;
import com.campuserp.event.EventRepository;
import com.campuserp.faculty.FacultyRepository;
import com.campuserp.project.ResearchProjectRepository;
import com.campuserp.student.StudentRepository;
import com.campuserp.training.TrainingProgramRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/** The search box in the top bar: up to 5 matches per kind of record. Any logged-in user (it's a GET). */
@RestController
@Transactional(readOnly = true)
public class SearchController {

    /** link is the page in the React app to open. */
    public record Hit(String type, String title, String detail, String link) {}

    private final FacultyRepository faculty;
    private final StudentRepository students;
    private final CourseRepository courses;
    private final ResearchProjectRepository projects;
    private final EventRepository events;
    private final TrainingProgramRepository training;

    public SearchController(FacultyRepository faculty, StudentRepository students, CourseRepository courses,
                            ResearchProjectRepository projects, EventRepository events,
                            TrainingProgramRepository training) {
        this.faculty = faculty;
        this.students = students;
        this.courses = courses;
        this.projects = projects;
        this.events = events;
        this.training = training;
    }

    @GetMapping("/api/search")
    public List<Hit> search(@RequestParam String q) {
        String t = q.strip();
        if (t.length() < 2 || t.length() > 100) return List.of();
        List<Hit> hits = new ArrayList<>();
        faculty.findTop5ByNameContainingIgnoreCaseOrEmployeeIdContainingIgnoreCaseOrderByNameAsc(t, t).forEach(f ->
                hits.add(new Hit("Faculty", f.getName(), f.getEmployeeId(), "/faculty/" + f.getId())));
        students.findTop5ByNameContainingIgnoreCaseOrRollNoContainingIgnoreCaseOrderByNameAsc(t, t).forEach(s ->
                hits.add(new Hit("Student", s.getName(), s.getRollNo(), "/students/" + s.getId())));
        // courses, projects and events have no page of their own, so open their list
        courses.findTop5ByNameContainingIgnoreCaseOrCodeContainingIgnoreCaseOrderByCodeAsc(t, t).forEach(c ->
                hits.add(new Hit("Course", c.getName(), c.getCode(), "/courses")));
        projects.findTop5ByTitleContainingIgnoreCaseOrderByTitleAsc(t).forEach(p ->
                hits.add(new Hit("Research project", p.getTitle(), p.getFundingAgency(), "/projects")));
        events.findTop5ByNameContainingIgnoreCaseOrderByStartDateDesc(t).forEach(e ->
                hits.add(new Hit("Event", e.getName(), e.getVenue(), "/events")));
        training.findTop5ByNameContainingIgnoreCaseOrderByNameAsc(t).forEach(p ->
                hits.add(new Hit("Training program", p.getName(), null, "/training/" + p.getId())));
        return hits;
    }
}
