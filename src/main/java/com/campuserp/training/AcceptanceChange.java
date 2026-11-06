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

import java.time.LocalDate;

/** History of the "accepted" field: one row per change. */
@Getter
@Setter
@NoArgsConstructor
@Entity
public class AcceptanceChange extends BaseEntity {

    @ManyToOne(optional = false)
    private TrainingClient client;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Acceptance accepted;

    @Column(length = 300)
    private String remarks;

    @Column(length = 50)
    private String changedBy;

    public AcceptanceChange(TrainingClient client, LocalDate date, Acceptance accepted, String remarks, String changedBy) {
        this.client = client;
        this.date = date;
        this.accepted = accepted;
        this.remarks = remarks;
        this.changedBy = changedBy;
    }
}
