package com.example.tb.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResponseDTO {
    private boolean verified;
    private String message;
    private UserDTO user;
}