package com.campuserp.config;

import com.campuserp.auth.JwtAuthFilter;
import com.campuserp.auth.JwtService;
import com.campuserp.common.ApiError;
import com.campuserp.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

/**
 * Who can call what:
 * - /api/health and /api/auth/login: anyone
 * - /api/users/**: ADMIN only
 * - any other GET under /api: any logged-in user (VIEWER and up)
 * - any other POST/PUT/DELETE under /api: ADMIN or STAFF
 * Admin-only actions inside other modules (like approving expenses) use
 * {@code @PreAuthorize("hasRole('ADMIN')")} on the controller method.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JsonMapper json;

    public SecurityConfig(JsonMapper json) {
        this.json = json;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Login is checked in AuthController. This bean is only here so Spring Boot
    // doesn't create its default in-memory user with a random password.
    @Bean
    UserDetailsService userDetailsService(UserRepository users) {
        return username -> {
            throw new UsernameNotFoundException(username);
        };
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService, UserRepository users) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new JwtAuthFilter(jwtService, users), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) ->
                                writeError(req, res, HttpStatus.UNAUTHORIZED, "Please log in"))
                        .accessDeniedHandler((req, res, ex) ->
                                writeError(req, res, HttpStatus.FORBIDDEN, "You don't have permission to do that")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/signup").permitAll()
                        // calendar apps can't log in; the feed checks its own signed key instead
                        .requestMatchers(HttpMethod.GET, "/api/calendar/feed/**").permitAll()
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                        .requestMatchers("/api/auth/**").authenticated()
                        .requestMatchers("/api/**").hasAnyRole("ADMIN", "STAFF")
                        // the built React app is served from the same jar later on
                        .anyRequest().permitAll());
        return http.build();
    }

    private void writeError(HttpServletRequest req, HttpServletResponse res,
                            HttpStatus status, String message) throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        json.writeValue(res.getOutputStream(),
                ApiError.of(status.value(), status.getReasonPhrase(), message, req.getRequestURI()));
    }
}
