package com.example.tb.authentication.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.tb.authentication.service.audience.AudienceService;
import com.example.tb.exception.RequestIncorrectException;
import com.example.tb.model.dto.UserDTO;
import com.example.tb.model.dto.VerificationResponseDTO;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1/audiences")
@CrossOrigin
@SecurityRequirement(name = "bearerAuth")
public class AudienceController {
    private final AudienceService audienceService;

    public AudienceController(AudienceService audienceService) {
        this.audienceService = audienceService;
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<UserDTO>> getAudiencesByEventId(@PathVariable UUID eventId) {
        return ResponseEntity.ok(audienceService.getAudiencesByEventId(eventId));
    }

    @GetMapping("/event/{eventId}/count")
    public ResponseEntity<Long> getAudienceCountByEventId(@PathVariable UUID eventId) {
        return ResponseEntity.ok(audienceService.getAudienceCountByEventId(eventId));
    }

    @GetMapping("/event/{eventId}/check/{userId}")
    public ResponseEntity<Boolean> isUserRegisteredForEvent(
            @PathVariable UUID eventId,
            @PathVariable UUID userId) {
        return ResponseEntity.ok(audienceService.isUserRegisteredForEvent(eventId, userId));
    }

    @PostMapping("/verify")
    public ResponseEntity<VerificationResponseDTO> verifyRegistration(
            @RequestParam UUID eventId,
            @RequestParam UUID userId) {
        return ResponseEntity.ok(audienceService.verifyRegistration(eventId, userId));
    }

    @GetMapping("/qrcode")
    public ResponseEntity<?> getQRCodeImage(@RequestParam String fileName) {
        try {
            Resource file = audienceService.getQRCodeImage(fileName);
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(file);
        } catch (Exception e) {
            throw new RequestIncorrectException("Incorrect Name", "This QR code file doesn't exist.");
        }
    }
}
