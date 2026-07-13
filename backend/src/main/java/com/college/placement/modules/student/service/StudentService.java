package com.college.placement.modules.student.service;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.Role;
import com.college.placement.modules.student.domain.Branch;
import com.college.placement.modules.student.domain.Skill;
import com.college.placement.modules.student.domain.Student;
import com.college.placement.modules.student.domain.StudentStatus;
import com.college.placement.modules.student.event.StudentSkillAddedEvent;
import com.college.placement.modules.student.event.StudentSkillRemovedEvent;
import com.college.placement.modules.student.event.StudentStatusChangedEvent;
import com.college.placement.modules.student.repository.StudentRepository;
import com.college.placement.shared.eventbus.EventPublisher;
import com.college.placement.shared.eventbus.events.StudentCreatedEvent;
import com.college.placement.shared.eventbus.events.StudentUpdatedEvent;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final BranchService branchService;
    private final SkillService skillService;
    private final EventPublisher eventPublisher;

    public StudentService(StudentRepository studentRepository,
                          BranchService branchService,
                          SkillService skillService,
                          EventPublisher eventPublisher) {
        this.studentRepository = studentRepository;
        this.branchService = branchService;
        this.skillService = skillService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Student createStudent(AppUser user, String rollNumber, UUID branchId, int currentYear) {
        if (studentRepository.existsByUser(user)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Student profile already exists for this user");
        }
        if (studentRepository.existsByRollNumber(rollNumber)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Roll number already registered");
        }
        Student student = new Student();
        student.setUser(user);
        student.setRollNumber(rollNumber);
        student.setCurrentYear(currentYear);
        if (branchId != null) {
            Branch branch = branchService.getById(branchId);
            student.setBranch(branch);
        }
        Student saved = studentRepository.save(student);
        eventPublisher.publish(StudentCreatedEvent.of(saved.getId(), user.getId(), rollNumber));
        return getById(saved.getId());
    }

    /**
     * Approve a pending student registration: create the profile for an existing
     * {@code ROLE_STUDENT} account and link it. Delegates the actual creation (dedup
     * checks, persistence, {@code StudentCreatedEvent}) to {@link #createStudent} so the
     * approval path and {@code POST /api/students} share one code path. The profile's
     * existence is itself the approval marker — no separate request entity is tracked.
     *
     * <p>Approval requires the account's email to be verified. Email verification is an
     * authentication concern — everywhere in the platform {@code emailVerified} is set only
     * by consuming an emailed token (proof the user controls the inbox), and
     * {@code AuthService.login} refuses unverified accounts. Admin approval is an
     * authorization decision and must not fabricate that proof, so it gates on (rather than
     * grants) verification. This keeps the two controls orthogonal: the student proves email
     * ownership, the officer authorizes the profile, and an approved student can log in
     * immediately.
     */
    @Transactional
    public Student approveRegistration(AppUser user, String rollNumber, UUID branchId, int currentYear) {
        if (user.getRole() != Role.ROLE_STUDENT) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Only student accounts can be approved as student profiles");
        }
        if (!user.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Cannot approve — the student has not verified their email address yet.");
        }
        if (studentRepository.existsByUser(user)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This registration has already been approved");
        }
        return createStudent(user, rollNumber, branchId, currentYear);
    }

    @Transactional(readOnly = true)
    public Page<AppUser> getPendingRegistrations(Pageable pageable) {
        return studentRepository.findUsersWithoutStudentProfile(Role.ROLE_STUDENT, pageable);
    }

    @Transactional
    public Student updateProfile(UUID studentId, UUID branchId, BigDecimal cgpa, int currentYear) {
        Student student = getById(studentId);
        if (branchId != null) {
            student.setBranch(branchService.getById(branchId));
        }
        student.setCgpa(cgpa);
        student.setCurrentYear(currentYear);
        studentRepository.save(student);
        eventPublisher.publish(StudentUpdatedEvent.of(studentId));
        return getById(studentId);
    }

    @Transactional
    public Student updateStatus(UUID studentId, StudentStatus newStatus) {
        Student student = getById(studentId);
        StudentStatus previous = student.getStatus();
        validateStatusTransition(previous, newStatus);
        student.setStatus(newStatus);
        studentRepository.save(student);
        eventPublisher.publish(StudentStatusChangedEvent.of(studentId, previous, newStatus));
        return getById(studentId);
    }

    @Transactional
    public Student assignSkill(UUID studentId, UUID skillId) {
        Student student = getById(studentId);
        Skill skill = skillService.getById(skillId);
        student.getSkills().add(skill);
        studentRepository.save(student);
        eventPublisher.publish(StudentSkillAddedEvent.of(studentId, skillId, skill.getName()));
        return getById(studentId);
    }

    @Transactional
    public Student removeSkill(UUID studentId, UUID skillId) {
        Student student = getById(studentId);
        Skill skill = skillService.getById(skillId);
        student.getSkills().remove(skill);
        studentRepository.save(student);
        eventPublisher.publish(StudentSkillRemovedEvent.of(studentId, skillId, skill.getName()));
        return getById(studentId);
    }

    @Transactional
    public Student evaluateEligibility(UUID studentId) {
        Student student = getById(studentId);
        boolean eligible = student.getStatus() == StudentStatus.ACTIVE
                && student.getCgpa() != null
                && student.getCgpa().compareTo(new BigDecimal("5.0")) >= 0;
        student.setPlacementEligible(eligible);
        studentRepository.save(student);
        return getById(studentId);
    }

    @Transactional(readOnly = true)
    public Student getById(UUID id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
    }

    @Transactional(readOnly = true)
    public Student getByUser(AppUser user) {
        return studentRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found"));
    }

    @Transactional(readOnly = true)
    public List<Student> getByBranch(UUID branchId) {
        Branch branch = branchService.getById(branchId);
        return studentRepository.findByBranch(branch);
    }

    @Transactional(readOnly = true)
    public List<Student> getByStatus(StudentStatus status) {
        return studentRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public Page<Student> getAll(Pageable pageable) {
        Page<Student> page = studentRepository.findAll(pageable);
        // findAll fetch-joins user+branch but intentionally not the skills collection
        // (joining a collection breaks SQL-level pagination). With open-in-view=false the
        // response is mapped after the session closes, so initialise skills here — one
        // batched SELECT via @BatchSize(50) — to avoid a LazyInitializationException.
        page.getContent().forEach(student -> student.getSkills().size());
        return page;
    }

    private void validateStatusTransition(StudentStatus current, StudentStatus next) {
        boolean valid = switch (current) {
            case ACTIVE -> next == StudentStatus.PLACED || next == StudentStatus.OPTED_OUT
                    || next == StudentStatus.BLOCKED || next == StudentStatus.GRADUATED;
            case PLACED -> next == StudentStatus.GRADUATED;
            case OPTED_OUT -> next == StudentStatus.ACTIVE;
            case BLOCKED -> next == StudentStatus.ACTIVE;
            case GRADUATED -> false;
        };
        if (!valid) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Invalid status transition from " + current + " to " + next);
        }
    }
}
