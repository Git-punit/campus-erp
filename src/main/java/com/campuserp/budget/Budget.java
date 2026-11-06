package com.campuserp.budget;

import com.campuserp.common.BaseEntity;
import com.campuserp.event.Event;
import com.campuserp.project.ResearchProject;
import com.campuserp.setup.Department;
import com.campuserp.training.TrainingClient;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Money set aside for something. Exactly one of department / event / project /
 * trainingClient is filled in, matching the type.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
public class Budget extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BudgetType type;

    @Column(nullable = false, length = 200)
    private String name;

    /** e.g. 2026-27. For department budgets it's the year the money is for; otherwise the year it started. */
    @Column(nullable = false, length = 10)
    private String financialYear;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal allocated = BigDecimal.ZERO;

    /** Set for department budgets, and for others when known (the project PI's department, etc.). */
    @ManyToOne
    private Department department;

    @OneToOne
    private Event event;

    @OneToOne
    private ResearchProject project;

    @OneToOne
    private TrainingClient trainingClient;
}
