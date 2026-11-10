package com.campuserp.auth;

import com.campuserp.user.User;
import com.campuserp.user.UserRepository;
import com.campuserp.user.UserResponse;
import jakarta.validation.Valid;
import com.campuserp.common.BusinessRuleException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record LoginResponse(String token, UserResponse user) {}

    public record UsernameChange(
            @NotBlank @Size(min = 3, max = 50)
            @Pattern(regexp = "[a-zA-Z0-9._-]+", message = "only letters, numbers, dot, dash and underscore")
            String newUsername,
            @NotBlank String currentPassword) {}

    public record PasswordChange(@NotBlank String currentPassword,
                                 @NotBlank @Size(min = 6, max = 100) String newPassword) {}

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final LoginAttempts attempts;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwtService,
                          LoginAttempts attempts) {
        this.attempts = attempts;
        this.users = users;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        String username = req.username().trim();
        long locked = attempts.minutesLocked(username);
        if (locked > 0) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many wrong passwords. Try again in " + locked + " minute" + (locked == 1 ? "" : "s") + ".");
        }
        User user = users.findByUsername(username)
                .filter(User::isEnabled)
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElse(null);
        if (user == null) {
            attempts.failed(username);
            // same message whether the username or the password was wrong
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong username or password");
        }
        attempts.succeeded(username);
        return new LoginResponse(jwtService.createToken(user), UserResponse.from(user));
    }

    @GetMapping("/me")
    public UserResponse me(Authentication auth) {
        return users.findByUsername(auth.getName())
                .map(UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in"));
    }

    /** Any logged-in user can change their own password. */
    @PostMapping("/password")
    @Transactional
    public LoginResponse changePassword(@Valid @RequestBody PasswordChange req, Authentication auth) {
        User user = users.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in"));
        if (!encoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("Your current password is wrong");
        }
        user.setPasswordHash(encoder.encode(req.newPassword()));
        // older tokens (other devices, a stolen one) stop working; this tab gets a fresh one
        return new LoginResponse(jwtService.createToken(user), UserResponse.from(user));
    }

    /**
     * Change your own login name. Needs your password, because the username is half of
     * what logs you in. Past records (who raised or approved an expense) keep the old name.
     */
    @PostMapping("/username")
    @Transactional
    public LoginResponse changeUsername(@Valid @RequestBody UsernameChange req, Authentication auth) {
        User user = current(auth);
        if (!encoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("Your current password is wrong");
        }
        String name = req.newUsername().trim();
        if (name.equalsIgnoreCase(user.getUsername())) {
            throw new BusinessRuleException("That is already your username");
        }
        if (users.existsByUsername(name)) {
            throw new BusinessRuleException("Username '" + name + "' is already taken");
        }
        user.setUsername(name);
        // tokens name the user, so the old ones stop working; this tab gets a fresh one
        return new LoginResponse(jwtService.createToken(user), UserResponse.from(user));
    }

    /** End every login of this account except the one making this request (it gets a fresh token). */
    @PostMapping("/logout-others")
    @Transactional
    public LoginResponse logOutOthers(Authentication auth) {
        User user = current(auth);
        user.logOutEverywhere();
        return new LoginResponse(jwtService.createToken(user), UserResponse.from(user));
    }

    private User current(Authentication auth) {
        return users.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in"));
    }
}
