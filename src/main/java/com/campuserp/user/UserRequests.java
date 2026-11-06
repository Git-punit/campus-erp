package com.campuserp.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class UserRequests {

    private UserRequests() {}

    public record Create(
            @NotBlank @Size(min = 3, max = 50)
            @Pattern(regexp = "[a-zA-Z0-9._-]+", message = "only letters, numbers, dot, dash and underscore")
            String username,
            @NotBlank @Size(min = 6, max = 100) String password,
            @NotBlank @Size(max = 100) String fullName,
            @NotNull Role role) {}

    /** password is optional here: leave it out (or blank) to keep the current one. */
    public record Update(
            @NotBlank @Size(max = 100) String fullName,
            @NotNull Role role,
            boolean enabled,
            @Size(max = 100) String password) {}
}
