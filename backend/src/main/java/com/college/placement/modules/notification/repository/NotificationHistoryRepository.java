package com.college.placement.modules.notification.repository;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.notification.domain.NotificationHistory;
import com.college.placement.modules.notification.domain.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, UUID> {

    Page<NotificationHistory> findByUser(AppUser user, Pageable pageable);

    Page<NotificationHistory> findByStatus(NotificationStatus status, Pageable pageable);
}
