package com.campuserp.budget;

import com.campuserp.common.BaseEntity;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The bytes of an expense's receipt, kept apart from Expense so lists of
 * expenses never load files. The name and type live on the Expense.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "expense_receipt")
public class ReceiptFile extends BaseEntity {

    @OneToOne(optional = false)
    @JoinColumn(name = "expense_id", nullable = false, unique = true)
    private Expense expense;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    // MySQL needs the length to pick a column big enough for 5 MB
    @Column(nullable = false, length = 6_000_000)
    private byte[] data;
}
