package com.campuserp.training;

import com.campuserp.budget.Budget;
import com.campuserp.budget.BudgetRepository;
import com.campuserp.budget.BudgetService;
import com.campuserp.common.BusinessRuleException;
import com.campuserp.common.Finder;
import com.campuserp.common.Money;
import com.campuserp.faculty.FacultyRepository;
import com.campuserp.setup.AcademicCalendar;
import com.campuserp.training.TrainingDtos.ClientRequest;
import com.campuserp.training.TrainingDtos.ClientView;
import com.campuserp.training.TrainingDtos.HistoryView;
import com.campuserp.training.TrainingDtos.ProgramRequest;
import com.campuserp.training.TrainingDtos.ProgramView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class TrainingService {

    private final TrainingProgramRepository programs;
    private final TrainingClientRepository clients;
    private final AcceptanceChangeRepository changes;
    private final FacultyRepository faculty;
    private final BudgetRepository budgetRepo;
    private final BudgetService budgets;
    private final AcademicCalendar calendar;

    public TrainingService(TrainingProgramRepository programs, TrainingClientRepository clients,
                           AcceptanceChangeRepository changes, FacultyRepository faculty,
                           BudgetRepository budgetRepo, BudgetService budgets, AcademicCalendar calendar) {
        this.programs = programs;
        this.clients = clients;
        this.changes = changes;
        this.faculty = faculty;
        this.budgetRepo = budgetRepo;
        this.budgets = budgets;
        this.calendar = calendar;
    }

    // ---------- programs

    @Transactional(readOnly = true)
    public List<ProgramView> programs() {
        return programs.findAllByOrderByNameAsc().stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public ProgramView program(Long id) {
        return view(Finder.find(programs, id, "Training program"));
    }

    public ProgramView saveProgram(Long id, ProgramRequest req) {
        TrainingProgram p = id == null ? new TrainingProgram() : Finder.find(programs, id, "Training program");
        p.setName(req.name().trim());
        p.setDescription(req.description() == null || req.description().isBlank() ? null : req.description().trim());
        p.setDurationDays(req.durationDays());
        p.setCoordinator(req.coordinatorId() == null ? null : Finder.find(faculty, req.coordinatorId(), "Faculty"));
        programs.save(p);
        // budget names include the program name
        clients.findByProgramId(p.getId()).forEach(c -> budgetRepo.findByTrainingClientId(c.getId())
                .ifPresent(b -> budgets.syncTrainingBudget(c)));
        return view(p);
    }

    public void deleteProgram(Long id) {
        TrainingProgram p = Finder.find(programs, id, "Training program");
        if (!clients.findByProgramId(id).isEmpty()) {
            throw new BusinessRuleException("This program has clients. Remove them first.");
        }
        programs.delete(p);
    }

    // ---------- clients

    @Transactional(readOnly = true)
    public List<ClientView> clients(Long programId) {
        Finder.find(programs, programId, "Training program");
        return clients.findByProgramIdOrderByDateContactedDesc(programId).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public ClientView client(Long id) {
        return view(Finder.find(clients, id, "Client"));
    }

    public ClientView addClient(Long programId, ClientRequest req, String username) {
        TrainingClient c = new TrainingClient();
        c.setProgram(Finder.find(programs, programId, "Training program"));
        apply(c, req, username);
        return view(c);
    }

    public ClientView updateClient(Long id, ClientRequest req, String username) {
        TrainingClient c = Finder.find(clients, id, "Client");
        apply(c, req, username);
        return view(c);
    }

    public void deleteClient(Long id) {
        TrainingClient c = Finder.find(clients, id, "Client");
        budgetRepo.findByTrainingClientId(id).ifPresent(budgets::removeIfUnused);
        changes.deleteByClientId(id);
        clients.delete(c);
    }

    /**
     * The steps go in order: proposal submitted, then accepted, then conducted.
     * Every change to "accepted" is kept. Accepted = YES creates the training budget.
     */
    private void apply(TrainingClient c, ClientRequest req, String username) {
        if (!req.proposalSubmitted() && req.accepted() != null) {
            throw new BusinessRuleException("Mark the proposal as submitted before recording the client's answer");
        }
        if (req.conducted() && req.accepted() != Acceptance.YES) {
            throw new BusinessRuleException("A training can only be marked conducted after the client has accepted");
        }
        if (req.conducted() && req.conductedFrom() == null) {
            throw new BusinessRuleException("Enter the dates the training was conducted");
        }
        if (req.conductedFrom() != null && req.conductedTo() != null && req.conductedTo().isBefore(req.conductedFrom())) {
            throw new BusinessRuleException("The training can't end before it starts");
        }
        if (req.accepted() == Acceptance.YES && req.budgetRequired() == null) {
            throw new BusinessRuleException("Enter the budget required before marking it accepted");
        }

        Acceptance before = c.getAccepted();
        java.time.LocalDate beforeDate = c.getAcceptedDate();
        String beforeRemarks = c.getAcceptedRemarks();

        c.setClientName(req.clientName().trim());
        c.setContactPerson(blank(req.contactPerson()));
        c.setPhone(blank(req.phone()));
        c.setEmail(blank(req.email()));
        c.setDateContacted(req.dateContacted());
        c.setProposalSubmitted(req.proposalSubmitted());
        c.setProposalDate(req.proposalSubmitted()
                ? (req.proposalDate() != null ? req.proposalDate() : calendar.today()) : null);
        c.setAccepted(req.accepted());
        c.setAcceptedDate(req.accepted() == null ? null
                : (req.acceptedDate() != null ? req.acceptedDate() : calendar.today()));
        c.setAcceptedRemarks(req.accepted() == null ? null : blank(req.acceptedRemarks()));
        c.setConducted(req.conducted());
        c.setConductedFrom(req.conducted() ? req.conductedFrom() : null);
        c.setConductedTo(req.conducted() ? (req.conductedTo() != null ? req.conductedTo() : req.conductedFrom()) : null);
        c.setBudgetRequired(req.budgetRequired());
        c.setFeeCharged(req.feeCharged());
        if (c.getProposalDate() != null && c.getAcceptedDate() != null && c.getAcceptedDate().isBefore(c.getProposalDate())) {
            throw new BusinessRuleException("The answer can't come before the proposal was submitted");
        }
        clients.save(c);

        boolean changed = c.getAccepted() != before || !Objects.equals(c.getAcceptedDate(), beforeDate)
                || !Objects.equals(c.getAcceptedRemarks(), beforeRemarks);
        if (changed && c.getAccepted() != null) {
            changes.save(new AcceptanceChange(c, c.getAcceptedDate(), c.getAccepted(), c.getAcceptedRemarks(), username));
        }

        if (c.getAccepted() == Acceptance.YES) {
            budgets.syncTrainingBudget(c);
        } else {
            // no longer accepted: drop the budget if nothing was spent from it
            budgetRepo.findByTrainingClientId(c.getId())
                    .filter(b -> budgets.approvedTotal(b).signum() == 0)
                    .ifPresent(budgets::removeIfUnused);
        }
    }

    // ---------- mapping

    private ProgramView view(TrainingProgram p) {
        List<TrainingClient> cs = clients.findByProgramId(p.getId());
        BigDecimal fee = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;
        for (TrainingClient c : cs) {
            if (c.getAccepted() == Acceptance.YES) {
                fee = fee.add(Money.nz(c.getFeeCharged()));
                cost = cost.add(actualCost(c));
            }
        }
        return new ProgramView(p.getId(), p.getName(), p.getDescription(), p.getDurationDays(),
                p.getCoordinator() == null ? null : p.getCoordinator().ref(), cs.size(),
                cs.stream().filter(TrainingClient::isProposalSubmitted).count(),
                cs.stream().filter(c -> c.getAccepted() == Acceptance.YES).count(),
                cs.stream().filter(TrainingClient::isConducted).count(), fee, cost);
    }

    private BigDecimal actualCost(TrainingClient c) {
        return budgetRepo.findByTrainingClientId(c.getId()).map(budgets::approvedTotal).orElse(BigDecimal.ZERO);
    }

    private ClientView view(TrainingClient c) {
        Budget b = budgetRepo.findByTrainingClientId(c.getId()).orElse(null);
        BigDecimal cost = b == null ? BigDecimal.ZERO : budgets.approvedTotal(b);
        BigDecimal profit = c.getAccepted() == Acceptance.YES ? Money.nz(c.getFeeCharged()).subtract(cost) : null;
        List<HistoryView> history = changes.findByClientIdOrderByDateAscIdAsc(c.getId()).stream()
                .map(h -> new HistoryView(h.getDate(), h.getAccepted(), h.getRemarks(), h.getChangedBy())).toList();
        return new ClientView(c.getId(), c.getProgram().getId(), c.getProgram().getName(), c.getClientName(),
                c.getContactPerson(), c.getPhone(), c.getEmail(), c.getDateContacted(), c.isProposalSubmitted(),
                c.getProposalDate(), c.getAccepted(), c.getAcceptedDate(), c.getAcceptedRemarks(), c.isConducted(),
                c.getConductedFrom(), c.getConductedTo(), c.getBudgetRequired(), c.getFeeCharged(),
                b == null ? null : b.getId(), cost, profit, history);
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
