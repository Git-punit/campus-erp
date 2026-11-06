package com.campuserp.student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByRollNo(String rollNo);

    @Query("select s from Student s where (:sectionId is null or s.section.id = :sectionId) "
            + "and (:semester is null or s.currentSemester = :semester) order by s.rollNo")
    List<Student> search(Long sectionId, Integer semester);

    List<Student> findBySectionIdAndCurrentSemesterAndStatus(Long sectionId, int semester, StudentStatus status);

    long countByStatus(StudentStatus status);
}
