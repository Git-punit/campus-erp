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

    /** A receipt as stored: shown back to the browser with its own name and type. */
    public record Receipt(String name, String type, byte[] data) {}

    static final long MAX_RECEIPT_BYTES = 5L * 1024 * 1024;

    private final ExpenseRepository expenses;
    private final BudgetRepository budgets;
    private final ReceiptFileRepository receipts;
    private final Clock clock;

    public ExpenseService(ExpenseRepository expenses, BudgetRepository budgets, ReceiptFileRepository receipts,
                          Clock clock) {
        this.expenses = expenses;
        this.budgets = budgets;
        this.receipts = receipts;
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
        Expense e = pending(id, "deleted");
        receipts.findByExpenseId(id).ifPresent(receipts::delete);
        expenses.delete(e);
    }

    // ---------- receipts

    /**
     * Attach (or replace) the bill for a pending expense. Only PDF, JPEG and PNG are
     * accepted, checked from the file's first bytes rather than what the browser claims.
     */
    public ExpenseView attachReceipt(Long id, String fileName, byte[] data) {
        Expense e = pending(id, "changed");
        if (data == null || data.length == 0) {
            throw new BusinessRuleException("The file is empty");
        }
        if (data.length > MAX_RECEIPT_BYTES) {
            throw new BusinessRuleException("The file is too big (at most 5 MB)");
        }
        String type = fileType(data);
        if (type == null) {
            throw new BusinessRuleException("Only PDF, JPG and PNG files can be attached");
        }
        ReceiptFile file = receipts.findByExpenseId(id).orElseGet(() -> {
            ReceiptFile f = new ReceiptFile();
            f.setExpense(e);
            return f;
        });
        file.setData(data);
        receipts.save(file);
        String name = fileName == null || fileName.isBlank() ? "receipt" : fileName.strip();
        e.setReceiptName(name.length() > 200 ? name.substring(name.length() - 200) : name);
        e.setReceiptType(type);
        e.setReceiptSize((long) data.length);
        return ExpenseView.from(e);
    }

    @Transactional(readOnly = true)
    public Receipt receipt(Long id) {
        Expense e = Finder.find(expenses, id, "Expense");
        ReceiptFile file = receipts.findByExpenseId(id)
                .orElseThrow(() -> com.campuserp.common.NotFoundException.of("Receipt for expense", id));
        return new Receipt(e.getReceiptName(), e.getReceiptType(), file.getData());
    }

    public ExpenseView removeReceipt(Long id) {
        Expense e = pending(id, "changed");
        receipts.findByExpenseId(id).ifPresent(receipts::delete);
        e.setReceiptName(null);
        e.setReceiptType(null);
        e.setReceiptSize(null);
        return ExpenseView.from(e);
    }

    /** The file type from its first bytes, or null if it isn't a PDF, JPEG or PNG. */
    static String fileType(byte[] d) {
        if (d.length >= 4 && d[0] == '%' && d[1] == 'P' && d[2] == 'D' && d[3] == 'F') {
            return "application/pdf";
        }
        if (d.length >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (d.length >= 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G') {
            return "image/png";
        }
        return null;
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
