package com.campuserp.project;

import com.campuserp.common.BaseEntity;
import com.campuserp.faculty.Faculty;
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
public class ProjectMember extends BaseEntity {

    @ManyToOne(optional = false)
    private ResearchProject project;

    @ManyToOne(optional = false)
    private Faculty faculty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    public ProjectMember(ResearchProject project, Faculty faculty, MemberRole role) {
        this.project = project;
        this.faculty = faculty;
        this.role = role;
    }
}
