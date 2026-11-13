package com.campuserp.user;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/** Admin only (enforced in SecurityConfig). */
@RestController
@RequestMapping("/api/users")
public class UserController {

    public record LoginView(Instant at, String username, String fullName, LoginEvent.Outcome outcome,
                            String ip, String userAgent) {}

    private final UserService service;
    private final LoginEventRepository loginEvents;

    public UserController(UserService service, LoginEventRepository loginEvents) {
        this.service = service;
        this.loginEvents = loginEvents;
    }

    @GetMapping
    public List<UserResponse> list() {
        return service.list().stream().map(UserResponse::from).toList();
    }

    /** The latest 200 sign-in attempts, newest first. */
    @GetMapping("/logins")
    public List<LoginView> logins() {
        return loginEvents.findTop200ByOrderByIdDesc().stream()
                .map(e -> new LoginView(e.getCreatedAt(), e.getUsername(), e.getFullName(), e.getOutcome(),
                        e.getIp(), e.getUserAgent()))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody UserRequests.Create req) {
        return UserResponse.from(service.create(req));
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserRequests.Update req) {
        return UserResponse.from(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
