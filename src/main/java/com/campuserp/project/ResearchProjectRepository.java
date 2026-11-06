package com.campuserp.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ResearchProjectRepository extends JpaRepository<ResearchProject, Long> {

    List<ResearchProject> findAllByOrderBySanctionDateDesc();

    @Query("select m.project from ProjectMember m where m.faculty.id = :facultyId order by m.project.sanctionDate desc")
    List<ResearchProject> findByMember(Long facultyId);
}
