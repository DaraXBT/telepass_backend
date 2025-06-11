package com.example.tb.model.dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class EventDto {
    private UUID id;
    private String name;
    private String description;
    private String status;
    private String category;
    private int capacity;
    private int registered;
    private List<String> organizers;
}