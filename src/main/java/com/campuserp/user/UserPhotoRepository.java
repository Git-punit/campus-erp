package com.campuserp.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPhotoRepository extends JpaRepository<UserPhoto, Long> {

    Optional<UserPhoto> findByUserId(Long userId);
}
