package com.college.placement.modules.notification.service;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.notification.domain.NotificationChannel;
import com.college.placement.modules.notification.domain.NotificationHistory;
import com.college.placement.modules.notification.domain.NotificationStatus;
import com.college.placement.modules.notification.repository.NotificationHistoryRepository;
import com.college.placement.shared.notification.email.EmailMessage;
import com.college.placement.shared.notification.email.EmailProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Records notification intent and dispatches it through the active channel
 * provider. Each dispatch persists a {@link NotificationHistory} row (which acts
 * as a per-attempt delivery log) and transitions it PENDING → SENT / FAILED.
 *
 * <p>On EMAIL delivery failure the underlying {@code EmailDeliveryException} is
 * rethrown so the calling transactional outbox can retry with backoff; the
 * FAILED history row remains as the audit trail for that attempt.
 */
@Slf4j
@Service
public class NotificationService {

    private final NotificationHistoryRepository notificationHistoryRepository;
    private final EmailProvider emailProvider;

    public NotificationService(NotificationHistoryRepository notificationHistoryRepository,
                               EmailProvider emailProvider) {
        this.notificationHistoryRepository = notificationHistoryRepository;
        this.emailProvider = emailProvider;
    }

    /**
     * Persists a PENDING record. Dispatch is handled separately by {@link #send}.
     * Retained for callers that only want to record intent.
     */
    @Transactional
    public NotificationHistory record(AppUser user, NotificationChannel channel,
                                      String subject, String body) {
        NotificationHistory history = new NotificationHistory();
        history.setUser(user);
        history.setChannel(channel);
        history.setSubject(subject);
        history.setBody(body);
        history.setStatus(NotificationStatus.PENDING);
        return notificationHistoryRepository.save(history);
    }

    /**
     * Records and dispatches a notification. For {@link NotificationChannel#EMAIL}
     * the message is sent via the active {@link EmailProvider}; other channels are
     * recorded and marked SENT (dedicated transports arrive in a later phase).
     */
    @Transactional
    public NotificationHistory send(AppUser user, NotificationChannel channel,
                                    String subject, String body) {
        NotificationHistory history = record(user, channel, subject, body);
        try {
            if (channel == NotificationChannel.EMAIL) {
                emailProvider.send(new EmailMessage(user.getEmail(), subject, body));
            }
            markSent(history);
            return history;
        } catch (RuntimeException ex) {
            markFailed(history);
            log.warn("NOTIFICATION_DELIVERY_FAILED userId={} channel={} subject={} error={}",
                    user.getId(), channel, subject, ex.getMessage());
            throw ex;
        }
    }

    @Transactional
    public void markSent(NotificationHistory history) {
        history.setStatus(NotificationStatus.SENT);
        history.setSentAt(Instant.now());
        notificationHistoryRepository.save(history);
    }

    @Transactional
    public void markFailed(NotificationHistory history) {
        history.setStatus(NotificationStatus.FAILED);
        notificationHistoryRepository.save(history);
    }

    @Transactional(readOnly = true)
    public Page<NotificationHistory> getByUser(AppUser user, Pageable pageable) {
        return notificationHistoryRepository.findByUser(user, pageable);
    }

    @Transactional(readOnly = true)
    public Page<NotificationHistory> getByStatus(NotificationStatus status, Pageable pageable) {
        return notificationHistoryRepository.findByStatus(status, pageable);
    }
}
