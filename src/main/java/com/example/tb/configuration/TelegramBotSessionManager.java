package com.example.tb.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.LongPollingBot;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class TelegramBotSessionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(TelegramBotSessionManager.class);
    private static final AtomicBoolean sessionActive = new AtomicBoolean(false);
    private static TelegramBotsApi botsApi;
    private static LongPollingBot registeredBot;
      public synchronized boolean registerBot(LongPollingBot bot) throws TelegramApiException {
        if (sessionActive.get()) {
            logger.debug("Bot session already active - skipping registration");
            return false;
        }
        
        try {
            // Check for existing running instances by attempting to stop any previous sessions
            if (registeredBot != null) {
                logger.debug("Cleaning up previous bot session");
                try {
                    registeredBot.onClosing();
                } catch (Exception e) {
                    logger.debug("Error during previous session cleanup: {}", e.getMessage());
                }
            }
            
            if (botsApi == null) {
                botsApi = new TelegramBotsApi(org.telegram.telegrambots.updatesreceivers.DefaultBotSession.class);
            }
            
            botsApi.registerBot(bot);
            registeredBot = bot;
            sessionActive.set(true);
            logger.info("Telegram bot registered successfully");
            return true;
            
        } catch (TelegramApiException e) {
            if (e.getMessage() != null && (e.getMessage().contains("409") || e.getMessage().contains("Conflict"))) {
                logger.debug("Bot conflict detected - another instance is likely running. Error: {}", e.getMessage());
                sessionActive.set(true); // Mark as active to prevent retry attempts
                return false;
            } else {
                logger.error("Failed to register Telegram bot: {}", e.getMessage());
                throw e;
            }
        }
    }
    
    @PreDestroy
    public void cleanup() {
        if (sessionActive.get() && registeredBot != null) {
            logger.info("Cleaning up Telegram bot session");
            try {
                registeredBot.onClosing();
                sessionActive.set(false);
            } catch (Exception e) {
                logger.debug("Error during bot session cleanup: {}", e.getMessage());
            }
        }
    }
    
    public boolean isSessionActive() {
        return sessionActive.get();
    }
}
