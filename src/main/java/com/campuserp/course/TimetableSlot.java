package com.campuserp.course;

import com.campuserp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** A weekly class: every <day> from <start> to <end> in <room>, for the offering's semester. */
@Getter
@Setter
@NoArgsConstructor
@Entity
public class TimetableSlot extends BaseEntity {

    @ManyToOne(optional = false)
    private CourseOffering offering;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek day;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false, length = 60)
    private String room;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TeachingRole type = TeachingRole.THEORY;

    public boolean overlaps(TimetableSlot o) {
        return day == o.day && startTime.isBefore(o.endTime) && o.startTime.isBefore(endTime);
    }

    public String when() {
        String d = day.name().charAt(0) + day.name().substring(1).toLowerCase();
        return d + " " + startTime + "-" + endTime;
    }
}
