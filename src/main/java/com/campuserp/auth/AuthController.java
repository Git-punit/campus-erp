package com.campuserp.auth;

import com.campuserp.user.LoginEvent;
import com.campuserp.user.LoginEventRepository;
import com.campuserp.user.Role;
import com.campuserp.user.UserController;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import com.campuserp.user.User;
import com.campuserp.user.UserRepository;
import com.campuserp.user.UserResponse;
import jakarta.validation.Valid;
import com.campuserp.common.BusinessRuleException;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
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

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record LoginResponse(String token, UserResponse user) {}

    /** Sign-up takes exactly these three things. Role and access are decided by the server. */
    public record SignupRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 100) String email,
            @NotBlank @Size(min = 6, max = 100) String password) {}

    static final Set<String> SIGNUP_FIELDS = Set.of("name", "email", "password");

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
    private final Validator validator;
    private final LoginEventRepository loginEvents;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwtService,
                          LoginAttempts attempts, Validator validator, LoginEventRepository loginEvents) {
        this.loginEvents = loginEvents;
        this.validator = validator;
        this.attempts = attempts;
        this.users = users;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        String username = req.username().trim();
        // behind Railway's proxy the visitor's address is the first one in X-Forwarded-For
        String forwarded = http.getHeader("X-Forwarded-For");
        String ip = forwarded != null ? forwarded.split(",")[0].trim() : http.getRemoteAddr();
        String device = http.getHeader("User-Agent");
        long locked = attempts.minutesLocked(username);
        if (locked > 0) {
            loginEvents.save(new LoginEvent(username, null, LoginEvent.Outcome.LOCKED, ip, device));
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many wrong passwords. Try again in " + locked + " minute" + (locked == 1 ? "" : "s") + ".");
        }
        // log in with the username or the email address
        User account = users.findByUsername(username)
                .or(() -> users.findFirstByEmailIgnoreCase(username))
                .orElse(null);
        User user = account != null && encoder.matches(req.password(), account.getPasswordHash()) ? account : null;
        if (user == null) {
            attempts.failed(username);
            // the history names the account someone tried, so an admin can spot password guessing
            loginEvents.save(account == null
                    ? new LoginEvent(username, null, LoginEvent.Outcome.WRONG_PASSWORD, ip, device)
                    : new LoginEvent(account.getUsername(), account.getFullName(), LoginEvent.Outcome.WRONG_PASSWORD, ip, device));
            // same message whether the username or the password was wrong
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong username or password");
        }
        // only said after the right password, so it doesn't reveal which accounts exist
        if (!user.isEnabled()) {
            loginEvents.save(new LoginEvent(user.getUsername(), user.getFullName(), LoginEvent.Outcome.NOT_ACTIVE, ip, device));
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This account isn't active. An admin has to approve or re-enable it.");
        }
        attempts.succeeded(username);
        loginEvents.save(new LoginEvent(user.getUsername(), user.getFullName(), LoginEvent.Outcome.SUCCESS, ip, device));
        Instant now = Instant.now();
        users.markLogin(user.getId(), now);
        user.setLastLoginAt(now);
        user.setLastSeenAt(now);
        return new LoginResponse(jwtService.createToken(user), UserResponse.from(user));
    }

    /**
     * Anyone can ask for an account, with a name, email and password only. Every new
     * account is a Viewer and starts switched off, so it can see nothing until an admin
     * approves it on the Users page (where the admin also picks the role).
     */
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String, String> signup(@RequestBody Map<String, Object> body) {
        // read as a plain map so extra fields such as "role" or "enabled" are refused, not silently dropped
        Set<String> extra = new TreeSet<>(body.keySet());
        extra.removeAll(SIGNUP_FIELDS);
        if (!extra.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only name, email and password can be sent. Not allowed: " + String.join(", ", extra));
        }
        SignupRequest req = new SignupRequest(text(body, "name"), text(body, "email"), text(body, "password"));
        var problems = validator.validate(req);
        if (!problems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, problems.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage()).sorted()
                    .reduce((a, b) -> a + ", " + b).orElse("Invalid sign-up"));
        }
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByUsername(email) || users.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException("An account with this email already exists");
        }
        // the server decides the role and access, never the request
        User user = new User(email, encoder.encode(req.password()), req.name().trim(), Role.VIEWER);
        user.setEmail(email);
        user.setEnabled(false);
        users.save(user);
        return Map.of("message", "Account created. An admin has to approve it before you can log in.");
    }

    private static String text(Map<String, Object> body, String field) {
        Object v = body.get(field);
        if (v != null && !(v instanceof String)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must be text");
        }
        return (String) v;
    }

    @GetMapping("/me")
    public UserResponse me(Authentication auth) {
        return users.findByUsername(auth.getName())
                .map(UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in"));
    }

    /** Your own latest 20 sign-in attempts, including wrong passwords typed for your account. */
    @GetMapping("/logins")
    public List<UserController.LoginView> myLogins(Authentication auth) {
        return loginEvents.findTop20ByUsernameOrderByIdDesc(auth.getName()).stream()
                .map(UserController.LoginView::from).toList();
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
