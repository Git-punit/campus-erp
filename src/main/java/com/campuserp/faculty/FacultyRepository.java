package com.campuserp.faculty;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FacultyRepository extends JpaRepository<Faculty, Long> {
    Optional<Faculty> findByEmployeeId(String employeeId);

    List<Faculty> findAllByOrderByNameAsc();

    long countByStatus(FacultyStatus status);
}
