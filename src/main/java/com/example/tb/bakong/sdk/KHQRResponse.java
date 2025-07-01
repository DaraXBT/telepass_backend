package com.example.tb.bakong.sdk;

import lombok.Data;

/**
 * Standard response wrapper for KHQR operations
 * Stub implementation matching official Bakong KHQR SDK interface
 */
@Data
public class KHQRResponse<T> {
    private KHQRStatus KHQRStatus;
    private T data;
}
