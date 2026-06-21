package com.college.placement.modules.student.service;

import com.college.placement.modules.auth.domain.AppUser;
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
        return saved;
    }

    @Transactional
    public Student updateProfile(UUID studentId, UUID branchId, BigDecimal cgpa, int currentYear) {
        Student student = getById(studentId);
        if (branchId != null) {
            student.setBranch(branchService.getById(branchId));
        }
        student.setCgpa(cgpa);
        student.setCurrentYear(currentYear);
        Student saved = studentRepository.save(student);
        eventPublisher.publish(StudentUpdatedEvent.of(studentId));
        return saved;
    }

    @Transactional
    public Student updateStatus(UUID studentId, StudentStatus newStatus) {
        Student student = getById(studentId);
        StudentStatus previous = student.getStatus();
        validateStatusTransition(previous, newStatus);
        student.setStatus(newStatus);
        Student saved = studentRepository.save(student);
        eventPublisher.publish(StudentStatusChangedEvent.of(studentId, previous, newStatus));
        return saved;
    }

    @Transactional
    public Student assignSkill(UUID studentId, UUID skillId) {
        Student student = getById(studentId);
        Skill skill = skillService.getById(skillId);
        student.getSkills().add(skill);
        Student saved = studentRepository.save(student);
        eventPublisher.publish(StudentSkillAddedEvent.of(studentId, skillId, skill.getName()));
        return saved;
    }

    @Transactional
    public Student removeSkill(UUID studentId, UUID skillId) {
        Student student = getById(studentId);
        Skill skill = skillService.getById(skillId);
        student.getSkills().remove(skill);
        Student saved = studentRepository.save(student);
        eventPublisher.publish(StudentSkillRemovedEvent.of(studentId, skillId, skill.getName()));
        return saved;
    }

    @Transactional
    public Student evaluateEligibility(UUID studentId) {
        Student student = getById(studentId);
        boolean eligible = student.getStatus() == StudentStatus.ACTIVE
                && student.getCgpa() != null
                && student.getCgpa().compareTo(new BigDecimal("5.0")) >= 0;
        student.setPlacementEligible(eligible);
        return studentRepository.save(student);
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
