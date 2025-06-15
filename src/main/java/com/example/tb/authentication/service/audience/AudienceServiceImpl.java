package com.example.tb.authentication.service.audience;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tb.authentication.repository.audience.AudienceRepository;
import com.example.tb.authentication.repository.events.EventRepository;
import com.example.tb.model.dto.UserDTO;
import com.example.tb.model.dto.VerificationResponseDTO;
import com.example.tb.model.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AudienceServiceImpl implements AudienceService {
    private final AudienceRepository audienceRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getAudiencesByEventId(UUID eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new RuntimeException("Event not found with id: " + eventId);
        }
        return audienceRepository.findAllByEventId(eventId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Long getAudienceCountByEventId(UUID eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new RuntimeException("Event not found with id: " + eventId);
        }
        return audienceRepository.countByEventId(eventId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserRegisteredForEvent(UUID eventId, UUID userId) {
        if (!eventRepository.existsById(eventId)) {
            throw new RuntimeException("Event not found with id: " + eventId);
        }
        return audienceRepository.existsByEventIdAndUserId(eventId, userId);
    }

    @Override
    @Transactional
    public VerificationResponseDTO verifyRegistration(UUID eventId, UUID userId) {
        if (!eventRepository.existsById(eventId)) {
            return new VerificationResponseDTO(false, "Event not found", null);
        }

        User user = audienceRepository.findById(userId)
                .orElse(null);

        if (user == null) {
            return new VerificationResponseDTO(false, "User not found", null);
        }

        boolean isRegistered = audienceRepository.existsByEventIdAndUserId(eventId, userId);
        if (!isRegistered) {
            return new VerificationResponseDTO(false, "User is not registered for this event", convertToDTO(user));
        }

        if (user.isCheckedIn()) {
            return new VerificationResponseDTO(false, "User already checked in for this event", convertToDTO(user));
        }

        // Mark as checked in
        user.setCheckedIn(true);
        audienceRepository.save(user);

        return new VerificationResponseDTO(true, "Registration verified and user checked in", convertToDTO(user));
    }

    @Override
    public Resource getQRCodeImage(String fileName) throws IOException {
        Path filePath = Paths.get("src/main/resources/qrcode", fileName);
        byte[] fileBytes = Files.readAllBytes(filePath);
        return new ByteArrayResource(fileBytes);
    }

    @Override
    @Transactional
    public VerificationResponseDTO verifyCheckIn(UUID eventId, UUID userId, String registrationToken) {
        if (!eventRepository.existsById(eventId)) {
            return new VerificationResponseDTO(false, "Event not found", null);
        }

        User user = audienceRepository.findById(userId)
                .orElse(null);

        if (user == null) {
            return new VerificationResponseDTO(false, "User not found", null);
        }

        // Verify registration token for security
        if (!registrationToken.equals(user.getRegistrationToken())) {
            return new VerificationResponseDTO(false, "Invalid registration token", null);
        }

        boolean isRegistered = audienceRepository.existsByEventIdAndUserId(eventId, userId);
        if (!isRegistered) {
            return new VerificationResponseDTO(false, "User is not registered for this event", convertToDTO(user));
        }

        if (user.isCheckedIn()) {
            return new VerificationResponseDTO(false, "User already checked in for this event", convertToDTO(user));
        }

        // Mark as checked in
        user.setCheckedIn(true);
        audienceRepository.save(user);

        return new VerificationResponseDTO(true, "Registration verified and user checked in successfully", convertToDTO(user));
    }

    private UserDTO convertToDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getGender(),
                user.getDateOfBirth(),
                user.getAddress(),
                user.getEmail(),
                user.getOccupation(),
                user.getRegistrationToken(),
                user.isCheckedIn(),
                user.getQrCode()
        );
    }
}
