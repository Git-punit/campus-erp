package com.campuserp.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CourseOfferingRepository extends JpaRepository<CourseOffering, Long> {

    @Query("select o from CourseOffering o where o.academicYear.id = :yearId and mod(o.semester, 2) = :parity")
    List<CourseOffering> findRunningTogether(Long yearId, int parity);

    @Query("select o from CourseOffering o where (:year is null or o.academicYear.name = :year) "
            + "and (:semester is null or o.semester = :semester) "
            + "and (:sectionId is null or o.section.id = :sectionId) "
            + "order by o.academicYear.oddSemStart desc, o.semester, o.section.code, o.course.code")
    List<CourseOffering> search(String year, Integer semester, Long sectionId);

    @Query("select distinct t.offering from OfferingTeacher t where t.faculty.id = :facultyId "
            + "and (:year is null or t.offering.academicYear.name = :year) "
            + "order by t.offering.semester")
    List<CourseOffering> findByTeacher(Long facultyId, String year);

    List<CourseOffering> findByAcademicYearNameAndSemesterAndSectionId(String year, int semester, Long sectionId);

    Optional<CourseOffering> findByCourseIdAndAcademicYearIdAndSemesterAndSectionId(Long courseId, Long yearId,
                                                                                     int semester, Long sectionId);

    long countByAcademicYearIdAndSemesterIn(Long yearId, List<Integer> semesters);
}
