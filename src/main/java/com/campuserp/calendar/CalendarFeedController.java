package com.campuserp.calendar;

import com.campuserp.auth.JwtService;
import com.campuserp.calendar.CalendarService.CalendarView;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * A personal calendar link (.ics) for a student or teacher, so their classes
 * show up in Google Calendar, Outlook or a phone. Calendar apps can't log in,
 * so the link carries a key signed with the server secret instead.
 */
@RestController
@RequestMapping("/api/calendar")
public class CalendarFeedController {

    private static final DateTimeFormatter UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    private final CalendarService calendar;
    private final JwtService jwt;
    private final Clock clock;
    private final com.campuserp.student.StudentRepository students;
    private final com.campuserp.faculty.FacultyRepository faculty;

    public CalendarFeedController(CalendarService calendar, JwtService jwt, Clock clock,
                                  com.campuserp.student.StudentRepository students,
                                  com.campuserp.faculty.FacultyRepository faculty) {
        this.students = students;
        this.faculty = faculty;
        this.calendar = calendar;
        this.jwt = jwt;
        this.clock = clock;
    }

    /** For a logged-in user: the private feed address to paste into a calendar app. */
    @GetMapping("/feed-link/{kind}/{id}")
    public Map<String, String> link(@PathVariable String kind, @PathVariable Long id) {
        checkKind(kind);
        boolean exists = kind.equals("student") ? students.existsById(id) : faculty.existsById(id);
        if (!exists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such " + kind);
        }
        return Map.of("path", "/api/calendar/feed/" + kind + "/" + id + ".ics?key=" + jwt.sign(kind + ":" + id));
    }

    @GetMapping("/feed/{kind}/{id}.ics")
    public ResponseEntity<String> feed(@PathVariable String kind, @PathVariable Long id,
                                       @RequestParam(required = false) String key) {
        checkKind(kind);
        if (!jwt.checkSignature(kind + ":" + id, key)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This calendar link isn't valid");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar; charset=utf-8"))
                .body(ics(view(kind, id)));
    }

    // two weeks back and about three months ahead, within the calendar's 120-day limit
    private CalendarView view(String kind, Long id) {
        LocalDate today = LocalDate.now(clock);
        LocalDate from = today.minusDays(14);
        LocalDate to = today.plusDays(105);
        return kind.equals("student") ? calendar.forStudent(id, from, to) : calendar.forFaculty(id, from, to);
    }

    private String ics(CalendarView view) {
        StringBuilder out = new StringBuilder("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//campus-erp//calendar//EN\r\n"
                + "CALSCALE:GREGORIAN\r\nX-WR-CALNAME:Campus ERP classes\r\n");
        String now = utc(LocalDateTime.now(clock));
        for (CalendarEntry e : view.entries()) {
            out.append("BEGIN:VEVENT\r\n")
                    .append("UID:").append(e.offeringId()).append('-').append(e.date()).append('-')
                    .append(e.startTime().toString().replace(":", "")).append("@campus-erp\r\n")
                    .append("DTSTAMP:").append(now).append("\r\n")
                    .append("DTSTART:").append(utc(e.date().atTime(e.startTime()))).append("\r\n")
                    .append("DTEND:").append(utc(e.date().atTime(e.endTime()))).append("\r\n")
                    .append("SUMMARY:").append(text(e.course().code() + " " + e.course().name()
                            + (e.status().equals("CANCELLED") ? " (cancelled)" : "")
                            + (e.status().equals("EXTRA") ? " (extra class)" : ""))).append("\r\n")
                    .append("LOCATION:").append(text(e.room())).append("\r\n")
                    .append("DESCRIPTION:").append(text("Sem " + e.semester() + " " + e.section().code()
                            + ", " + String.join(", ", e.teachers())
                            + (e.note() == null ? "" : ". " + e.note()))).append("\r\n");
            if (e.status().equals("CANCELLED")) {
                out.append("STATUS:CANCELLED\r\n");
            }
            out.append("END:VEVENT\r\n");
        }
        return out.append("END:VCALENDAR\r\n").toString();
    }

    private String utc(LocalDateTime local) {
        return local.atZone(clock.getZone()).withZoneSameInstant(ZoneOffset.UTC).format(UTC);
    }

    /** iCalendar text values escape backslash, comma, semicolon and new lines */
    static String text(String s) {
        return s.replace("\\", "\\\\").replace(",", "\\,").replace(";", "\\;").replace("\n", "\\n");
    }

    private static void checkKind(String kind) {
        if (!kind.equals("student") && !kind.equals("faculty")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such calendar");
        }
    }
}
