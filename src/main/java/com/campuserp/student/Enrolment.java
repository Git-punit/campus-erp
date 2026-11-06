package com.campuserp.student;

import com.campuserp.common.BaseEntity;
import com.campuserp.course.CourseOffering;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "offering_id"}))
public class Enrolment extends BaseEntity {

    @ManyToOne(optional = false)
    private Student student;

    @ManyToOne(optional = false)
    private CourseOffering offering;

    public Enrolment(Student student, CourseOffering offering) {
        this.student = student;
        this.offering = offering;
    }
}
