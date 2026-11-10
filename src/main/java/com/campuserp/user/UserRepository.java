package com.campuserp.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    // first match: emails aren't a database key, but sign-up and My account keep them unique
    Optional<User> findFirstByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    long countByRoleAndEnabledTrue(Role role);

    List<User> findAllByOrderByUsernameAsc();
}
