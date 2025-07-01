package com.example.tb.bakong.sdk;

import lombok.Data;

/**
 * QR code data response containing the generated QR and MD5 hash
 * Stub implementation matching official Bakong KHQR SDK interface
 */
@Data
public class KHQRData {
    private String qr;   // The EMV QR code string
    private String md5;  // MD5 hash for verification
}
