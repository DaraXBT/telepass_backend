package com.example.tb.model.request;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequest {
    private UUID id;
    private String name;
    private String description;
    private String status;
    private String category;
    private int capacity;
    private int registered;
    private String qrCodePath;
    private String eventImg;
    private UUID adminId; // Admin who creates/owns this event
    
    // New fields for event scheduling and location
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String location; // Can be text or URL for event location
    
    private List<EventRoleRequest> eventRoles;
    private Set<UUID> registeredUsers;
}