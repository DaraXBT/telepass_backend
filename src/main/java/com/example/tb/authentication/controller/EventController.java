package com.example.tb.authentication.controller;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.tb.authentication.service.event.EventService;
import com.example.tb.model.dto.EventRoleDTO;
import com.example.tb.model.entity.Event;
import com.example.tb.model.entity.EventRole;
import com.example.tb.model.request.AddEventRoleRequest;
import com.example.tb.model.request.EventRequest;
import com.example.tb.model.response.EventResponse;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/events")
@CrossOrigin
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class EventController {
    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public List<EventResponse> getAllEvents() {
        return eventService.getAllEvents();
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable UUID id) {
        return eventService.getEventById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/qrcode")
    public ResponseEntity<byte[]> getEventQrCode(@PathVariable UUID id) {
        byte[] qrCode = eventService.getEventQrCode(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        return new ResponseEntity<>(qrCode, headers, HttpStatus.OK);
    }    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@RequestBody EventRequest eventRequest) {
        try {
            log.info("Attempting to create event with name: {}", eventRequest.getName());
            log.debug("Event request details: {}", eventRequest);
            
            Event createdEvent = eventService.createEvent(eventRequest);
              // Convert to EventResponse to avoid serialization issues
            EventResponse eventResponse = EventResponse.builder()
                    .id(createdEvent.getId())
                    .name(createdEvent.getName())
                    .description(createdEvent.getDescription())
                    .status(createdEvent.getStatus())
                    .category(createdEvent.getCategory())
                    .capacity(createdEvent.getCapacity())
                    .registered(createdEvent.getRegistered())
                    .qrCodePath(createdEvent.getQrCodePath())
                    .eventImg(createdEvent.getEventImg())
                    .adminId(createdEvent.getAdminId())
                    .startDateTime(createdEvent.getStartDateTime())
                    .endDateTime(createdEvent.getEndDateTime())
                    .location(createdEvent.getLocation())
                    .eventRoles(new ArrayList<>()) // Empty list to avoid circular reference
                    .registeredUsers(new HashSet<>()) // Empty set to avoid lazy loading issues
                    .build();
            
            log.info("Successfully created event with ID: {}", createdEvent.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(eventResponse);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid request for creating event: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Failed to create event: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(@PathVariable UUID id, @RequestBody EventRequest eventRequest) {
        try {
            log.info("Attempting to update event with ID: {}", id);
            log.debug("Update request details: {}", eventRequest);
            log.debug("Request fields - Name: {}, AdminId: {}, Status: {}, Category: {}", 
                    eventRequest.getName(), eventRequest.getAdminId(), 
                    eventRequest.getStatus(), eventRequest.getCategory());
            
            EventResponse eventResponse = eventService.updateEventAndReturnResponse(id, eventRequest);
            
            log.info("Successfully updated event with ID: {}", id);
            log.debug("Updated event details - Name: {}, AdminId: {}, Status: {}", 
                    eventResponse.getName(), eventResponse.getAdminId(), eventResponse.getStatus());
            
            return ResponseEntity.ok(eventResponse);
            
        } catch (RuntimeException e) {
            log.error("Failed to update event with ID {}: {}", id, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("Unexpected error while updating event with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }@DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteEvent(@PathVariable UUID id) {
        try {
            log.info("Attempting to delete event with ID: {}", id);
            eventService.deleteEvent(id);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Event deleted successfully");
            response.put("eventId", id.toString());
            
            log.info("Successfully deleted event with ID: {}", id);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Failed to delete event with ID {}: {}", id, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("eventId", id.toString());
            
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            
        } catch (Exception e) {
            log.error("Unexpected error while deleting event with ID {}: {}", id, e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "An unexpected error occurred while deleting the event");
            errorResponse.put("eventId", id.toString());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/{eventId}/roles")
    public EventRole addEventRole(@PathVariable UUID eventId, @RequestBody AddEventRoleRequest request) {
        return eventService.addEventRole(eventId, request);
    }

    @DeleteMapping("/{eventId}/roles/{userId}")
    public void removeEventRole(@PathVariable UUID eventId, @PathVariable UUID userId) {
        eventService.removeEventRole(eventId, userId);
    }
    @GetMapping("/{eventId}/roles")
    public List<EventRoleDTO> getEventRoles(@PathVariable UUID eventId) {
        return eventService.getEventRolesAsDTO(eventId);
    }

    @GetMapping("/admin/{adminId}")
    public List<EventResponse> getEventsByAdminId(@PathVariable UUID adminId) {
        log.info("Getting events for admin ID: {}", adminId);
        return eventService.getEventsByAdminId(adminId);
    }    // Test endpoint without authentication
    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        log.info("Test endpoint called successfully");
        return ResponseEntity.ok("Event service is running on port 8080");
    }

    // Debug endpoint to get event details before update
    @GetMapping("/debug/{id}")
    public ResponseEntity<Map<String, Object>> debugEventDetails(@PathVariable UUID id) {
        try {
            log.info("Debug endpoint called for event ID: {}", id);
            
            Optional<EventResponse> eventOpt = eventService.getEventById(id);
            if (eventOpt.isEmpty()) {
                log.warn("Event not found for debug with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
              EventResponse event = eventOpt.get();
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("eventId", event.getId());
            debugInfo.put("name", event.getName());
            debugInfo.put("description", event.getDescription());
            debugInfo.put("status", event.getStatus());
            debugInfo.put("category", event.getCategory());
            debugInfo.put("capacity", event.getCapacity());
            debugInfo.put("registered", event.getRegistered());
            debugInfo.put("adminId", event.getAdminId());
            debugInfo.put("eventImg", event.getEventImg());
            debugInfo.put("qrCodePath", event.getQrCodePath());
            debugInfo.put("startDateTime", event.getStartDateTime());
            debugInfo.put("endDateTime", event.getEndDateTime());
            debugInfo.put("location", event.getLocation());
            
            log.info("Debug info retrieved for event: {}", event.getName());
            return ResponseEntity.ok(debugInfo);
            
        } catch (Exception e) {
            log.error("Error in debug endpoint for event ID {}: {}", id, e.getMessage(), e);
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("error", e.getMessage());
            errorInfo.put("eventId", id);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorInfo);
        }
    }
}
