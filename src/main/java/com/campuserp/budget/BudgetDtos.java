package com.campuserp.budget;

import com.campuserp.common.Ref;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class BudgetDtos {

    private BudgetDtos() {}

    /** Only department budgets are created by hand; the other types come from their own records. */
    public record DepartmentBudgetRequest(@NotNull Long departmentId,
                                          @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}", message = "use the form 2026-27") String financialYear,
                                          @NotNull @DecimalMin("0") BigDecimal allocated) {}

    public record AllocationRequest(@NotNull @DecimalMin("0") BigDecimal allocated) {}

    /** What it is for: the event, project or training client behind the budget. */
    public record Source(String kind, Long id, String name) {}

    public record BudgetSummary(Long id, BudgetType type, String name, String financialYear, Ref department,
                                Source source, BigDecimal allocated, BigDecimal utilized, BigDecimal pending,
                                BigDecimal balance, double percentUsed, boolean overspent,
                                boolean pendingWouldOverspend) {}

    public record ExpenseRequest(@NotNull Long budgetId, @NotNull LocalDate date,
                                 @NotNull @DecimalMin(value = "0.01", message = "must be more than zero") BigDecimal amount,
                                 @NotNull ExpenseCategory category,
                                 @NotBlank @Size(max = 300) String description) {}

    public record DecisionRequest(@Size(max = 300) String remark, boolean allowOverspend) {}

    public record ExpenseView(Long id, Long budgetId, String budgetName, BudgetType budgetType, LocalDate date,
                              BigDecimal amount, ExpenseCategory category, String description, ExpenseStatus status,
                              String raisedBy, String decidedBy, Instant decidedAt, String decisionRemark) {
        public static ExpenseView from(Expense e) {
            return new ExpenseView(e.getId(), e.getBudget().getId(), e.getBudget().getName(), e.getBudget().getType(),
                    e.getDate(), e.getAmount(), e.getCategory(), e.getDescription(), e.getStatus(), e.getRaisedBy(),
                    e.getDecidedBy(), e.getDecidedAt(), e.getDecisionRemark());
        }
    }
}
