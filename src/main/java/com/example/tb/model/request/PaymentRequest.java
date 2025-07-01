package com.example.tb.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    
    @NotNull(message = "Event ID is required")
    private UUID eventId;
    
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1000.0", message = "Amount must be at least 1000 KHR")
    @DecimalMax(value = "50000000.0", message = "Amount cannot exceed 50,000,000 KHR")
    private BigDecimal amount;
    
    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^(KHR|USD)$", message = "Currency must be KHR or USD")
    private String currency;
    
    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;
    
    @NotBlank(message = "Payer name is required")
    @Size(min = 2, max = 100, message = "Payer name must be between 2 and 100 characters")
    private String payerName;
    
    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Invalid phone number format")
    private String payerPhone;
    
    @Email(message = "Invalid email format")
    private String payerEmail;
    
    private String returnUrl;
}
