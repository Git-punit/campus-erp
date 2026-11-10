package com.campuserp.budget;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReceiptFileRepository extends JpaRepository<ReceiptFile, Long> {

    Optional<ReceiptFile> findByExpenseId(Long expenseId);
}
