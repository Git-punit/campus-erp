package com.campuserp.student;

import com.campuserp.common.BaseEntity;
import com.campuserp.common.Ref;
import com.campuserp.setup.Programme;
import com.campuserp.setup.Section;
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
public class Student extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String rollNo;

    @Column(nullable = false, length = 120)
    private String name;

    @ManyToOne(optional = false)
    private Programme programme;

    @Column(nullable = false)
    private int currentSemester;

    @ManyToOne(optional = false)
    private Section section;

    @Column(length = 120)
    private String email;

    @Column(length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StudentStatus status = StudentStatus.ACTIVE;

    public Ref ref() {
        return new Ref(getId(), rollNo, name);
    }
}
