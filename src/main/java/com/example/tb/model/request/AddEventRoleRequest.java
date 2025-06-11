package com.example.tb.model.request;

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
public class AddEventRoleRequest {
    private UUID userId;
    private EventRoleType role;
} 