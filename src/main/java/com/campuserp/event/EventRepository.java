package com.campuserp.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findAllByOrderByStartDateDesc();

    List<Event> findByEndDateGreaterThanEqualAndStartDateLessThanEqualAndStatusInOrderByStartDateAsc(
            LocalDate from, LocalDate to, Collection<EventStatus> statuses);

    List<Event> findTop5ByNameContainingIgnoreCaseOrderByStartDateDesc(String name);
}
