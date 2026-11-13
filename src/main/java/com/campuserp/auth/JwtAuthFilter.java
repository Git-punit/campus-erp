package com.campuserp.auth;

import com.campuserp.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * Reads "Authorization: Bearer <token>" and, if the token is valid and the
 * user still exists and is enabled, marks the request as logged in.
 * The role is looked up fresh each time, so role changes apply straight away.
 *
 * Not a @Component on purpose: Spring Boot would also register it as a plain
 * servlet filter. SecurityConfig adds it to the security chain instead.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository users;

    public JwtAuthFilter(JwtService jwtService, UserRepository users) {
        this.jwtService = jwtService;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            jwtService.read(header.substring(7)).ifPresent(claims -> users.findByUsername(claims.getSubject())
                    .filter(u -> u.isEnabled())
                    // a password change or "log out everywhere" since the token was issued ends it
                    .filter(u -> jwtService.isCurrent(claims, u))
                    .ifPresent(u -> {
                        // "last active" for the Users page; at most one write a minute per user
                        Instant now = Instant.now();
                        if (u.getLastSeenAt() == null || u.getLastSeenAt().isBefore(now.minusSeconds(60))) {
                            users.markSeen(u.getId(), now);
                        }
                        var auth = new UsernamePasswordAuthenticationToken(
                                u.getUsername(), null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }));
        }
        chain.doFilter(request, response);
    }
}
