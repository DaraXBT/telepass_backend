package com.example.tb.model.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.example.tb.model.entity.User.Gender;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private UUID id;
    private String fullName;
    private String phoneNumber;
    private Gender gender;
    private LocalDate dateOfBirth;
    private String address;
    private String email;
    private String occupation;
    private String registrationToken;
}