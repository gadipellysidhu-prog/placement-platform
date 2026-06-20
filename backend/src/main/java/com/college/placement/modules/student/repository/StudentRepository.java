package com.college.placement.modules.student.repository;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.student.domain.Branch;
import com.college.placement.modules.student.domain.Student;
import com.college.placement.modules.student.domain.StudentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID> {

    Optional<Student> findByUser(AppUser user);

    Optional<Student> findByRollNumber(String rollNumber);

    boolean existsByUser(AppUser user);

    boolean existsByRollNumber(String rollNumber);

    List<Student> findByBranch(Branch branch);

    List<Student> findByStatus(StudentStatus status);
}
