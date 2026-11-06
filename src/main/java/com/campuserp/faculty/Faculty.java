package com.campuserp.faculty;

import com.campuserp.common.BaseEntity;
import com.campuserp.common.Ref;
import com.campuserp.setup.Department;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Faculty extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String employeeId;

    @Column(nullable = false, length = 120)
    private String name;

    @ManyToOne(optional = false)
    private Department department;

    @Column(length = 60)
    private String designation;

    @Column(length = 120)
    private String email;

    @Column(length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FacultyStatus status = FacultyStatus.ACTIVE;

    public Ref ref() {
        return new Ref(getId(), employeeId, name);
    }
}
