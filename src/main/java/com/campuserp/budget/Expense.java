package com.campuserp.budget;

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
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Expense extends BaseEntity {

    @ManyToOne(optional = false)
    private Budget budget;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExpenseCategory category;

    @Column(nullable = false, length = 300)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExpenseStatus status = ExpenseStatus.PENDING;

    @Column(nullable = false, length = 50)
    private String raisedBy;

    @Column(length = 50)
    private String decidedBy;

    private Instant decidedAt;

    @Column(length = 300)
    private String decisionRemark;

    /** Set when a receipt is attached; the file itself is in ReceiptFile. */
    @Column(length = 200)
    private String receiptName;

    @Column(length = 50)
    private String receiptType;

    private Long receiptSize;
}
