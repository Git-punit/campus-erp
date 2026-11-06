package com.campuserp.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    /** rows of [facultyId, number of projects] */
    @Query("select m.faculty.id, count(distinct m.project.id) from ProjectMember m group by m.faculty.id")
    List<Object[]> countProjectsPerFaculty();
}
