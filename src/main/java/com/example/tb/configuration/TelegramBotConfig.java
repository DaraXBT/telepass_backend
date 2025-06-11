package com.example.tb.configuration;

import com.example.tb.authentication.service.telegram.TelegramServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class TelegramBotConfig {

    private final TelegramServiceImpl telegramService;

    public TelegramBotConfig(TelegramServiceImpl telegramService) {
        this.telegramService = telegramService;
    }
    @Bean
    public String registerTelegramBot() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(telegramService);
            return "Telegram bot successfully registered.";
        } catch (TelegramApiException e) {
            throw new IllegalStateException("Failed to register Telegram bot.", e);
        }
    }
}
