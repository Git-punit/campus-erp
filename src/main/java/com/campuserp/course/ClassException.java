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

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class ClassException extends BaseEntity {

    @ManyToOne(optional = false)
    private CourseOffering offering;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExceptionType type;

    /** For CANCELLED: start time of the weekly slot being cancelled. For EXTRA: the extra class times. */
    @Column(nullable = false)
    private LocalTime startTime;

    private LocalTime endTime;

    @Column(length = 60)
    private String room;

    @Column(length = 200)
    private String reason;
}
