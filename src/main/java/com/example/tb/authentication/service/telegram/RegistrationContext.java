package com.example.tb.authentication.service.telegram;

import java.util.UUID;

import com.example.tb.model.entity.User;

public class RegistrationContext {
    private UUID eventId;
    private RegistrationStep currentStep;
    private User user;
    // Payment related fields
    private boolean paymentRequired = false;
    private String merchantTransactionId;
    private boolean paymentCompleted = false;

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

    // Payment related getters and setters
    public boolean isPaymentRequired() {
        return paymentRequired;
    }

    public void setPaymentRequired(boolean paymentRequired) {
        this.paymentRequired = paymentRequired;
    }

    public String getMerchantTransactionId() {
        return merchantTransactionId;
    }

    public void setMerchantTransactionId(String merchantTransactionId) {
        this.merchantTransactionId = merchantTransactionId;
    }

    public boolean isPaymentCompleted() {
        return paymentCompleted;
    }

    public void setPaymentCompleted(boolean paymentCompleted) {
        this.paymentCompleted = paymentCompleted;
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
        PAYMENT, // New step for payment
        PAYMENT_PENDING, // Waiting for payment confirmation
        COMPLETED
    }
}