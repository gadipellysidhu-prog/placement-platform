package com.college.placement.modules.student.repository;

import com.college.placement.modules.student.domain.SkillAlias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SkillAliasRepository extends JpaRepository<SkillAlias, UUID> {

    Optional<SkillAlias> findByAliasNormalized(String aliasNormalized);

    boolean existsByAliasNormalized(String aliasNormalized);

    List<SkillAlias> findBySkillId(UUID skillId);

    void deleteBySkillId(UUID skillId);

    /** Aliases whose text contains the query (case-insensitive) — used by catalog search. */
    @Query("select a from SkillAlias a join fetch a.skill s "
            + "where s.active = true and lower(a.alias) like lower(concat('%', :q, '%'))")
    List<SkillAlias> searchByAliasContaining(@Param("q") String q);
}
