package com.campuserp.project;

import com.campuserp.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class ResearchProject extends BaseEntity {

    @Column(nullable = false, length = 250)
    private String title;

    @Column(nullable = false, length = 120)
    private String fundingAgency;

    /** The whole amount is received at once. */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal sanctionedAmount;

    @Column(nullable = false)
    private LocalDate sanctionDate;

    private LocalDate startDate;

    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectStatus status = ProjectStatus.ONGOING;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectMember> team = new ArrayList<>();
}
