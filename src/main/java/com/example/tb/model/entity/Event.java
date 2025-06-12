package com.example.tb.model.entity;

import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    private String status; // e.g., "Upcoming", "Ongoing"
    private String category;
    private int capacity;
    private int registered;
    private String qrCodePath;
    private String eventImg; // Path to event image
    private UUID adminId; // Admin who created/owns this event

    @OneToMany(mappedBy = "event")
    @JsonIgnore // Prevent circular reference during JSON serialization
    private Set<EventRole> eventRoles;

    @ManyToMany
    @JoinTable(name = "event_registrations", joinColumns = @JoinColumn(name = "event_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    @JsonIgnore // Prevent lazy loading issues during JSON serialization
    private Set<User> registeredUsers;
}
