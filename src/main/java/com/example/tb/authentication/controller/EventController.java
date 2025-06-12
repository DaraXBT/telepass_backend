package com.example.tb.authentication.controller;

import java.util.List;
import java.util.UUID;

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

@RestController
@RequestMapping("/api/v1/events")
@CrossOrigin
@SecurityRequirement(name = "bearerAuth")
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
        return eventService.createEvent(eventRequest);
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
}
