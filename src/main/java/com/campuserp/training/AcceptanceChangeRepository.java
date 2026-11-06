package com.campuserp.training;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcceptanceChangeRepository extends JpaRepository<AcceptanceChange, Long> {
    List<AcceptanceChange> findByClientIdOrderByDateAscIdAsc(Long clientId);

    void deleteByClientId(Long clientId);
}
