package com.campuserp.setup;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Works out "today", the current academic year / semester half, and financial years. */
@Service
@Transactional(readOnly = true)
public class AcademicCalendar {

    /** The academic year and whether we're in its odd (1, 3, 5, 7) or even half. */
    public record Current(AcademicYear year, boolean odd) {
        public boolean matches(String yearName, int semester) {
            return year.getName().equals(yearName) && (semester % 2 == 1) == odd;
        }
    }

    private final AcademicYearRepository years;
    private final Clock clock;
    private final int fyStartMonth;

    public AcademicCalendar(AcademicYearRepository years, Clock clock,
                            @Value("${app.financial-year-start-month:4}") int fyStartMonth) {
        this.years = years;
        this.clock = clock;
        this.fyStartMonth = fyStartMonth;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /**
     * The year whose dates contain today. Between semesters, the half that
     * started most recently. Empty if no academic years are set up.
     */
    public Optional<Current> current() {
        LocalDate today = today();
        List<AcademicYear> all = years.findAllByOrderByOddSemStartDesc();
        for (AcademicYear y : all) {
            if (!today.isBefore(y.getOddSemStart()) && !today.isAfter(y.getEvenSemEnd())) {
                return Optional.of(new Current(y, today.isBefore(y.getEvenSemStart())));
            }
        }
        // not inside any year: the latest one that has started, else the earliest one
        return all.stream().filter(y -> !today.isBefore(y.getOddSemStart())).findFirst()
                .or(() -> all.isEmpty() ? Optional.empty() : Optional.of(all.getLast()))
                .map(y -> new Current(y, today.isBefore(y.getEvenSemStart())));
    }

    public Optional<String> currentYearName() {
        return current().map(c -> c.year().getName());
    }

    /** "2026-27" for any date from 1 April 2026 to 31 March 2027 (with the default April start). */
    public String financialYear(LocalDate date) {
        int start = date.getMonthValue() >= fyStartMonth ? date.getYear() : date.getYear() - 1;
        return "%d-%02d".formatted(start, (start + 1) % 100);
    }

    public String currentFinancialYear() {
        return financialYear(today());
    }
}
