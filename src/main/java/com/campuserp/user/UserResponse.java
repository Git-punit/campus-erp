package com.campuserp.user;

import java.time.Instant;

/** What the API sends back for a user. Never includes the password hash. */
public record UserResponse(Long id, String username, String fullName, Role role,
                           boolean enabled, Instant createdAt) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getFullName(), u.getRole(),
                u.isEnabled(), u.getCreatedAt());
    }
}
