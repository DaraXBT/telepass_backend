package com.example.tb.authentication.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    }

    @PostMapping
    public Event createEvent(@RequestBody EventRequest eventRequest) {
        try {
            log.info("Attempting to create event with name: {}", eventRequest.getName());
            log.debug("Event request details: {}", eventRequest);
            
            Event createdEvent = eventService.createEvent(eventRequest);
            
            log.info("Successfully created event with ID: {}", createdEvent.getId());
            return createdEvent;
        } catch (Exception e) {
            log.error("Failed to create event: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PutMapping("/{id}")
    public Event updateEvent(@PathVariable UUID id, @RequestBody EventRequest eventRequest) {
        return eventService.updateEvent(id, eventRequest);
    }

    @DeleteMapping("/{id}")
    public void deleteEvent(@PathVariable UUID id) {
        eventService.deleteEvent(id);
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
    public List<EventRole> getEventRoles(@PathVariable UUID eventId) {
        return eventService.getEventRoles(eventId);
    }

    // Test endpoint without authentication
    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        log.info("Test endpoint called successfully");
        return ResponseEntity.ok("Event service is running on port 8080");
    }
    
    @GetMapping("/debug")
    public ResponseEntity<String> getDebugInfo() {
        try {
            List<EventResponse> events = eventService.getAllEvents();
            return ResponseEntity.ok("Events count: " + events.size());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage() + " - " + e.getClass().getSimpleName());
        }
    }
    
    @GetMapping("/simple")
    public ResponseEntity<List<Map<String, Object>>> getSimpleEvents() {
        try {
            List<Event> events = eventService.getAllEvents().stream()
                    .map(eventResponse -> {
                        Event event = new Event();
                        event.setId(eventResponse.getId());
                        event.setName(eventResponse.getName());
                        event.setDescription(eventResponse.getDescription());
                        event.setStatus(eventResponse.getStatus());
                        event.setCategory(eventResponse.getCategory());
                        event.setCapacity(eventResponse.getCapacity());
                        event.setRegistered(eventResponse.getRegistered());
                        return event;
                    })
                    .collect(Collectors.toList());
            
            List<Map<String, Object>> simpleEvents = events.stream()
                    .map(event -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", event.getId());
                        map.put("name", event.getName());
                        map.put("description", event.getDescription());
                        map.put("status", event.getStatus());
                        map.put("category", event.getCategory());
                        map.put("capacity", event.getCapacity());
                        map.put("registered", event.getRegistered());
                        return map;
                    })
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(simpleEvents);
        } catch (Exception e) {
            log.error("Error getting simple events: ", e);
            return ResponseEntity.status(500).body(List.of(Map.of("error", e.getMessage())));
        }
    }
}
