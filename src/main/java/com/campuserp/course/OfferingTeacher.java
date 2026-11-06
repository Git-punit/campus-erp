package com.campuserp.course;

import com.campuserp.common.BaseEntity;
import com.campuserp.faculty.Faculty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class OfferingTeacher extends BaseEntity {

    @ManyToOne(optional = false)
    private CourseOffering offering;

    @ManyToOne(optional = false)
    private Faculty faculty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TeachingRole role = TeachingRole.THEORY;

    public OfferingTeacher(CourseOffering offering, Faculty faculty, TeachingRole role) {
        this.offering = offering;
        this.faculty = faculty;
        this.role = role;
    }
}
