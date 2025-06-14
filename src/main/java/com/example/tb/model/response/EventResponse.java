package com.example.tb.model.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.example.tb.model.entity.EventRole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {
    private UUID id;
    private String name;
    private String description;
    private String status;
    private String category;
    private int capacity;
    private int registered;
    private String qrCodePath;
    private String eventImg;
    private UUID adminId; // Admin who created/owns this event
    
    // New fields for event scheduling and location
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String location; // Can be text or URL for event location
    
    private List<EventRole> eventRoles;
    private Set<UUID> registeredUsers;
}