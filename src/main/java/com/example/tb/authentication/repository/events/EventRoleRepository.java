package com.example.tb.authentication.repository.events;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.tb.model.entity.EventRole;

public interface EventRoleRepository extends JpaRepository<EventRole, UUID> {
    List<EventRole> findByEventId(UUID eventId);
    void deleteByEventIdAndUserId(UUID eventId, UUID userId);
    void deleteByEventId(UUID eventId);
    boolean existsByEventIdAndUserId(UUID eventId, UUID userId);
    List<EventRole> findByUserId(UUID userId);
}