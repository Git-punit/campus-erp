package com.campuserp.setup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SectionRepository extends JpaRepository<Section, Long> {
    Optional<Section> findByCode(String code);

    List<Section> findAllByOrderByCodeAsc();
}
