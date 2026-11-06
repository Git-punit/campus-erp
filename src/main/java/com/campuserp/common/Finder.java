package com.campuserp.common;

import org.springframework.data.jpa.repository.JpaRepository;

public final class Finder {

    private Finder() {}

    public static <T> T find(JpaRepository<T, Long> repo, Long id, String what) {
        if (id == null) {
            throw new BusinessRuleException(what + " is required");
        }
        return repo.findById(id).orElseThrow(() -> NotFoundException.of(what, id));
    }

    /** Delete and flush straight away, so "still in use" errors surface here as a 409. */
    public static <T> void delete(JpaRepository<T, Long> repo, Long id, String what) {
        T entity = find(repo, id, what);
        repo.delete(entity);
        repo.flush();
    }
}
