package com.example.tb.authentication.service.telegram;

import java.util.UUID;

import com.example.tb.model.entity.User;

public class RegistrationContext {
    private UUID eventId;
    private RegistrationStep currentStep;
    private User user;

    public RegistrationContext() {
        this.currentStep = RegistrationStep.START;
        this.user = new User();
    }

    public RegistrationContext(UUID eventId) {
        this.eventId = eventId;
        this.currentStep = RegistrationStep.START;
        this.user = new User();
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    // Getter for user
    public User getUser() {
        return user;
    }

    // Setter for user
    public void setUser(User user) {
        this.user = user;
    }

    // Getter for currentStep
    public RegistrationStep getCurrentStep() {
        return currentStep;
    }

    // Setter for currentStep
    public void setCurrentStep(RegistrationStep currentStep) {
        this.currentStep = currentStep;
    }

    // Enum for RegistrationStep (moved inside or alongside the class)
    public enum RegistrationStep {
        START,
        FULL_NAME,
        PHONE_NUMBER,
        GENDER,
        DATE_OF_BIRTH,
        ADDRESS,
        EMAIL,
        OCCUPATION,
        COMPLETED
    }
}