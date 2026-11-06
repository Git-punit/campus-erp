package com.campuserp.setup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProgrammeRepository extends JpaRepository<Programme, Long> {
    Optional<Programme> findByCode(String code);

    List<Programme> findAllByOrderByCodeAsc();
}
