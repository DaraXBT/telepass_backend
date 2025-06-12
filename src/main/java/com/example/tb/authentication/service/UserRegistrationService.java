package com.example.tb.authentication.service;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.example.tb.authentication.repository.user.UserRepository;
import com.example.tb.model.entity.User;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import jakarta.validation.Valid;

@Service
@Validated
public class UserRegistrationService {

    @Autowired
    private UserRepository userRepository;

    public User registerUser(@Valid User user) {
        // Check if phone number already exists
        if (userRepository.existsByPhoneNumber(user.getPhoneNumber())) {
            throw new RuntimeException("Phone number already registered");
        }

        // Save the user
        return userRepository.save(user);
    }

    // Generate QR Code with registration token
    public String generateQRCode(String registrationToken) throws Exception {
        // Generate QR code with registration URL
        String qrCodeData = "https://yourapp.com/register/" + registrationToken;

        // Generate QR Code
        BitMatrix matrix = new MultiFormatWriter().encode(
                qrCodeData,
                BarcodeFormat.QR_CODE,
                300,
                300);

        // Convert to image
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);

        // Convert to Base64
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    // Generate QR Code with eventId, userId, and registrationToken
    public String generateQRCode(String eventId, String userId, String registrationToken) throws Exception {
        String qrCodeData = eventId + "|" + userId + "|" + registrationToken;

        // Generate QR Code
        BitMatrix matrix = new MultiFormatWriter().encode(
                qrCodeData,
                BarcodeFormat.QR_CODE,
                300,
                300);

        // Convert to image
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);

        // Convert to Base64
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }
}