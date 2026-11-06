package com.campuserp.budget;

import com.campuserp.budget.BudgetDtos.AllocationRequest;
import com.campuserp.budget.BudgetDtos.BudgetSummary;
import com.campuserp.budget.BudgetDtos.DecisionRequest;
import com.campuserp.budget.BudgetDtos.DepartmentBudgetRequest;
import com.campuserp.budget.BudgetDtos.ExpenseRequest;
import com.campuserp.budget.BudgetDtos.ExpenseView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class BudgetController {

    private final BudgetService budgets;
    private final ExpenseService expenses;

    public BudgetController(BudgetService budgets, ExpenseService expenses) {
        this.budgets = budgets;
        this.expenses = expenses;
    }

    // ---------- budgets

    @GetMapping("/budgets")
    public List<BudgetSummary> list(@RequestParam(required = false) BudgetType type,
                                    @RequestParam(required = false) Long departmentId,
                                    @RequestParam(required = false) String financialYear) {
        return budgets.search(type, departmentId, financialYear);
    }

    @GetMapping("/budgets/{id}")
    public BudgetSummary get(@PathVariable Long id) {
        return budgets.summary(id);
    }

    @PostMapping("/budgets")
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetSummary createDepartmentBudget(@Valid @RequestBody DepartmentBudgetRequest req) {
        return budgets.createDepartmentBudget(req);
    }

    @PutMapping("/budgets/{id}")
    public BudgetSummary setAllocation(@PathVariable Long id, @Valid @RequestBody AllocationRequest req) {
        return budgets.setAllocation(id, req.allocated());
    }

    @DeleteMapping("/budgets/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        budgets.deleteDepartmentBudget(id);
    }

    // ---------- expenses

    @GetMapping("/expenses")
    public List<ExpenseView> expenses(@RequestParam(required = false) Long budgetId,
                                      @RequestParam(required = false) ExpenseStatus status) {
        return expenses.search(budgetId, status);
    }

    @PostMapping("/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseView raise(@Valid @RequestBody ExpenseRequest req, Authentication auth) {
        return expenses.raise(req, auth.getName());
    }

    @PutMapping("/expenses/{id}")
    public ExpenseView update(@PathVariable Long id, @Valid @RequestBody ExpenseRequest req) {
        return expenses.update(id, req);
    }

    @DeleteMapping("/expenses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExpense(@PathVariable Long id) {
        expenses.delete(id);
    }

    @PostMapping("/expenses/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ExpenseView approve(@PathVariable Long id, @Valid @RequestBody DecisionRequest req, Authentication auth) {
        return expenses.approve(id, req, auth.getName());
    }

    @PostMapping("/expenses/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ExpenseView reject(@PathVariable Long id, @Valid @RequestBody DecisionRequest req, Authentication auth) {
        return expenses.reject(id, req, auth.getName());
    }
}
