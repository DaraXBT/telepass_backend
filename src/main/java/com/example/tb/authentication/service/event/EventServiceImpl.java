package com.example.tb.authentication.service.event;

import static com.example.tb.utils.QrCodeUtil.generateComplexQRCode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tb.authentication.repository.events.EventRepository;
import com.example.tb.authentication.repository.events.EventRoleRepository;
import com.example.tb.authentication.service.UserRegistrationService;
import com.example.tb.model.entity.Admin;
import com.example.tb.model.entity.Event;
import com.example.tb.model.entity.EventRole;
import com.example.tb.model.entity.User;
import com.example.tb.model.request.AddEventRoleRequest;
import com.example.tb.model.request.EventRequest;
import com.example.tb.model.response.EventResponse;
import com.example.tb.utils.QrCodeUtil;
import com.google.zxing.WriterException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final EventRoleRepository eventRoleRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private UserRegistrationService userRegistrationService;

    public EventServiceImpl(EventRepository eventRepository, EventRoleRepository eventRoleRepository) {
        this.eventRepository = eventRepository;
        this.eventRoleRepository = eventRoleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getAllEvents() {
        return eventRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EventResponse> getEventById(UUID id) {
        return eventRepository.findById(id)
                .map(this::convertToResponse);
    }

    @Transactional(readOnly = true)
    private EventResponse convertToResponse(Event event) {
        // Initialize the event to load all necessary collections
        entityManager.refresh(event);

        List<EventRole> roles = eventRoleRepository.findByEventId(event.getId());
        return EventResponse.builder()
                .id(event.getId())
                .name(event.getName())
                .description(event.getDescription())
                .status(event.getStatus())
                .category(event.getCategory())
                .capacity(event.getCapacity())
                .registered(event.getRegistered())
                .qrCodePath(event.getQrCodePath())
                .eventImg(event.getEventImg())
                .eventRoles(roles)
                .registeredUsers(event.getRegisteredUsers().stream()
                        .map(User::getId)
                        .collect(Collectors.toSet()))
                .build();
    }

    @Override
    @Transactional
    public Event createEvent(EventRequest eventRequest) {
        // Convert request to entity
        Event event = Event.builder()
                .name(eventRequest.getName())
                .description(eventRequest.getDescription())
                .status(eventRequest.getStatus())
                .category(eventRequest.getCategory())
                .capacity(eventRequest.getCapacity())
                .registered(eventRequest.getRegistered())
                .eventImg(eventRequest.getEventImg())
                .build();

        // Save the event
        Event savedEvent = eventRepository.save(event);

        // Generate QR code
        String qrFileName = "event_" + savedEvent.getId() + ".png";
        String qrFilePath = "src/main/resources/storage/" + qrFileName;
        try {
            String telegramCommand = "https://t.me/telepasskhbot?start=event_" + savedEvent.getId();
            QrCodeUtil.generateComplexQRCode(telegramCommand, 300, 300, qrFilePath);
            savedEvent.setQrCodePath("storage/" + qrFileName);
            eventRepository.save(savedEvent);
        } catch (Exception e) {
            // handle error
        }

        // Create initial owner role if specified in request
        if (eventRequest.getEventRoles() != null && !eventRequest.getEventRoles().isEmpty()) {
            for (var roleRequest : eventRequest.getEventRoles()) {
                if (roleRequest.getUserId() == null) {
                    throw new RuntimeException("User ID cannot be null for event role");
                }

                Admin user = new Admin();
                user.setId(roleRequest.getUserId());

                EventRole role = EventRole.builder()
                        .event(savedEvent)
                        .user(user)
                        .role(roleRequest.getRole())
                        .build();
                eventRoleRepository.save(role);
            }
        }

        return savedEvent;
    }

    @Override
    @Transactional
    public Event updateEvent(UUID id, EventRequest eventRequest) {
        Event existingEvent = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        // Update event fields
        existingEvent.setName(eventRequest.getName());
        existingEvent.setDescription(eventRequest.getDescription());
        existingEvent.setStatus(eventRequest.getStatus());
        existingEvent.setCategory(eventRequest.getCategory());
        existingEvent.setCapacity(eventRequest.getCapacity());
        existingEvent.setRegistered(eventRequest.getRegistered());
        existingEvent.setEventImg(eventRequest.getEventImg());

        return eventRepository.save(existingEvent);
    }

    @Override
    public void deleteEvent(UUID id) {
        eventRepository.deleteById(id);
    }

    @Override
    public byte[] getEventQrCode(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        String qrPath = "src/main/resources/storage/event_" + eventId + ".png";
        try {
            File qrFile = new File(qrPath);
            if (!qrFile.exists()) {
                // Generate QR code if it doesn't exist
                generateComplexQRCode(event.getId().toString(), 300, 300, qrPath);
            }
            return Files.readAllBytes(qrFile.toPath());
        } catch (IOException e) {
            throw new RuntimeException("Error reading QR code", e);
        } catch (WriterException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @Transactional
    public String registerUserForEvent(UUID eventId, User user) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        event.getRegisteredUsers().add(user);
        eventRepository.save(event);

        try {
            return userRegistrationService.generateQRCode(
                    eventId.toString(),
                    user.getId().toString(),
                    user.getRegistrationToken());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    @Override
    @Transactional
    public EventRole addEventRole(UUID eventId, AddEventRoleRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        // Check if the role already exists
        if (eventRoleRepository.existsByEventIdAndUserId(eventId, request.getUserId())) {
            throw new RuntimeException("User already has a role in this event");
        }

        Admin user = new Admin();
        user.setId(request.getUserId());

        EventRole role = EventRole.builder()
                .event(event)
                .user(user)
                .role(request.getRole())
                .build();

        return eventRoleRepository.save(role);
    }

    @Override
    @Transactional
    public void removeEventRole(UUID eventId, UUID userId) {
        // Check if the event exists
        if (!eventRepository.existsById(eventId)) {
            throw new RuntimeException("Event not found");
        }

        // Delete the role
        eventRoleRepository.deleteByEventIdAndUserId(eventId, userId);
    }

    @Override
    public List<EventRole> getEventRoles(UUID eventId) {
        // Check if the event exists
        if (!eventRepository.existsById(eventId)) {
            throw new RuntimeException("Event not found");
        }

        return eventRoleRepository.findByEventId(eventId);
    }
}
