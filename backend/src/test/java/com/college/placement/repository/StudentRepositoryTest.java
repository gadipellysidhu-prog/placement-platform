package com.college.placement.repository;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.Role;
import com.college.placement.modules.student.domain.Branch;
import com.college.placement.modules.student.domain.Student;
import com.college.placement.modules.student.domain.StudentStatus;
import com.college.placement.modules.student.repository.StudentRepository;
import com.college.placement.shared.audit.JpaAuditingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class StudentRepositoryTest {

    @Autowired StudentRepository studentRepository;
    @Autowired TestEntityManager em;

    private AppUser user;
    private Branch branch;

    @BeforeEach
    void setUp() {
        user   = em.persistAndFlush(buildUser("student@test.com"));
        branch = em.persistAndFlush(buildBranch("Computer Science", "CS"));
    }

    // ── findByRollNumber ──────────────────────────────────────────────────────

    @Test
    void findByRollNumber_existingStudent_returnsStudent() {
        em.persistAndFlush(buildStudent(user, "CS2021001"));

        Optional<Student> result = studentRepository.findByRollNumber("CS2021001");

        assertThat(result).isPresent();
        assertThat(result.get().getRollNumber()).isEqualTo("CS2021001");
    }

    @Test
    void findByRollNumber_nonExisting_returnsEmpty() {
        assertThat(studentRepository.findByRollNumber("UNKNOWN")).isEmpty();
    }

    // ── existsByRollNumber ────────────────────────────────────────────────────

    @Test
    void existsByRollNumber_whenPresent_returnsTrue() {
        em.persistAndFlush(buildStudent(user, "CS2021002"));

        assertThat(studentRepository.existsByRollNumber("CS2021002")).isTrue();
    }

    @Test
    void existsByRollNumber_whenAbsent_returnsFalse() {
        assertThat(studentRepository.existsByRollNumber("NOPE")).isFalse();
    }

    // ── existsByUser ──────────────────────────────────────────────────────────

    @Test
    void existsByUser_whenStudentExists_returnsTrue() {
        em.persistAndFlush(buildStudent(user, "CS2021003"));

        assertThat(studentRepository.existsByUser(user)).isTrue();
    }

    @Test
    void existsByUser_whenNoStudent_returnsFalse() {
        AppUser unlinked = em.persistAndFlush(buildUser("unlinked@test.com"));

        assertThat(studentRepository.existsByUser(unlinked)).isFalse();
    }

    // ── findByUser ────────────────────────────────────────────────────────────

    @Test
    void findByUser_returnsStudentForUser() {
        Student saved = em.persistAndFlush(buildStudent(user, "CS2021004"));

        Optional<Student> result = studentRepository.findByUser(user);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(saved.getId());
    }

    // ── findByStatus ──────────────────────────────────────────────────────────

    @Test
    void findByStatus_filtersCorrectly() {
        em.persistAndFlush(buildStudent(user, "CS2021005"));

        AppUser user2 = em.persistAndFlush(buildUser("user2@test.com"));
        Student graduated = buildStudent(user2, "CS2021006");
        graduated.setStatus(StudentStatus.GRADUATED);
        em.persistAndFlush(graduated);

        List<Student> active = studentRepository.findByStatus(StudentStatus.ACTIVE);
        List<Student> grad   = studentRepository.findByStatus(StudentStatus.GRADUATED);

        assertThat(active).hasSize(1);
        assertThat(grad).hasSize(1);
        assertThat(grad.get(0).getRollNumber()).isEqualTo("CS2021006");
    }

    // ── findByBranch ──────────────────────────────────────────────────────────

    @Test
    void findByBranch_returnsStudentsInBranch() {
        Student s = buildStudent(user, "CS2021007");
        s.setBranch(branch);
        em.persistAndFlush(s);

        List<Student> result = studentRepository.findByBranch(branch);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRollNumber()).isEqualTo("CS2021007");
    }

    // ── unique constraint ─────────────────────────────────────────────────────

    @Test
    void save_duplicateRollNumber_throwsConstraintViolation() {
        em.persistAndFlush(buildStudent(user, "DUPROLL"));

        AppUser user2 = em.persistAndFlush(buildUser("other@test.com"));
        Student dup   = buildStudent(user2, "DUPROLL");

        assertThatThrownBy(() -> studentRepository.saveAndFlush(dup))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static AppUser buildUser(String email) {
        AppUser u = new AppUser();
        u.setEmail(email);
        u.setPasswordHash("$2a$10$hash");
        u.setRole(Role.ROLE_STUDENT);
        return u;
    }

    private static Branch buildBranch(String name, String code) {
        Branch b = new Branch();
        b.setName(name);
        b.setCode(code);
        return b;
    }

    private static Student buildStudent(AppUser user, String rollNumber) {
        Student s = new Student();
        s.setUser(user);
        s.setRollNumber(rollNumber);
        s.setCurrentYear(1);
        s.setStatus(StudentStatus.ACTIVE);
        return s;
    }
}
