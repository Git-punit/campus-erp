package com.campuserp.event;

import com.campuserp.budget.BudgetDtos.BudgetSummary;
import com.campuserp.budget.BudgetRepository;
import com.campuserp.budget.BudgetService;
import com.campuserp.common.BusinessRuleException;
import com.campuserp.common.Finder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@Transactional
public class EventController {

    /** budgetAmount: leave empty or 0 for no budget. */
    public record EventRequest(@NotBlank @Size(max = 150) String name, @NotBlank @Size(max = 40) String type,
                               @NotNull LocalDate startDate, @NotNull LocalDate endDate,
                               @Size(max = 120) String venue, @NotNull EventStatus status,
                               @DecimalMin("0") BigDecimal budgetAmount) {}

    public record EventResponse(Long id, String name, String type, LocalDate startDate, LocalDate endDate,
                                String venue, EventStatus status, BudgetSummary budget) {}

    private final EventRepository events;
    private final BudgetRepository budgetRepo;
    private final BudgetService budgets;

    public EventController(EventRepository events, BudgetRepository budgetRepo, BudgetService budgets) {
        this.events = events;
        this.budgetRepo = budgetRepo;
        this.budgets = budgets;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<EventResponse> list() {
        return events.findAllByOrderByStartDateDesc().stream().map(this::view).toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public EventResponse get(@PathVariable Long id) {
        return view(Finder.find(events, id, "Event"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse create(@Valid @RequestBody EventRequest req) {
        Event e = events.save(apply(new Event(), req));
        budgets.syncEventBudget(e, req.budgetAmount());
        return view(e);
    }

    @PutMapping("/{id}")
    public EventResponse update(@PathVariable Long id, @Valid @RequestBody EventRequest req) {
        Event e = apply(Finder.find(events, id, "Event"), req);
        budgets.syncEventBudget(e, req.budgetAmount());
        return view(e);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        Event e = Finder.find(events, id, "Event");
        budgetRepo.findByEventId(id).ifPresent(budgets::removeIfUnused);
        events.delete(e);
    }

    private Event apply(Event e, EventRequest req) {
        if (req.endDate().isBefore(req.startDate())) {
            throw new BusinessRuleException("The end date can't be before the start date");
        }
        e.setName(req.name().trim());
        e.setType(req.type().trim().toUpperCase());
        e.setStartDate(req.startDate());
        e.setEndDate(req.endDate());
        e.setVenue(req.venue() == null || req.venue().isBlank() ? null : req.venue().trim());
        e.setStatus(req.status());
        return e;
    }

    private EventResponse view(Event e) {
        BudgetSummary b = e.getId() == null ? null : budgets.summaryOf(budgetRepo.findByEventId(e.getId())).orElse(null);
        return new EventResponse(e.getId(), e.getName(), e.getType(), e.getStartDate(), e.getEndDate(), e.getVenue(),
                e.getStatus(), b);
    }
}
