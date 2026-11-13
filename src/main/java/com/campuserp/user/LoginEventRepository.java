package com.campuserp.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoginEventRepository extends JpaRepository<LoginEvent, Long> {

    // shortcut: old rows are never deleted, add a clean-up job if the table grows past ~1M rows
    List<LoginEvent> findTop200ByOrderByIdDesc();
}
