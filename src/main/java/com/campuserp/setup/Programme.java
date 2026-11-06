package com.campuserp.setup;

import com.campuserp.common.BaseEntity;
import com.campuserp.common.Ref;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** e.g. B.Tech CSE, 8 semesters. */
@Getter
@Setter
@NoArgsConstructor
@Entity
public class Programme extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @ManyToOne(optional = false)
    private Department department;

    @Column(nullable = false)
    private int semesters;

    public Ref ref() {
        return new Ref(getId(), code, name);
    }
}
