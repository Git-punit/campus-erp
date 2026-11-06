package com.campuserp.course;

import com.campuserp.common.Ref;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class OfferingDtos {

    private OfferingDtos() {}

    public record TeacherInput(@NotNull Long facultyId, TeachingRole role) {}

    public record SlotInput(@NotNull DayOfWeek day, @NotNull LocalTime startTime, @NotNull LocalTime endTime,
                            @NotBlank @Size(max = 60) String room, TeachingRole type) {}

    public record OfferingRequest(@NotNull Long courseId, @NotNull Long academicYearId,
                                  @Min(1) @Max(12) int semester, @NotNull Long sectionId,
                                  @NotEmpty(message = "add at least one teacher") List<@Valid TeacherInput> teachers,
                                  List<@Valid SlotInput> slots) {}

    public record TeacherView(Long facultyId, String employeeId, String name, TeachingRole role) {}

    public record SlotView(Long id, DayOfWeek day, LocalTime startTime, LocalTime endTime, String room,
                           TeachingRole type) {}

    public record OfferingView(Long id, Ref course, int credits, Ref academicYear, int semester, Ref section,
                               Ref programme, List<TeacherView> teachers, List<SlotView> slots, long enrolled) {}

    /** Saving an offering returns it plus any clash warnings (it is saved either way). */
    public record SaveResult(OfferingView offering, List<String> warnings) {}

    public record ExceptionRequest(@NotNull LocalDate date, @NotNull ExceptionType type,
                                   @NotNull LocalTime startTime, LocalTime endTime,
                                   @Size(max = 60) String room, @Size(max = 200) String reason) {}

    public record ExceptionView(Long id, Long offeringId, LocalDate date, ExceptionType type, LocalTime startTime,
                                LocalTime endTime, String room, String reason) {
        public static ExceptionView from(ClassException e) {
            return new ExceptionView(e.getId(), e.getOffering().getId(), e.getDate(), e.getType(), e.getStartTime(),
                    e.getEndTime(), e.getRoom(), e.getReason());
        }
    }
}
