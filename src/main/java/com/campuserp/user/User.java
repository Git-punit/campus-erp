package com.campuserp.user;

import com.campuserp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "app_user")
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    /** Job title / department, e.g. "Accounts Officer, Finance" */
    @Column(length = 100)
    private String designation;

    /** image/jpeg or image/png when a profile photo is set; the bytes are in UserPhoto */
    @Column(length = 20)
    private String photoType;

    /** Raised by "log out of all other devices"; null on accounts made before it existed. */
    private Integer sessionVersion;

    public User(String username, String passwordHash, String fullName, Role role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
    }

    /** Changes whenever older login tokens must stop working: a new password, or "log out everywhere". */
    public String tokenKey() {
        return passwordHash + ":" + (sessionVersion == null ? 0 : sessionVersion);
    }

    public void logOutEverywhere() {
        sessionVersion = (sessionVersion == null ? 0 : sessionVersion) + 1;
    }
}
