package com.campuserp.user;

import com.campuserp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One sign-in attempt, good or bad, for the admin's sign-in history. The time is createdAt.
 * Keeps the name as text (no link to the user) so deleting a user keeps their history.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "login_event")
public class LoginEvent extends BaseEntity {

    public enum Outcome { SUCCESS, WRONG_PASSWORD, LOCKED, NOT_ACTIVE }

    /** The account's username, or what was typed when no account matched. */
    @Column(nullable = false, length = 100)
    private String username;

    /** Empty when no account matched. */
    @Column(length = 100)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Outcome outcome;

    @Column(length = 64)
    private String ip;

    @Column(length = 255)
    private String userAgent;

    public LoginEvent(String username, String fullName, Outcome outcome, String ip, String userAgent) {
        this.username = cut(username, 100);
        this.fullName = fullName;
        this.outcome = outcome;
        this.ip = cut(ip, 64);
        this.userAgent = cut(userAgent, 255);
    }

    private static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
