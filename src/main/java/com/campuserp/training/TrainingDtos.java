package com.campuserp.training;

import com.campuserp.common.Ref;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class TrainingDtos {

    private TrainingDtos() {}

    public record ProgramRequest(@NotBlank @Size(max = 150) String name, @Size(max = 1000) String description,
                                 @Min(1) Integer durationDays, Long coordinatorId) {}

    /** clientsReached / proposals / accepted / conducted are counts over the program's clients. */
    public record ProgramView(Long id, String name, String description, Integer durationDays, Ref coordinator,
                              long clientsReached, long proposals, long accepted, long conducted,
                              BigDecimal totalFee, BigDecimal totalCost) {}

    public record ClientRequest(@NotBlank @Size(max = 150) String clientName, @Size(max = 120) String contactPerson,
                                @Size(max = 30) String phone, @Email @Size(max = 120) String email,
                                LocalDate dateContacted,
                                boolean proposalSubmitted, LocalDate proposalDate,
                                Acceptance accepted, LocalDate acceptedDate, @Size(max = 300) String acceptedRemarks,
                                boolean conducted, LocalDate conductedFrom, LocalDate conductedTo,
                                @DecimalMin("0") BigDecimal budgetRequired, @DecimalMin("0") BigDecimal feeCharged) {}

    public record HistoryView(LocalDate date, Acceptance accepted, String remarks, String changedBy) {}

    /** actualCost = approved expenses on the client's training budget; profit = fee - actual cost. */
    public record ClientView(Long id, Long programId, String programName, String clientName, String contactPerson,
                             String phone, String email, LocalDate dateContacted,
                             boolean proposalSubmitted, LocalDate proposalDate,
                             Acceptance accepted, LocalDate acceptedDate, String acceptedRemarks,
                             boolean conducted, LocalDate conductedFrom, LocalDate conductedTo,
                             BigDecimal budgetRequired, BigDecimal feeCharged, Long budgetId,
                             BigDecimal actualCost, BigDecimal profit, List<HistoryView> history) {}
}
