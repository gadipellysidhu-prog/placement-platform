package com.college.placement.modules.student.repository;

import com.college.placement.modules.student.domain.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SkillRepository extends JpaRepository<Skill, UUID> {

    Optional<Skill> findByName(String name);

    boolean existsByName(String name);

    List<Skill> findByCategory(String category);

    List<Skill> findByVerifiedTrue();

    Optional<Skill> findByNameIgnoreCase(String name);

    List<Skill> findByActiveTrue();

    /** Active skills whose name contains the query (case-insensitive) — catalog search. */
    @Query("select s from Skill s where s.active = true "
            + "and lower(s.name) like lower(concat('%', :q, '%'))")
    List<Skill> searchByNameContaining(@Param("q") String q);

    /** Atomic popularity bump — avoids optimistic-lock churn on hot skills. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Skill s set s.popularityScore = s.popularityScore + 1 where s.id = :id")
    void incrementPopularity(@Param("id") UUID id);
}
