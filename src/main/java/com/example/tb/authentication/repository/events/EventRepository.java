package com.example.tb.authentication.repository.events;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.tb.model.entity.Event;

public interface EventRepository extends JpaRepository<Event, UUID> {
    // Add custom queries if needed
}