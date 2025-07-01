package com.example.tb.authentication.service.event;

import static com.example.tb.utils.QrCodeUtil.generateComplexQRCode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tb.authentication.repository.events.EventRepository;
import com.example.tb.authentication.repository.events.EventRoleRepository;
import com.example.tb.authentication.service.UserRegistrationService;
import com.example.tb.model.dto.EventRoleDTO;
import com.example.tb.model.entity.Admin;
import com.example.tb.model.entity.Event;
import com.example.tb.model.entity.EventRole;
import com.example.tb.model.entity.User;
import com.example.tb.model.request.AddEventRoleRequest;
import com.example.tb.model.request.EventRequest;
import com.example.tb.model.request.EventRoleRequest;
import com.example.tb.model.response.EventResponse;
import com.example.tb.utils.QrCodeUtil;
import com.google.zxing.WriterException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
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
    protected EventResponse convertToResponse(Event event) {
        List<EventRole> roles = eventRoleRepository.findByEventId(event.getId());
        
        // Safely handle registeredUsers to avoid lazy loading issues
        Set<UUID> registeredUserIds = new HashSet<>();
        try {
            if (event.getRegisteredUsers() != null) {
                registeredUserIds = event.getRegisteredUsers().stream()
                        .map(User::getId)
                        .collect(Collectors.toSet());
            }
        } catch (Exception e) {
            log.warn("Could not load registered users for event {}: {}", event.getId(), e.getMessage());
            // Continue with empty set
        }          return EventResponse.builder()
                .id(event.getId())
                .name(event.getName())                .description(event.getDescription())                .status(event.getStatus())
                .category(event.getCategory())
                .capacity(event.getCapacity())
                .registered(event.getRegistered())
                .qrCodePath(event.getQrCodePath())
                .eventImg(event.getEventImg())
                .adminId(event.getAdminId())
                .startDateTime(event.getStartDateTime())
                .endDateTime(event.getEndDateTime())
                .location(event.getLocation())
                .ticketPrice(event.getTicketPrice())
                .currency(event.getCurrency())
                .paymentRequired(event.getPaymentRequired())
                .eventRoles(new ArrayList<>()) // Return empty list to avoid circular reference issues
                .registeredUsers(registeredUserIds)
                .build();
    }

    @Override
    @Transactional
    public Event createEvent(EventRequest eventRequest) {
        try {
            log.info("Starting event creation process for event: {}", eventRequest.getName());
            
            // Validate required fields
            if (eventRequest.getName() == null || eventRequest.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Event name is required");
            }            log.debug("Creating event entity from request");
            // Convert request to entity
            Event event = Event.builder()
                    .name(eventRequest.getName())                    .description(eventRequest.getDescription())
                    .status(eventRequest.getStatus())
                    .category(eventRequest.getCategory())
                    .capacity(eventRequest.getCapacity())
                    .registered(eventRequest.getRegistered())
                    .eventImg(eventRequest.getEventImg())
                    .adminId(eventRequest.getAdminId())
                    .startDateTime(eventRequest.getStartDateTime())
                    .endDateTime(eventRequest.getEndDateTime())
                    .location(eventRequest.getLocation())
                    .ticketPrice(eventRequest.getTicketPrice())
                    .currency(eventRequest.getCurrency() != null ? eventRequest.getCurrency() : "KHR")
                    .paymentRequired(eventRequest.getPaymentRequired() != null ? eventRequest.getPaymentRequired() : false)
                    .build();

            // Save the event
            log.debug("Saving event to database");
            Event savedEvent = eventRepository.save(event);
            log.info("Event saved successfully with ID: {}", savedEvent.getId());

            // Generate QR code
            log.debug("Generating QR code for event: {}", savedEvent.getId());
            String qrFileName = "event_" + savedEvent.getId() + ".png";
            String qrFilePath = "src/main/resources/storage/" + qrFileName;
            
            try {
                // Ensure storage directory exists
                File storageDir = new File("src/main/resources/storage");
                if (!storageDir.exists()) {
                    log.debug("Creating storage directory: {}", storageDir.getAbsolutePath());
                    storageDir.mkdirs();
                }
                
                String telegramCommand = "https://t.me/telepasskhbot?start=event_" + savedEvent.getId();
                log.debug("Generating QR code with command: {}", telegramCommand);
                QrCodeUtil.generateComplexQRCode(telegramCommand, 300, 300, qrFilePath);
                
                savedEvent.setQrCodePath("storage/" + qrFileName);
                eventRepository.save(savedEvent);
                log.info("QR code generated and saved successfully");
            } catch (Exception e) {
                log.error("Failed to generate QR code for event {}: {}", savedEvent.getId(), e.getMessage(), e);
                // Continue without QR code - don't fail the entire operation
            }            // Create initial owner role if specified in request
            handleEventRoles(savedEvent.getId(), eventRequest.getEventRoles(), savedEvent, false);

            log.info("Event creation completed successfully for ID: {}", savedEvent.getId());
            return savedEvent;
            
        } catch (Exception e) {
            log.error("Event creation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create event: " + e.getMessage(), e);
        }
    }    @Override
    @Transactional
    public Event updateEvent(UUID id, EventRequest eventRequest) {
        try {
            log.info("Starting event update process for event ID: {}", id);
            log.debug("Update request received: {}", eventRequest);
            
            // Validate input
            if (id == null) {
                throw new IllegalArgumentException("Event ID cannot be null");
            }
            if (eventRequest == null) {
                throw new IllegalArgumentException("Event request cannot be null");
            }
            
            log.debug("Looking for existing event with ID: {}", id);
            Event existingEvent = eventRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Event not found with ID: " + id));
            
            log.info("Found existing event: '{}' (ID: {})", existingEvent.getName(), existingEvent.getId());            log.debug("Current event state - Name: {}, AdminId: {}, Status: {}, Category: {}, Capacity: {}, TicketPrice: {}, Currency: {}, PaymentRequired: {}", 
                    existingEvent.getName(), existingEvent.getAdminId(), existingEvent.getStatus(), 
                    existingEvent.getCategory(), existingEvent.getCapacity(), existingEvent.getTicketPrice(), 
                    existingEvent.getCurrency(), existingEvent.getPaymentRequired());
            
            // Log what's being updated
            log.debug("Updating event fields...");
            if (!Objects.equals(existingEvent.getName(), eventRequest.getName())) {
                log.debug("Name changing from '{}' to '{}'", existingEvent.getName(), eventRequest.getName());
            }
            if (!Objects.equals(existingEvent.getAdminId(), eventRequest.getAdminId())) {
                log.debug("AdminId changing from '{}' to '{}'", existingEvent.getAdminId(), eventRequest.getAdminId());
            }
            if (!Objects.equals(existingEvent.getStatus(), eventRequest.getStatus())) {
                log.debug("Status changing from '{}' to '{}'", existingEvent.getStatus(), eventRequest.getStatus());
            }
            if (!Objects.equals(existingEvent.getCategory(), eventRequest.getCategory())) {
                log.debug("Category changing from '{}' to '{}'", existingEvent.getCategory(), eventRequest.getCategory());
            }
            if (existingEvent.getCapacity() != eventRequest.getCapacity()) {
                log.debug("Capacity changing from {} to {}", existingEvent.getCapacity(), eventRequest.getCapacity());
            }            if (existingEvent.getRegistered() != eventRequest.getRegistered()) {
                log.debug("Registered count changing from {} to {}", existingEvent.getRegistered(), eventRequest.getRegistered());
            }
            if (!Objects.equals(existingEvent.getTicketPrice(), eventRequest.getTicketPrice())) {
                log.debug("Ticket price changing from {} to {}", existingEvent.getTicketPrice(), eventRequest.getTicketPrice());
            }
            if (!Objects.equals(existingEvent.getCurrency(), eventRequest.getCurrency())) {
                log.debug("Currency changing from '{}' to '{}'", existingEvent.getCurrency(), eventRequest.getCurrency());
            }
            if (!Objects.equals(existingEvent.getPaymentRequired(), eventRequest.getPaymentRequired())) {
                log.debug("Payment required changing from {} to {}", existingEvent.getPaymentRequired(), eventRequest.getPaymentRequired());
            }// Update event fields
            existingEvent.setName(eventRequest.getName());
            existingEvent.setDescription(eventRequest.getDescription());
            existingEvent.setStatus(eventRequest.getStatus());
            existingEvent.setCategory(eventRequest.getCategory());            existingEvent.setCapacity(eventRequest.getCapacity());
            existingEvent.setRegistered(eventRequest.getRegistered());
            existingEvent.setEventImg(eventRequest.getEventImg());
            existingEvent.setAdminId(eventRequest.getAdminId());
            existingEvent.setStartDateTime(eventRequest.getStartDateTime());
            existingEvent.setEndDateTime(eventRequest.getEndDateTime());
            existingEvent.setLocation(eventRequest.getLocation());
            
            // Update payment-related fields
            existingEvent.setTicketPrice(eventRequest.getTicketPrice());
            existingEvent.setCurrency(eventRequest.getCurrency());
            existingEvent.setPaymentRequired(eventRequest.getPaymentRequired());              log.debug("All fields updated, saving to database...");
            Event savedEvent = eventRepository.save(existingEvent);

            // VERIFICATION: Re-fetch from database to confirm persistence
            log.debug("Verifying database persistence...");
            Event verificationEvent = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found during verification"));
            
            log.info("=== DATABASE PERSISTENCE VERIFICATION ===");
            log.info("Saved ticketPrice: {} | Verified ticketPrice: {}", 
                    savedEvent.getTicketPrice(), verificationEvent.getTicketPrice());
            log.info("Saved currency: {} | Verified currency: {}", 
                    savedEvent.getCurrency(), verificationEvent.getCurrency());
            log.info("Saved paymentRequired: {} | Verified paymentRequired: {}", 
                    savedEvent.getPaymentRequired(), verificationEvent.getPaymentRequired());
            
            // Check for critical mismatches
            if (!Objects.equals(savedEvent.getTicketPrice(), verificationEvent.getTicketPrice())) {
                log.error("CRITICAL: Ticket price mismatch! Saved: {}, Verified: {}", 
                        savedEvent.getTicketPrice(), verificationEvent.getTicketPrice());
            }
            if (!Objects.equals(savedEvent.getCurrency(), verificationEvent.getCurrency())) {
                log.error("CRITICAL: Currency mismatch! Saved: {}, Verified: {}", 
                        savedEvent.getCurrency(), verificationEvent.getCurrency());
            }
            if (!Objects.equals(savedEvent.getPaymentRequired(), verificationEvent.getPaymentRequired())) {
                log.error("CRITICAL: Payment required mismatch! Saved: {}, Verified: {}", 
                        savedEvent.getPaymentRequired(), verificationEvent.getPaymentRequired());
            }

            // Handle event roles update if specified in request
            handleEventRoles(savedEvent.getId(), eventRequest.getEventRoles(), savedEvent, true);
            
            log.info("Successfully updated event with ID: {}", savedEvent.getId());            log.debug("Final event state - Name: {}, AdminId: {}, Status: {}, Category: {}, Capacity: {}, TicketPrice: {}, Currency: {}, PaymentRequired: {}", 
                    savedEvent.getName(), savedEvent.getAdminId(), savedEvent.getStatus(), 
                    savedEvent.getCategory(), savedEvent.getCapacity(), savedEvent.getTicketPrice(), 
                    savedEvent.getCurrency(), savedEvent.getPaymentRequired());
            
            return savedEvent;
            
        } catch (RuntimeException e) {
            log.error("Runtime exception during event update for ID {}: {}", id, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected exception during event update for ID {}: {}", id, e.getMessage(), e);            throw new RuntimeException("Failed to update event: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public EventResponse updateEventAndReturnResponse(UUID id, EventRequest eventRequest) {
        Event updatedEvent = updateEvent(id, eventRequest);
        return convertToResponse(updatedEvent);
    }

    @Override
    @Transactional
    public void deleteEvent(UUID id) {
        log.info("Attempting to delete event with ID: {}", id);
        
        // Check if event exists
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with ID: " + id));
        
        log.info("Found event '{}' to delete", event.getName());
        
        try {
            // Delete associated event roles first
            eventRoleRepository.deleteByEventId(id);
            log.debug("Deleted event roles for event ID: {}", id);
            
            // Delete the event
            eventRepository.deleteById(id);
            log.info("Successfully deleted event with ID: {}", id);
            
        } catch (Exception e) {
            log.error("Failed to delete event with ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to delete event: " + e.getMessage(), e);
        }
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

    @Override    @Transactional
    public String registerUserForEvent(UUID eventId, User user) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        
        // Ensure user is attached to the current session
        User managedUser = entityManager.merge(user);
        
        // Only add user to event's collection (avoid bidirectional for now)
        event.getRegisteredUsers().add(managedUser);
        
        eventRepository.save(event);

        try {
            return userRegistrationService.generateQRCode(
                    eventId.toString(),
                    managedUser.getId().toString(),
                    managedUser.getRegistrationToken());
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
    }    @Override
    public List<EventRole> getEventRoles(UUID eventId) {
        // Check if the event exists
        if (!eventRepository.existsById(eventId)) {
            throw new RuntimeException("Event not found");        }

        return eventRoleRepository.findByEventId(eventId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventRoleDTO> getEventRolesAsDTO(UUID eventId) {
        // Check if the event exists
        if (!eventRepository.existsById(eventId)) {
            throw new RuntimeException("Event not found");
        }

        List<EventRole> eventRoles = eventRoleRepository.findByEventId(eventId);
        return eventRoles.stream()
                .map(this::convertToEventRoleDTO)
                .collect(Collectors.toList());
    }

    private EventRoleDTO convertToEventRoleDTO(EventRole eventRole) {
        return EventRoleDTO.builder()
                .id(eventRole.getId())
                .userId(eventRole.getUser().getId())
                .username(eventRole.getUser().getUsername())
                .role(eventRole.getRole())
                .email(eventRole.getUser().getEmail())
                .build();
    }

    private void handleEventRoles(UUID eventId, List<EventRoleRequest> eventRoleRequests, Event event, boolean isUpdate) {
        if (eventRoleRequests == null || eventRoleRequests.isEmpty()) {
            log.debug("No event roles provided - skipping role handling");
            return;
        }

        log.debug("Handling {} event roles for event ID: {}", eventRoleRequests.size(), eventId);

        // If this is an update, remove existing roles first
        if (isUpdate) {
            log.debug("Removing existing event roles for event ID: {}", eventId);
            eventRoleRepository.deleteByEventId(eventId);
        }

        // Add new roles from request
        for (var roleRequest : eventRoleRequests) {
            try {
                if (roleRequest.getUserId() == null) {
                    log.warn("Skipping event role with null user ID");
                    continue;
                }

                Admin user = new Admin();
                user.setId(roleRequest.getUserId());

                EventRole role = EventRole.builder()
                        .event(event)
                        .user(user)
                        .role(roleRequest.getRole())
                        .build();
                eventRoleRepository.save(role);
                log.debug("Created event role for user: {} with role: {}", roleRequest.getUserId(), roleRequest.getRole());
            } catch (Exception e) {
                log.error("Failed to create event role for user {}: {}", roleRequest.getUserId(), e.getMessage(), e);
                // Continue with other roles - don't fail the entire operation
            }
        }
        log.debug("Event roles handling completed");
    }
    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getEventsByAdminId(UUID adminId) {
        log.info("Getting events for admin ID: {}", adminId);
        
        // Get all events directly by adminId
        List<Event> events = eventRepository.findByAdminId(adminId);
        
        // Convert events to responses
        List<EventResponse> eventResponses = events.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
                
        log.info("Found {} events for admin ID: {}", eventResponses.size(), adminId);
        return eventResponses;
    }
}
