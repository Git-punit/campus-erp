package com.campuserp.budget;

import com.campuserp.budget.BudgetDtos.BudgetSummary;
import com.campuserp.budget.BudgetDtos.Source;
import com.campuserp.common.BusinessRuleException;
import com.campuserp.common.Finder;
import com.campuserp.common.Money;
import com.campuserp.event.Event;
import com.campuserp.project.ResearchProject;
import com.campuserp.setup.AcademicCalendar;
import com.campuserp.setup.Department;
import com.campuserp.setup.DepartmentRepository;
import com.campuserp.training.TrainingClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class BudgetService {

    private final BudgetRepository budgets;
    private final ExpenseRepository expenses;
    private final DepartmentRepository departments;
    private final AcademicCalendar calendar;

    public BudgetService(BudgetRepository budgets, ExpenseRepository expenses, DepartmentRepository departments,
                         AcademicCalendar calendar) {
        this.budgets = budgets;
        this.expenses = expenses;
        this.departments = departments;
        this.calendar = calendar;
    }

    // ---------- summaries

    @Transactional(readOnly = true)
    public List<BudgetSummary> search(BudgetType type, Long departmentId, String fy) {
        return summarise(budgets.search(type, departmentId, fy == null || fy.isBlank() ? null : fy));
    }

    @Transactional(readOnly = true)
    public BudgetSummary summary(Long id) {
        return summarise(List.of(Finder.find(budgets, id, "Budget"))).getFirst();
    }

    @Transactional(readOnly = true)
    public Optional<BudgetSummary> summaryOf(Optional<Budget> budget) {
        return budget.map(b -> summarise(List.of(b)).getFirst());
    }

    @Transactional(readOnly = true)
    public List<BudgetSummary> summarise(List<Budget> list) {
        Map<Long, BigDecimal> used = new HashMap<>();
        Map<Long, BigDecimal> pending = new HashMap<>();
        if (!list.isEmpty()) {
            for (Object[] row : expenses.totalsFor(list.stream().map(Budget::getId).toList())) {
                Long id = (Long) row[0];
                BigDecimal sum = (BigDecimal) row[2];
                if (row[1] == ExpenseStatus.APPROVED) {
                    used.put(id, sum);
                } else if (row[1] == ExpenseStatus.PENDING) {
                    pending.put(id, sum);
                }
            }
        }
        return list.stream().map(b -> {
            BigDecimal u = used.getOrDefault(b.getId(), BigDecimal.ZERO);
            BigDecimal p = pending.getOrDefault(b.getId(), BigDecimal.ZERO);
            BigDecimal balance = b.getAllocated().subtract(u);
            return new BudgetSummary(b.getId(), b.getType(), b.getName(), b.getFinancialYear(),
                    b.getDepartment() == null ? null : b.getDepartment().ref(), source(b), b.getAllocated(), u, p,
                    balance, Money.percent(u, b.getAllocated()), u.compareTo(b.getAllocated()) > 0,
                    u.add(p).compareTo(b.getAllocated()) > 0);
        }).toList();
    }

    private static Source source(Budget b) {
        if (b.getEvent() != null) {
            return new Source("EVENT", b.getEvent().getId(), b.getEvent().getName());
        }
        if (b.getProject() != null) {
            return new Source("PROJECT", b.getProject().getId(), b.getProject().getTitle());
        }
        if (b.getTrainingClient() != null) {
            return new Source("TRAINING", b.getTrainingClient().getProgram().getId(),
                    b.getTrainingClient().getProgram().getName() + " / " + b.getTrainingClient().getClientName());
        }
        return null;
    }

    // ---------- department budgets (entered by hand)

    public BudgetSummary createDepartmentBudget(BudgetDtos.DepartmentBudgetRequest req) {
        Department d = Finder.find(departments, req.departmentId(), "Department");
        budgets.findByTypeAndDepartmentIdAndFinancialYear(BudgetType.DEPARTMENT, d.getId(), req.financialYear())
                .ifPresent(b -> {
                    throw new BusinessRuleException(d.getCode() + " already has a budget for " + req.financialYear());
                });
        Budget b = new Budget();
        b.setType(BudgetType.DEPARTMENT);
        b.setDepartment(d);
        b.setFinancialYear(req.financialYear());
        b.setName(d.getName() + " " + req.financialYear());
        b.setAllocated(req.allocated());
        return summary(budgets.save(b).getId());
    }

    /** Only department budgets can be changed here; the others follow their event, project or client. */
    public BudgetSummary setAllocation(Long id, BigDecimal allocated) {
        Budget b = Finder.find(budgets, id, "Budget");
        if (b.getType() != BudgetType.DEPARTMENT) {
            throw new BusinessRuleException("Change this amount on the " + b.getType().name().toLowerCase()
                    + " itself, the budget follows it");
        }
        b.setAllocated(allocated);
        return summary(id);
    }

    public void deleteDepartmentBudget(Long id) {
        Budget b = Finder.find(budgets, id, "Budget");
        if (b.getType() != BudgetType.DEPARTMENT) {
            throw new BusinessRuleException("This budget belongs to a " + b.getType().name().toLowerCase()
                    + " and is removed with it");
        }
        removeIfUnused(b);
    }

    // ---------- budgets that follow another record

    public Budget syncProjectBudget(ResearchProject p) {
        Budget b = budgets.findByProjectId(p.getId()).orElseGet(() -> {
            Budget n = new Budget();
            n.setType(BudgetType.PROJECT);
            n.setProject(p);
            n.setFinancialYear(calendar.financialYear(p.getSanctionDate()));
            return n;
        });
        b.setName("Project: " + p.getTitle());
        b.setAllocated(p.getSanctionedAmount());
        return budgets.save(b);
    }

    /** Events only get a budget when an amount is entered. Clearing it removes the budget if nothing was spent. */
    public void syncEventBudget(Event e, BigDecimal amount) {
        Optional<Budget> existing = budgets.findByEventId(e.getId());
        if (amount == null || amount.signum() == 0) {
            existing.ifPresent(this::removeIfUnused);
            return;
        }
        Budget b = existing.orElseGet(() -> {
            Budget n = new Budget();
            n.setType(BudgetType.EVENT);
            n.setEvent(e);
            return n;
        });
        b.setName("Event: " + e.getName());
        b.setFinancialYear(calendar.financialYear(e.getStartDate()));
        b.setAllocated(amount);
        budgets.save(b);
    }

    /** Called when a client accepts a proposal, and again if budget required changes later. */
    public Budget syncTrainingBudget(TrainingClient c) {
        Budget b = budgets.findByTrainingClientId(c.getId()).orElseGet(() -> {
            Budget n = new Budget();
            n.setType(BudgetType.TRAINING);
            n.setTrainingClient(c);
            n.setFinancialYear(calendar.financialYear(c.getAcceptedDate() != null ? c.getAcceptedDate() : calendar.today()));
            return n;
        });
        b.setName("Training: " + c.getProgram().getName() + " / " + c.getClientName());
        b.setAllocated(Money.nz(c.getBudgetRequired()));
        if (c.getProgram().getCoordinator() != null) {
            b.setDepartment(c.getProgram().getCoordinator().getDepartment());
        }
        return budgets.save(b);
    }

    public void removeIfUnused(Budget b) {
        if (expenses.countByBudgetId(b.getId()) > 0) {
            throw new BusinessRuleException("'" + b.getName() + "' already has expenses against it, so it can't be removed");
        }
        budgets.delete(b);
        budgets.flush();
    }

    @Transactional(readOnly = true)
    public BigDecimal approvedTotal(Budget b) {
        return expenses.total(b.getId(), ExpenseStatus.APPROVED);
    }
}
