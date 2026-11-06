package com.campuserp.setup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    List<Holiday> findAllByOrderByDateAsc();

    List<Holiday> findByDateBetweenOrderByDateAsc(LocalDate from, LocalDate to);

    boolean existsByDate(LocalDate date);
}
