package com.college.placement.modules.student.repository;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.student.domain.Branch;
import com.college.placement.modules.student.domain.Student;
import com.college.placement.modules.student.domain.StudentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID> {

    @EntityGraph(attributePaths = {"user", "branch", "skills"})
    Optional<Student> findById(UUID id);

    @EntityGraph(attributePaths = {"user", "branch", "skills"})
    Optional<Student> findByUser(AppUser user);

    // Fetch user+branch eagerly via JOIN FETCH (to-one only — no collection).
    // skills are loaded separately via @BatchSize(50) on the entity, avoiding
    // the HHH90003004 in-memory pagination warning caused by joining a collection.
    @Query(value = "SELECT DISTINCT s FROM Student s JOIN FETCH s.user LEFT JOIN FETCH s.branch",
           countQuery = "SELECT COUNT(s) FROM Student s")
    Page<Student> findAll(Pageable pageable);

    Optional<Student> findByRollNumber(String rollNumber);

    boolean existsByUser(AppUser user);

    boolean existsByRollNumber(String rollNumber);

    List<Student> findByBranch(Branch branch);

    List<Student> findByStatus(StudentStatus status);

    long countByStatus(StudentStatus status);
}
