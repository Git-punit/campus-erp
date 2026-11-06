package com.campuserp.budget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    /** rows of [budgetId, status, total] */
    @Query("select e.budget.id, e.status, sum(e.amount) from Expense e "
            + "where e.budget.id in :budgetIds group by e.budget.id, e.status")
    List<Object[]> totalsFor(Collection<Long> budgetIds);

    @Query("select coalesce(sum(e.amount), 0) from Expense e where e.budget.id = :budgetId and e.status = :status")
    BigDecimal total(Long budgetId, ExpenseStatus status);

    @Query("select e from Expense e where (:budgetId is null or e.budget.id = :budgetId) "
            + "and (:status is null or e.status = :status) order by e.date desc, e.id desc")
    List<Expense> search(Long budgetId, ExpenseStatus status);

    long countByBudgetId(Long budgetId);

    long countByStatus(ExpenseStatus status);

    @Query("select coalesce(sum(e.amount), 0) from Expense e where e.status = :status")
    BigDecimal totalByStatus(ExpenseStatus status);
}
