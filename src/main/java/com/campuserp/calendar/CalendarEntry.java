package com.campuserp.calendar;

import com.campuserp.common.Ref;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * One class on one date. status is CLASS (normal weekly class), CANCELLED
 * (weekly class called off that day) or EXTRA (one-off class).
 */
public record CalendarEntry(LocalDate date, LocalTime startTime, LocalTime endTime, Long offeringId,
                            Ref course, Ref section, int semester, String room, List<String> teachers,
                            String status, String note) {
}
