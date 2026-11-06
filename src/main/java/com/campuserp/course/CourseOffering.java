package com.campuserp.course;

import com.campuserp.common.BaseEntity;
import com.campuserp.setup.AcademicYear;
import com.campuserp.setup.Section;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** One run of a course: in one academic year, for one semester of one section. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "academic_year_id", "semester", "section_id"}))
public class CourseOffering extends BaseEntity {

    @ManyToOne(optional = false)
    private Course course;

    @ManyToOne(optional = false)
    private AcademicYear academicYear;

    @Column(nullable = false)
    private int semester;

    @ManyToOne(optional = false)
    private Section section;

    @OneToMany(mappedBy = "offering", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OfferingTeacher> teachers = new ArrayList<>();

    @OneToMany(mappedBy = "offering", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("day, startTime")
    private List<TimetableSlot> slots = new ArrayList<>();

    /** e.g. "MA101 Sem 1 CSE-A 2026-27" */
    public String label() {
        return course.getCode() + " (Sem " + semester + " " + section.getCode() + ")";
    }

    /** Teachers who take this kind of class; everyone if nobody has that role. */
    public List<OfferingTeacher> teachersFor(TeachingRole type) {
        List<OfferingTeacher> match = teachers.stream().filter(t -> t.getRole() == type).toList();
        return match.isEmpty() ? teachers : match;
    }
}
