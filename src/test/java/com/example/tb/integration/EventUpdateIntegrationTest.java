package com.example.tb.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.tb.authentication.repository.events.EventRepository;
import com.example.tb.model.entity.Event;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EventUpdateIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testUpdateEventPaymentFields() throws Exception {
        // Create a test event in the database
        Event testEvent = Event.builder()
                .id(UUID.randomUUID())
                .name("Test Event")
                .description("Test description")
                .status("active")
                .category("conference")
                .capacity(100)
                .registered(0)
                .adminId(UUID.randomUUID())
                .startDateTime(LocalDateTime.now().plusDays(1))
                .endDateTime(LocalDateTime.now().plusDays(1).plusHours(8))
                .location("Test Location")
                .ticketPrice(new BigDecimal("50.00"))
                .currency("KHR")
                .paymentRequired(true)
                .build();

        Event savedEvent = eventRepository.save(testEvent);

        // Create update request with new payment values
        String updateRequestJson = """
                {
                    "name": "Updated Test Event",
                    "description": "Updated description",
                    "status": "pending",
                    "category": "workshop",
                    "capacity": 150,
                    "registered": 5,
                    "adminId": "%s",
                    "startDateTime": "%s",
                    "endDateTime": "%s",
                    "location": "Updated Location",
                    "ticketPrice": 75.00,
                    "currency": "USD",
                    "paymentRequired": false
                }
                """.formatted(
                        savedEvent.getAdminId().toString(),
                        LocalDateTime.now().plusDays(2).toString(),
                        LocalDateTime.now().plusDays(2).plusHours(6).toString()
                );        // Perform the update request
        mockMvc.perform(put("/api/v1/events/{id}", savedEvent.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequestJson)).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Test Event"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.status").value("pending"))
                .andExpect(jsonPath("$.category").value("workshop"))
                .andExpect(jsonPath("$.capacity").value(150))
                .andExpect(jsonPath("$.registered").value(5))
                .andExpect(jsonPath("$.location").value("Updated Location"))
                .andExpect(jsonPath("$.ticketPrice").value(75.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.paymentRequired").value(false));

        // Verify the event was actually updated in the database
        Event updatedEvent = eventRepository.findById(savedEvent.getId()).orElseThrow();
        assertEquals("Updated Test Event", updatedEvent.getName());
        assertEquals(new BigDecimal("75.00"), updatedEvent.getTicketPrice());
        assertEquals("USD", updatedEvent.getCurrency());
        assertEquals(false, updatedEvent.getPaymentRequired());
    }
}
