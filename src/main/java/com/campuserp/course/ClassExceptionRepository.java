package com.campuserp.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface ClassExceptionRepository extends JpaRepository<ClassException, Long> {

    List<ClassException> findByOfferingIdOrderByDateAsc(Long offeringId);

    List<ClassException> findByOfferingIdInAndDateBetween(Collection<Long> offeringIds, LocalDate from, LocalDate to);

    void deleteByOfferingId(Long offeringId);
}
