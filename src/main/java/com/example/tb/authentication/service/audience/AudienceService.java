package com.example.tb.authentication.service.audience;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.Resource;

import com.example.tb.model.dto.UserDTO;
import com.example.tb.model.dto.VerificationResponseDTO;

public interface AudienceService {
    List<UserDTO> getAudiencesByEventId(UUID eventId);

    Long getAudienceCountByEventId(UUID eventId);

    boolean isUserRegisteredForEvent(UUID eventId, UUID userId);

    VerificationResponseDTO verifyRegistration(UUID eventId, UUID userId);

    VerificationResponseDTO verifyCheckIn(UUID eventId, UUID userId, String registrationToken);

    Resource getQRCodeImage(String fileName) throws IOException;
}
