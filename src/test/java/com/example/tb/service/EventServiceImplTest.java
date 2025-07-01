package com.example.tb.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.tb.authentication.repository.events.EventRepository;
import com.example.tb.authentication.repository.events.EventRoleRepository;
import com.example.tb.authentication.service.UserRegistrationService;
import com.example.tb.authentication.service.event.EventServiceImpl;
import com.example.tb.model.entity.Event;
import com.example.tb.model.request.EventRequest;
import com.example.tb.model.response.EventResponse;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventRoleRepository eventRoleRepository;

    @Mock
    private UserRegistrationService userRegistrationService;

    @InjectMocks
    private EventServiceImpl eventService;

    private UUID eventId;
    private Event existingEvent;
    private EventRequest updateRequest;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        
        // Create an existing event
        existingEvent = Event.builder()
                .id(eventId)
                .name("Original Event")
                .description("Original description")
                .status("active")
                .category("conference")
                .capacity(100)
                .registered(0)
                .adminId(UUID.randomUUID())
                .startDateTime(LocalDateTime.now().plusDays(1))
                .endDateTime(LocalDateTime.now().plusDays(1).plusHours(8))
                .location("Original Location")
                .ticketPrice(new BigDecimal("50.00"))
                .currency("KHR")
                .paymentRequired(true)
                .build();

        // Create an update request with new payment fields
        updateRequest = EventRequest.builder()
                .name("Updated Event")
                .description("Updated description")
                .status("pending")
                .category("workshop")
                .capacity(150)
                .registered(5)
                .adminId(existingEvent.getAdminId())
                .startDateTime(LocalDateTime.now().plusDays(2))
                .endDateTime(LocalDateTime.now().plusDays(2).plusHours(6))
                .location("Updated Location")
                .ticketPrice(new BigDecimal("75.00"))
                .currency("USD")
                .paymentRequired(false)
                .build();
    }

    @Test
    void testUpdateEvent_Success() {
        // Arrange
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Event updatedEvent = eventService.updateEvent(eventId, updateRequest);

        // Assert
        assertNotNull(updatedEvent);
        assertEquals(updateRequest.getName(), updatedEvent.getName());
        assertEquals(updateRequest.getDescription(), updatedEvent.getDescription());
        assertEquals(updateRequest.getStatus(), updatedEvent.getStatus());
        assertEquals(updateRequest.getCategory(), updatedEvent.getCategory());
        assertEquals(updateRequest.getCapacity(), updatedEvent.getCapacity());
        assertEquals(updateRequest.getRegistered(), updatedEvent.getRegistered());
        assertEquals(updateRequest.getLocation(), updatedEvent.getLocation());
        
        // Verify payment fields are updated correctly
        assertEquals(updateRequest.getTicketPrice(), updatedEvent.getTicketPrice());
        assertEquals(updateRequest.getCurrency(), updatedEvent.getCurrency());
        assertEquals(updateRequest.getPaymentRequired(), updatedEvent.getPaymentRequired());

        verify(eventRepository).findById(eventId);
        verify(eventRepository).save(updatedEvent);
    }

    @Test
    void testUpdateEventAndReturnResponse_Success() {
        // Arrange
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRoleRepository.findByEventId(eventId)).thenReturn(java.util.Collections.emptyList());

        // Act
        EventResponse response = eventService.updateEventAndReturnResponse(eventId, updateRequest);

        // Assert
        assertNotNull(response);
        assertEquals(updateRequest.getName(), response.getName());
        assertEquals(updateRequest.getDescription(), response.getDescription());
        assertEquals(updateRequest.getStatus(), response.getStatus());
        assertEquals(updateRequest.getCategory(), response.getCategory());
        assertEquals(updateRequest.getCapacity(), response.getCapacity());
        assertEquals(updateRequest.getRegistered(), response.getRegistered());
        assertEquals(updateRequest.getLocation(), response.getLocation());
        
        // Verify payment fields are mapped correctly in response
        assertEquals(updateRequest.getTicketPrice(), response.getTicketPrice());
        assertEquals(updateRequest.getCurrency(), response.getCurrency());
        assertEquals(updateRequest.getPaymentRequired(), response.getPaymentRequired());
    }

    @Test
    void testUpdateEvent_EventNotFound() {
        // Arrange
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> eventService.updateEvent(eventId, updateRequest));
        
        assertTrue(exception.getMessage().contains("Event not found with ID"));
        verify(eventRepository).findById(eventId);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void testUpdateEvent_NullEventId() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> eventService.updateEvent(null, updateRequest));
        
        assertEquals("Event ID cannot be null", exception.getMessage());
        verify(eventRepository, never()).findById(any());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void testUpdateEvent_NullEventRequest() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> eventService.updateEvent(eventId, null));
        
        assertEquals("Event request cannot be null", exception.getMessage());
        verify(eventRepository, never()).findById(any());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void testUpdateEvent_PaymentFieldsOnly() {
        // Arrange - Test updating only payment-related fields
        EventRequest paymentOnlyRequest = EventRequest.builder()
                .name(existingEvent.getName()) // Keep existing
                .description(existingEvent.getDescription()) // Keep existing
                .status(existingEvent.getStatus()) // Keep existing
                .category(existingEvent.getCategory()) // Keep existing
                .capacity(existingEvent.getCapacity()) // Keep existing
                .registered(existingEvent.getRegistered()) // Keep existing
                .adminId(existingEvent.getAdminId()) // Keep existing
                .startDateTime(existingEvent.getStartDateTime()) // Keep existing
                .endDateTime(existingEvent.getEndDateTime()) // Keep existing
                .location(existingEvent.getLocation()) // Keep existing
                // Update only payment fields
                .ticketPrice(new BigDecimal("100.00"))
                .currency("USD")
                .paymentRequired(false)
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Event updatedEvent = eventService.updateEvent(eventId, paymentOnlyRequest);

        // Assert
        assertNotNull(updatedEvent);
        
        // Verify non-payment fields remain unchanged
        assertEquals(existingEvent.getName(), updatedEvent.getName());
        assertEquals(existingEvent.getDescription(), updatedEvent.getDescription());
        assertEquals(existingEvent.getStatus(), updatedEvent.getStatus());
        
        // Verify payment fields are updated
        assertEquals(new BigDecimal("100.00"), updatedEvent.getTicketPrice());
        assertEquals("USD", updatedEvent.getCurrency());
        assertEquals(false, updatedEvent.getPaymentRequired());

        verify(eventRepository).save(updatedEvent);
    }
}
