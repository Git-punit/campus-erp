package com.campuserp.budget;

import com.campuserp.budget.BudgetDtos.DecisionRequest;
import com.campuserp.budget.BudgetDtos.ExpenseRequest;
import com.campuserp.budget.BudgetDtos.ExpenseView;
import com.campuserp.common.BusinessRuleException;
import com.campuserp.common.Finder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class ExpenseService {

    private final ExpenseRepository expenses;
    private final BudgetRepository budgets;
    private final Clock clock;

    public ExpenseService(ExpenseRepository expenses, BudgetRepository budgets, Clock clock) {
        this.expenses = expenses;
        this.budgets = budgets;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ExpenseView> search(Long budgetId, ExpenseStatus status) {
        return expenses.search(budgetId, status).stream().map(ExpenseView::from).toList();
    }

    /** Staff or admin. Always starts as PENDING. */
    public ExpenseView raise(ExpenseRequest req, String username) {
        Expense e = new Expense();
        e.setRaisedBy(username);
        apply(e, req);
        return ExpenseView.from(expenses.save(e));
    }

    public ExpenseView update(Long id, ExpenseRequest req) {
        Expense e = pending(id, "edited");
        apply(e, req);
        return ExpenseView.from(e);
    }

    public void delete(Long id) {
        expenses.delete(pending(id, "deleted"));
    }

    /**
     * Admin only. If approving takes the budget over its allocation the admin
     * has to say so (allowOverspend), otherwise it's refused with a warning.
     */
    public ExpenseView approve(Long id, DecisionRequest req, String admin) {
        Expense e = pending(id, "approved");
        Budget b = e.getBudget();
        BigDecimal usedAfter = expenses.total(b.getId(), ExpenseStatus.APPROVED).add(e.getAmount());
        if (usedAfter.compareTo(b.getAllocated()) > 0 && !req.allowOverspend()) {
            throw new BusinessRuleException("Approving this takes '" + b.getName() + "' over its allocation by "
                    + rupees(usedAfter.subtract(b.getAllocated())) + ". Approve again with allowOverspend to go ahead.");
        }
        decide(e, ExpenseStatus.APPROVED, req.remark(), admin);
        return ExpenseView.from(e);
    }

    public ExpenseView reject(Long id, DecisionRequest req, String admin) {
        Expense e = pending(id, "rejected");
        if (req.remark() == null || req.remark().isBlank()) {
            throw new BusinessRuleException("Give a reason when rejecting");
        }
        decide(e, ExpenseStatus.REJECTED, req.remark(), admin);
        return ExpenseView.from(e);
    }

    private void decide(Expense e, ExpenseStatus status, String remark, String admin) {
        e.setStatus(status);
        e.setDecisionRemark(remark == null || remark.isBlank() ? null : remark.trim());
        e.setDecidedBy(admin);
        e.setDecidedAt(Instant.now(clock));
    }

    private Expense pending(Long id, String action) {
        Expense e = Finder.find(expenses, id, "Expense");
        if (e.getStatus() != ExpenseStatus.PENDING) {
            throw new BusinessRuleException("This expense is already " + e.getStatus().name().toLowerCase()
                    + " and can't be " + action);
        }
        return e;
    }

    private void apply(Expense e, ExpenseRequest req) {
        e.setBudget(Finder.find(budgets, req.budgetId(), "Budget"));
        e.setDate(req.date());
        e.setAmount(req.amount());
        e.setCategory(req.category());
        e.setDescription(req.description().trim());
    }

    static String rupees(BigDecimal amount) {
        NumberFormat f = NumberFormat.getCurrencyInstance(Locale.of("en", "IN"));
        f.setMaximumFractionDigits(0);
        return f.format(amount);
    }
}
