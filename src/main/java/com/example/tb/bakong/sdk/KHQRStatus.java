package com.example.tb.bakong.sdk;

import lombok.Data;

/**
 * Status information for KHQR operations
 * Stub implementation matching official Bakong KHQR SDK interface
 */
@Data
public class KHQRStatus {
    private int code;
    private String errorCode;
    private String message;
}
