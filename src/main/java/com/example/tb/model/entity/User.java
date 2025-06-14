package com.example.tb.model.entity;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    @Column(name = "full_name")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    @Column(name = "phone_number", unique = true)
    private String phoneNumber;

    @NotNull(message = "Gender is required")
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Address is required")
    @Size(min = 5, max = 255, message = "Address must be between 5 and 255 characters")
    private String address;

    @Column(name = "email")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Occupation is required")
    private String occupation;

    @Column(name = "registration_token", unique = true)
    private String registrationToken;

    @ManyToMany(mappedBy = "registeredUsers")
    private Set<Event> registeredEvents = new HashSet<>();    @Column(name = "checked_in")
    private boolean checkedIn = false;

    @Column(name = "qr_code")
    private String qrCode;

    public enum Gender {
        MALE, FEMALE, OTHER
    }


    // Constructors, getters, and setters
    public User() {
        this.registrationToken = UUID.randomUUID().toString();
    }

}