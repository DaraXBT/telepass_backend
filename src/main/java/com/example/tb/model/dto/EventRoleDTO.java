package com.example.tb.model.dto;

import java.util.UUID;

import com.example.tb.model.entity.EventRole.EventRoleType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRoleDTO {
    private UUID id;
    private UUID userId;
    private String username;
    private EventRoleType role;
    private String email;
}
