package com.example.tb.authentication.service.event;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.tb.model.entity.Event;
import com.example.tb.model.entity.EventRole;
import com.example.tb.model.entity.User;
import com.example.tb.model.request.AddEventRoleRequest;
import com.example.tb.model.request.EventRequest;
import com.example.tb.model.response.EventResponse;

public interface EventService {
    List<EventResponse> getAllEvents();

    Optional<EventResponse> getEventById(UUID id);

    Event createEvent(EventRequest eventRequest);

    Event updateEvent(UUID id, EventRequest eventRequest);

    void deleteEvent(UUID id);

    byte[] getEventQrCode(UUID eventId);

    String registerUserForEvent(UUID eventId, User user);

    // Event role methods
    EventRole addEventRole(UUID eventId, AddEventRoleRequest request);

    void removeEventRole(UUID eventId, UUID userId);

    List<EventRole> getEventRoles(UUID eventId);
}
