package com.example.tb.configuration;

import com.example.tb.authentication.service.telegram.TelegramServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Configuration
public class TelegramBotConfig {

    private final TelegramServiceImpl telegramService;
    private final TelegramBotSessionManager sessionManager;

    public TelegramBotConfig(TelegramServiceImpl telegramService, TelegramBotSessionManager sessionManager) {
        this.telegramService = telegramService;
        this.sessionManager = sessionManager;
    }
    
    @Bean
    public String registerTelegramBot() {
        try {
            boolean registered = sessionManager.registerBot(telegramService);
            if (registered) {
                return "Telegram bot successfully registered.";
            } else {
                return "Telegram bot registration skipped - session already active or conflict detected.";
            }
        } catch (TelegramApiException e) {
            throw new IllegalStateException("Failed to register Telegram bot: " + e.getMessage(), e);
        }
    }
}
