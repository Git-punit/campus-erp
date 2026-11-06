package com.campuserp.budget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    @Query("select b from Budget b where (:type is null or b.type = :type) "
            + "and (:departmentId is null or b.department.id = :departmentId) "
            + "and (:fy is null or b.financialYear = :fy) order by b.financialYear desc, b.type, b.name")
    List<Budget> search(BudgetType type, Long departmentId, String fy);

    Optional<Budget> findByTypeAndDepartmentIdAndFinancialYear(BudgetType type, Long departmentId, String fy);

    Optional<Budget> findByEventId(Long eventId);

    Optional<Budget> findByProjectId(Long projectId);

    Optional<Budget> findByTrainingClientId(Long clientId);

    List<Budget> findByFinancialYear(String fy);
}
