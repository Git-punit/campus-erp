package com.campuserp.training;

import com.campuserp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One client a program was pitched to, and how far it got. */
@Getter
@Setter
@NoArgsConstructor
@Entity
public class TrainingClient extends BaseEntity {

    @ManyToOne(optional = false)
    private TrainingProgram program;

    @Column(nullable = false, length = 150)
    private String clientName;

    @Column(length = 120)
    private String contactPerson;

    @Column(length = 30)
    private String phone;

    @Column(length = 120)
    private String email;

    private LocalDate dateContacted;

    @Column(nullable = false)
    private boolean proposalSubmitted;

    private LocalDate proposalDate;

    /** null until the client answers */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Acceptance accepted;

    private LocalDate acceptedDate;

    @Column(length = 300)
    private String acceptedRemarks;

    @Column(nullable = false)
    private boolean conducted;

    private LocalDate conductedFrom;

    private LocalDate conductedTo;

    @Column(precision = 14, scale = 2)
    private BigDecimal budgetRequired;

    /** what the client pays the institute */
    @Column(precision = 14, scale = 2)
    private BigDecimal feeCharged;
}
