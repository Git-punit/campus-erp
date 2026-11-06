package com.campuserp.setup;

import com.campuserp.common.BaseEntity;
import com.campuserp.common.Ref;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** e.g. 2026-27. Odd semesters (1, 3, 5, 7) run in the first half, even ones in the second. */
@Getter
@Setter
@NoArgsConstructor
@Entity
public class AcademicYear extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String name;

    @Column(nullable = false)
    private LocalDate oddSemStart;

    @Column(nullable = false)
    private LocalDate oddSemEnd;

    @Column(nullable = false)
    private LocalDate evenSemStart;

    @Column(nullable = false)
    private LocalDate evenSemEnd;

    public LocalDate semesterStart(int semester) {
        return semester % 2 == 1 ? oddSemStart : evenSemStart;
    }

    public LocalDate semesterEnd(int semester) {
        return semester % 2 == 1 ? oddSemEnd : evenSemEnd;
    }

    public Ref ref() {
        return new Ref(getId(), name, name);
    }
}
