package com.campuserp.student;

import com.campuserp.course.CourseOffering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EnrolmentRepository extends JpaRepository<Enrolment, Long> {

    long countByOfferingId(Long offeringId);

    @org.springframework.data.jpa.repository.Query(
            "select e.offering.id, count(e) from Enrolment e where e.offering.id in :offeringIds group by e.offering.id")
    List<Object[]> countPerOffering(java.util.Collection<Long> offeringIds);

    boolean existsByStudentIdAndOfferingId(Long studentId, Long offeringId);

    Optional<Enrolment> findByStudentIdAndOfferingId(Long studentId, Long offeringId);

    @Query("select e.offering from Enrolment e where e.student.id = :studentId "
            + "and (:year is null or e.offering.academicYear.name = :year)")
    List<CourseOffering> findOfferingsOfStudent(Long studentId, String year);

    @Query("select e.student from Enrolment e where e.offering.id = :offeringId order by e.student.rollNo")
    List<Student> findStudentsOfOffering(Long offeringId);

    void deleteByStudentId(Long studentId);
}
