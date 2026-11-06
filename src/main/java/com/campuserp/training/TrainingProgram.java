package com.campuserp.training;

import com.campuserp.common.BaseEntity;
import com.campuserp.faculty.Faculty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class TrainingProgram extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    /** in days */
    private Integer durationDays;

    @ManyToOne
    private Faculty coordinator;
}
