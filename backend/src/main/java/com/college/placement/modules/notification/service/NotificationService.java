package com.college.placement.modules.notification.service;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.notification.domain.NotificationChannel;
import com.college.placement.modules.notification.domain.NotificationHistory;
import com.college.placement.modules.notification.domain.NotificationStatus;
import com.college.placement.modules.notification.repository.NotificationHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationHistoryRepository notificationHistoryRepository;

    public NotificationService(NotificationHistoryRepository notificationHistoryRepository) {
        this.notificationHistoryRepository = notificationHistoryRepository;
    }

    /**
     * Records a notification intent. Actual dispatch to external providers is handled
     * by the delivery pipeline in a later phase. This method only persists the history record.
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

    @Transactional(readOnly = true)
    public Page<NotificationHistory> getByUser(AppUser user, Pageable pageable) {
        return notificationHistoryRepository.findByUser(user, pageable);
    }

    @Transactional(readOnly = true)
    public Page<NotificationHistory> getByStatus(NotificationStatus status, Pageable pageable) {
        return notificationHistoryRepository.findByStatus(status, pageable);
    }
}
