package com.campuserp.course;

import com.campuserp.common.BaseEntity;
import com.campuserp.common.Ref;
import com.campuserp.setup.Department;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Course extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private int credits;

    @ManyToOne(optional = false)
    private Department department;

    public Ref ref() {
        return new Ref(getId(), code, name);
    }
}
