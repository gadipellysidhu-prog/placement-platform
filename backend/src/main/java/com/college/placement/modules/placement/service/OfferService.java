package com.college.placement.modules.placement.service;

import com.college.placement.modules.placement.domain.ApplicationStatus;
import com.college.placement.modules.placement.domain.JobApplication;
import com.college.placement.modules.placement.domain.Offer;
import com.college.placement.modules.placement.domain.OfferStatus;
import com.college.placement.modules.placement.event.OfferAcceptedEvent;
import com.college.placement.modules.placement.event.OfferCreatedEvent;
import com.college.placement.modules.placement.event.OfferRejectedEvent;
import com.college.placement.modules.placement.repository.OfferRepository;
import com.college.placement.modules.student.domain.StudentStatus;
import com.college.placement.modules.student.repository.StudentRepository;
import com.college.placement.shared.eventbus.EventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class OfferService {

    private final OfferRepository offerRepository;
    private final JobApplicationService jobApplicationService;
    private final StudentRepository studentRepository;
    private final EventPublisher eventPublisher;

    public OfferService(OfferRepository offerRepository,
                        JobApplicationService jobApplicationService,
                        StudentRepository studentRepository,
                        EventPublisher eventPublisher) {
        this.offerRepository = offerRepository;
        this.jobApplicationService = jobApplicationService;
        this.studentRepository = studentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Offer createOffer(UUID applicationId, BigDecimal ctc, LocalDate joiningDate) {
        JobApplication application = jobApplicationService.getById(applicationId);

        if (application.getStatus() != ApplicationStatus.OFFERED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Offer can only be created for applications with status OFFERED");
        }
        if (offerRepository.existsByApplication(application)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An offer already exists for this application");
        }

        Offer offer = new Offer();
        offer.setApplication(application);
        offer.setCtc(ctc);
        offer.setJoiningDate(joiningDate);
        Offer saved = offerRepository.save(offer);
        eventPublisher.publish(OfferCreatedEvent.of(
                saved.getId(),
                applicationId,
                application.getStudent().getId(),
                application.getJobPosting().getCompany().getId(),
                ctc));
        return saved;
    }

    @Transactional
    public Offer acceptOffer(UUID offerId) {
        Offer offer = getById(offerId);
        requirePending(offer);
        offer.setStatus(OfferStatus.ACCEPTED);
        Offer saved = offerRepository.save(offer);

        var student = offer.getApplication().getStudent();
        student.setStatus(StudentStatus.PLACED);
        studentRepository.save(student);

        eventPublisher.publish(OfferAcceptedEvent.of(
                offerId,
                offer.getApplication().getId(),
                student.getId(),
                offer.getApplication().getJobPosting().getCompany().getId()));
        return saved;
    }

    @Transactional
    public Offer rejectOffer(UUID offerId) {
        Offer offer = getById(offerId);
        requirePending(offer);
        offer.setStatus(OfferStatus.REJECTED);
        Offer saved = offerRepository.save(offer);
        eventPublisher.publish(OfferRejectedEvent.of(
                offerId,
                offer.getApplication().getId(),
                offer.getApplication().getStudent().getId(),
                offer.getApplication().getJobPosting().getCompany().getId()));
        return saved;
    }

    @Transactional
    public Offer expireOffer(UUID offerId) {
        Offer offer = getById(offerId);
        requirePending(offer);
        offer.setStatus(OfferStatus.EXPIRED);
        return offerRepository.save(offer);
    }

    @Transactional(readOnly = true)
    public Offer getById(UUID id) {
        return offerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found"));
    }

    @Transactional(readOnly = true)
    public Offer getByApplication(UUID applicationId) {
        JobApplication application = jobApplicationService.getById(applicationId);
        return offerRepository.findByApplication(application)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No offer for this application"));
    }

    @Transactional(readOnly = true)
    public List<Offer> getByStatus(OfferStatus status) {
        return offerRepository.findByStatus(status);
    }

    private void requirePending(Offer offer) {
        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Only PENDING offers can be updated; current status: " + offer.getStatus());
        }
    }
}
