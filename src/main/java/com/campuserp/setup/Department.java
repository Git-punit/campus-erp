package com.campuserp.setup;

import com.campuserp.common.BaseEntity;
import com.campuserp.common.Ref;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Department extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    public Department(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public Ref ref() {
        return new Ref(getId(), code, name);
    }
}
