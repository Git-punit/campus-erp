package com.campuserp.setup;

import com.campuserp.common.BaseEntity;
import com.campuserp.common.Ref;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** e.g. CSE-A. The same section exists in every semester; the semester lives on the student and the offering. */
@Getter
@Setter
@NoArgsConstructor
@Entity
public class Section extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @ManyToOne(optional = false)
    private Programme programme;

    public Ref ref() {
        return new Ref(getId(), code, code);
    }
}
