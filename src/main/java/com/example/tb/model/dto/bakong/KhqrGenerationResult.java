package com.example.tb.model.dto.bakong;

import lombok.Builder;
import lombok.Data;

/**
 * Result of KHQR generation
 */
@Data
@Builder
public class KhqrGenerationResult {
    private boolean success;
    private String qrCode;
    private String md5Hash;
    private Integer errorCode;
    private String errorMessage;
}
