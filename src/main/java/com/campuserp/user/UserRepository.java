package com.campuserp.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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

    // update just these columns, so a request can't overwrite a password or role changed at the same moment
    @Modifying
    @Transactional
    @Query("update User u set u.lastSeenAt = :now where u.id = :id")
    void markSeen(Long id, Instant now);

    @Modifying
    @Transactional
    @Query("update User u set u.lastLoginAt = :now, u.lastSeenAt = :now where u.id = :id")
    void markLogin(Long id, Instant now);
}
