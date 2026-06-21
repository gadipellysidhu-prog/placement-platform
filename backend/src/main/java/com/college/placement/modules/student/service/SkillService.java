package com.college.placement.modules.student.service;

import com.college.placement.modules.student.domain.Skill;
import com.college.placement.modules.student.repository.SkillRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class SkillService {

    private final SkillRepository skillRepository;

    public SkillService(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    @Transactional
    public Skill createSkill(String name, String category) {
        if (skillRepository.existsByName(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Skill already exists");
        }
        Skill skill = new Skill();
        skill.setName(name);
        skill.setCategory(category);
        return skillRepository.save(skill);
    }

    @Transactional
    public Skill verify(UUID id) {
        Skill skill = getById(id);
        skill.setVerified(true);
        return skillRepository.save(skill);
    }

    @Transactional(readOnly = true)
    public Skill getById(UUID id) {
        return skillRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Skill not found"));
    }

    @Transactional(readOnly = true)
    public List<Skill> getAll() {
        return skillRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Skill> getByCategory(String category) {
        return skillRepository.findByCategory(category);
    }

    @Transactional(readOnly = true)
    public List<Skill> getVerified() {
        return skillRepository.findByVerifiedTrue();
    }
}
