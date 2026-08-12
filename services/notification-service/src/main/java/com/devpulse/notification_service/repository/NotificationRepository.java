package com.devpulse.notification_service.repository;

import com.devpulse.notification_service.entities.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findAllByRecipientEmailOrderByCreatedAtDescIdDesc(
            String recipientEmail, Pageable pageable
    );
}
