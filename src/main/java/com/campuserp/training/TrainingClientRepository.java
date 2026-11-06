package com.campuserp.training;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainingClientRepository extends JpaRepository<TrainingClient, Long> {
    List<TrainingClient> findByProgramIdOrderByDateContactedDesc(Long programId);

    List<TrainingClient> findByProgramId(Long programId);

    long countByProposalSubmittedTrue();

    long countByAccepted(Acceptance accepted);

    long countByConductedTrue();
}
