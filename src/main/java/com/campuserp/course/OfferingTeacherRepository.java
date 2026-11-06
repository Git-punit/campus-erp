package com.campuserp.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OfferingTeacherRepository extends JpaRepository<OfferingTeacher, Long> {

    /** [facultyId, number of offerings] for one academic year */
    @Query("select t.faculty.id, count(distinct t.offering.id) from OfferingTeacher t "
            + "where t.offering.academicYear.name = :year group by t.faculty.id")
    List<Object[]> countOfferingsPerFaculty(String year);
}
