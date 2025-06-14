package com.example.tb.authentication.service.telegram;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.example.tb.authentication.service.UserRegistrationService;
import com.example.tb.authentication.service.event.EventService;
import com.example.tb.model.dto.VerificationResponseDTO;
import com.example.tb.model.entity.Event;
import com.example.tb.model.entity.EventRole;
import com.example.tb.model.entity.User;
import com.example.tb.model.response.EventResponse;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

@Service
public class TelegramServiceImpl extends TelegramLongPollingBot {
    private static final Logger logger = LoggerFactory.getLogger(TelegramServiceImpl.class);
    public static final String botToken = "7604740715:AAGnrNxu0hnnJ8JdtEBin1R3S_yE6GiHGHI";
    public static final String botUsername = "telepasskhbot";
    private static final long ADMIN_CHAT_ID = 1238939350;
    private boolean awaitingQrUpload = false;

    public TelegramServiceImpl() {
        super(botToken);
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }    @Autowired
    private UserRegistrationService userRegistrationService;
    @Autowired
    private EventService eventService;
    @Autowired
    private com.example.tb.authentication.repository.admin.AdminRepository adminRepository;
    private Map<Long, RegistrationContext> registrationContexts = new ConcurrentHashMap<>();    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) {
                long chatId = update.getMessage().getChatId();

                // --- ADMIN SCAN FEATURE ---
                if (chatId == ADMIN_CHAT_ID) {
                    // Handle QR image upload (must check for photo first!)
                    if (awaitingQrUpload && update.getMessage().hasPhoto()) {
                        awaitingQrUpload = false;
                        List<PhotoSize> photos = update.getMessage().getPhoto();
                        String fileId = photos.get(photos.size() - 1).getFileId();
                        handleQrImage(chatId, fileId);
                        return;
                    }
                    // Handle /scan command
                    if (update.getMessage().hasText() && update.getMessage().getText().equals("/scan")) {
                        awaitingQrUpload = true;
                        sendMessage(chatId, "🖼️ Please upload the user's registration QR code image.");
                        return;
                    }
                } else if (update.getMessage().hasText() && update.getMessage().getText().equals("/scan")) {
                    sendMessage(chatId, "❌ You do not have permission to use this command.");
                    return;
                }

                // --- EXISTING REGISTRATION LOGIC ---
                if (update.getMessage().hasText()) {
                    String messageText = update.getMessage().getText();

                    // Only handle QR code scan or continue registration process
                    if (messageText.startsWith("/start")) {
                        String[] parts = messageText.split(" ");
                        if (parts.length > 1 && parts[1].startsWith("event_")) {
                            String eventId = parts[1].substring(6); // Remove "event_" prefix
                            startEventRegistration(chatId, UUID.fromString(eventId));
                        } else {
                            sendMessage(chatId, "⚠️ សូមស្កេន QR Code ដើម្បីចុះឈ្មោះចូលរួមព្រឹត្តិការណ៍។");
                        }
                    } else {
                        // Process registration steps only if user is in registration process
                        RegistrationContext context = registrationContexts.get(chatId);
                        if (context != null) {
                            processRegistrationStep(chatId, messageText);
                        } else {
                            sendMessage(chatId, "⚠️ សូមស្កេន QR Code ដើម្បីចុះឈ្មោះចូលរួមព្រឹត្តិការណ៍។");
                        }
                    }
                }
            }        } catch (Exception e) {
            logger.error("Error in registration process: {}", e.getMessage(), e);
            // Check if it's a bot conflict error
            if (e.getMessage() != null && e.getMessage().contains("409")) {
                logger.warn("Telegram bot conflict detected - another instance may be running");
            }
        }
    }

    private void sendWelcomeMessage(long chatId) {
        String welcomeMessage = """
                👋 សូមស្វាគមន៍មកកាន់ Telepass Bot!

                📋 ពាក្យបញ្ជាដែលមាន:
                /start - ចាប់ផ្តើមឬមើលពាក្យបញ្ជាទាំងអស់
                /register - ចុះឈ្មោះថ្មី
                /reset - ចាប់ផ្តើមការចុះឈ្មោះម្តងទៀត
                /help - មើលជំនួយបន្ថែម

                សូមជ្រើសរើសពាក្យបញ្ជាដែលអ្នកចង់ប្រើប្រាស់។""";

        sendMessage(chatId, welcomeMessage);
    }

    private void sendHelpMessage(long chatId) {
        String helpMessage = """
                ℹ️ ជំនួយបន្ថែម:

                1️⃣ ការចុះឈ្មោះ (/register):
                   - បញ្ចូលឈ្មោះពេញ
                   - បញ្ចូលលេខទូរស័ព្ទ
                   - ជ្រើសរើសភេទ
                   - បញ្ចូលថ្ងៃខែឆ្នាំកំណើត
                   - បញ្ចូលអាសយដ្ឋាន
                   - បញ្ចូលអ៊ីមែល
                   - បញ្ចូលមុខរបរ

                2️⃣ ការចាប់ផ្តើមម្តងទៀត (/reset):
                   - ប្រើបើអ្នកចង់ចាប់ផ្តើមការចុះឈ្មោះម្តងទៀត

                ប្រសិនបើមានបញ្ហា សូមទាក់ទងអ្នកគ្រប់គ្រង។""";

        sendMessage(chatId, helpMessage);
    }

    private void resetRegistration(long chatId) {
        registrationContexts.remove(chatId);
        sendMessage(chatId, "🔄 ការចុះឈ្មោះត្រូវបានកំណត់ឡើងវិញ។ អ្នកអាចចាប់ផ្តើមម្តងទៀតដោយប្រើ /register");
    }

    private void startRegistration(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("""
                \uD83E\uDD16 សូមស្វាគមន៍មកកាន់ការចុះឈ្មោះ!\s
                \uD83D\uDE4F សូមបំពេញព័ត៌មានខាងក្រោម៖
                ✍️ សូមបញ្ចូលឈ្មោះពេញរបស់អ្នក៖""");

        // Initialize registration context
        registrationContexts.put(chatId, new RegistrationContext());

        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.info("Error starting registration", e);
        }
    }

    private void processRegistrationStep(long chatId, String messageText) {
        RegistrationContext context = registrationContexts.get(chatId);
        if (context == null) {
            sendMessage(chatId, "⚠️ សូមស្កេន QR Code ដើម្បីចុះឈ្មោះចូលរួមព្រឹត្តិការណ៍។");
            return;
        }

        try {
            switch (context.getCurrentStep()) {
                case START:
                case FULL_NAME:
                    if (isValidName(messageText)) {
                        context.getUser().setFullName(messageText);
                        context.setCurrentStep(RegistrationContext.RegistrationStep.PHONE_NUMBER);
                        sendMessage(chatId, "☎️ សូមបញ្ចូលលេខទូរស័ព្ទរបស់អ្នក៖ \n" +
                                "     (ឧទាហរណ៍៖ +855XXXXXXXXX)");
                    } else {
                        sendMessage(chatId, "⚠️ ឈ្មោះមិនត្រឹមត្រូវ។ សូមបញ្ចូលឈ្មោះពេញរបស់អ្នកម្តងទៀត។\n" +
                                "ឬស្កេន QR Code ម្តងទៀតដើម្បីចាប់ផ្តើមការចុះឈ្មោះ។");
                    }
                    break;

                case PHONE_NUMBER:
                    if (isValidPhoneNumber(messageText)) {
                        context.getUser().setPhoneNumber(messageText);
                        context.setCurrentStep(RegistrationContext.RegistrationStep.GENDER);
                        sendGenderKeyboard(chatId);
                    } else {
                        sendMessage(chatId, "⚠️ លេខទូរស័ព្ទមិនត្រឹមត្រូវ។ សូមបញ្ចូលលេខទូរស័ព្ទម្តងទៀត។\n" +
                                "ឬស្កេន QR Code ម្តងទៀតដើម្បីចាប់ផ្តើមការចុះឈ្មោះ។");
                    }
                    break;

                case GENDER:
                    if (isValidGender(messageText)) {
                        context.getUser().setGender(User.Gender.valueOf(messageText.toUpperCase()));
                        context.setCurrentStep(RegistrationContext.RegistrationStep.DATE_OF_BIRTH);
                        sendMessage(chatId, "📅 សូមបញ្ចូលថ្ងៃខែឆ្នាំកំណើតរបស់អ្នក៖\n" +
                                "      (ទម្រង់៖ YYYY-MM-DD)");
                    } else {
                        sendMessage(chatId, "⚠️ ភេទមិនត្រឹមត្រូវ។ សូមជ្រើសរើសភេទម្តងទៀត។\n" +
                                "ឬស្កេន QR Code ម្តងទៀតដើម្បីចាប់ផ្តើមការចុះឈ្មោះ។");
                        sendGenderKeyboard(chatId);
                    }
                    break;

                case DATE_OF_BIRTH:
                    if (isValidDateOfBirth(messageText)) {
                        context.getUser().setDateOfBirth(LocalDate.parse(messageText));
                        context.setCurrentStep(RegistrationContext.RegistrationStep.ADDRESS);
                        sendMessage(chatId, "\uD83D\uDCCD សូមបញ្ចូលអាសយដ្ឋានពេញរបស់អ្នក៖");
                    } else {
                        sendMessage(chatId,
                                "⚠️ ទម្រង់កាលបរិច្ឆេទមិនត្រឹមត្រូវ។ សូមបញ្ចូលកាលបរិច្ឆេទម្តងទៀត (YYYY-MM-DD)។\n" +
                                        "ឬស្កេន QR Code ម្តងទៀតដើម្បីចាប់ផ្តើមការចុះឈ្មោះ។");
                    }
                    break;

                case ADDRESS:
                    if (isValidAddress(messageText)) {
                        context.getUser().setAddress(messageText);
                        context.setCurrentStep(RegistrationContext.RegistrationStep.EMAIL);
                        sendMessage(chatId, "\uD83D\uDCE7 សូមបញ្ចូលអ៊ីមែលរបស់អ្នក៖");
                    } else {
                        sendMessage(chatId, "⚠️ អាសយដ្ឋានមិនត្រឹមត្រូវ។ សូមបញ្ចូលអាសយដ្ឋានម្តងទៀត។\n" +
                                "ឬស្កេន QR Code ម្តងទៀតដើម្បីចាប់ផ្តើមការចុះឈ្មោះ។");
                    }
                    break;

                case EMAIL:
                    if (isValidEmail(messageText)) {
                        context.getUser().setEmail(messageText);
                        context.setCurrentStep(RegistrationContext.RegistrationStep.OCCUPATION);
                        sendMessage(chatId, "\uD83D\uDCBC សូមបញ្ចូលមុខរបររបស់អ្នក៖");
                    } else {
                        sendMessage(chatId, "⚠️ អ៊ីមែលមិនត្រឹមត្រូវ។ សូមបញ្ចូលអ៊ីមែលម្តងទៀត។\n" +
                                "ឬស្កេន QR Code ម្តងទៀតដើម្បីចាប់ផ្តើមការចុះឈ្មោះ។");
                    }
                    break;                case OCCUPATION:
                    if (isValidOccupation(messageText)) {
                        context.getUser().setOccupation(messageText);

                        // Complete registration
                        User registeredUser = userRegistrationService.registerUser(context.getUser());

                        // Register user for the event
                        if (context.getEventId() != null) {
                            eventService.registerUserForEvent(context.getEventId(), registeredUser);
                        }

                        // Generate QR Code and save to filesystem
                        String qrCodeFilePath = userRegistrationService.generateAndSaveQRCode(
                                context.getEventId().toString(),
                                registeredUser.getId().toString(),
                                registeredUser.getRegistrationToken());                        // Update user with QR code file path
                        registeredUser.setQrCode(qrCodeFilePath);
                        userRegistrationService.updateUser(registeredUser);

                        // Generate Base64 QR Code for Telegram
                        String qrCodeBase64 = userRegistrationService.generateQRCode(
                                context.getEventId().toString(),
                                registeredUser.getId().toString(),
                                registeredUser.getRegistrationToken());

                        // Send QR Code and completion message
                        sendQRCodeAndCompleteRegistration(chatId, qrCodeBase64);

                        // Remove context
                        registrationContexts.remove(chatId);
                    } else {
                        sendMessage(chatId, "⚠️ មុខរបរមិនត្រឹមត្រូវ។ សូមបញ្ចូលមុខរបរម្តងទៀត។\n" +
                                "ឬស្កេន QR Code ម្តងទៀតដើម្បីចាប់ផ្តើមការចុះឈ្មោះ។");
                    }
                    break;

                case COMPLETED:
                    sendMessage(chatId, "✅ ការចុះឈ្មោះបានបញ្ចប់។");
                    break;
            }
        } catch (Exception e) {
            logger.error("Error in registration step", e);
            sendMessage(chatId, "⚠️ មានកំហុសក្នុងការចុះឈ្មោះ។ សូមព្យាយាមម្តងទៀត។\n" +
                    "ឬស្កេន QR Code ម្តងទៀតដើម្បីចាប់ផ្តើមការចុះឈ្មោះ។");
        }
    }

    private void sendQRCodeAndCompleteRegistration(long chatId, String qrCodeBase64) throws TelegramApiException {
        // Decode Base64 QR Code
        byte[] qrCodeBytes = Base64.getDecoder().decode(qrCodeBase64);

        // Send QR Code as photo
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId);
        sendPhoto.setPhoto(new InputFile(new ByteArrayInputStream(qrCodeBytes), "registration_qr.png"));
        sendPhoto.setCaption("✅ ការចុះឈ្មោះបានជោគជ័យ!\n" +
                "\uD83D\uDD14 សូមរក្សាទុក QR Code នេះសម្រាប់ការចូលរួមព្រឹត្តិការណ៍។");

        // Send the QR code
        execute(sendPhoto);
    }

    private void sendGenderKeyboard(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("⚧️ សូមជ្រើសរើសភេទរបស់អ្នក៖");

        // Create custom keyboard
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();

        row.add(new KeyboardButton("Male"));
        row.add(new KeyboardButton("Female"));
        row.add(new KeyboardButton("Other"));

        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setOneTimeKeyboard(true);

        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending gender keyboard", e);
        }
    }

    private void startEventRegistration(long chatId, UUID eventId) {
        try {
            // Check if event exists
            Optional<EventResponse> eventOpt = eventService.getEventById(eventId);
            if (eventOpt.isEmpty()) {
                sendMessage(chatId, "❌ ព្រឹត្តិការណ៍មិនត្រូវបានរកឃើញ។");
                return;
            }

            EventResponse event = eventOpt.get();

            // Initialize registration context with event ID
            RegistrationContext context = new RegistrationContext();
            context.setEventId(eventId);
            registrationContexts.put(chatId, context);

            // Send event image if available
            if (event.getEventImg() != null && !event.getEventImg().isEmpty()) {
                try {
                    // Extract filename from URL
                    String imagePath = event.getEventImg();
                    String fileName = imagePath.substring(imagePath.lastIndexOf("=") + 1);                    // Use ClassPathResource to access the file
                    ClassPathResource resource = new ClassPathResource("files/" + fileName);
                    logger.debug("Loading resource: {}", resource);
                    if (resource.exists()) {
                        SendPhoto sendPhoto = new SendPhoto();
                        sendPhoto.setChatId(chatId);
                        sendPhoto.setPhoto(new InputFile(resource.getInputStream(), fileName));
                        execute(sendPhoto);
                    } else {
                        logger.warn("Image file not found: {}", fileName);
                    }
                } catch (Exception e) {
                    logger.error("Error sending event image", e);
                }
            }            // Send welcome message for event registration
            // Fetch event roles directly from EventService since EventResponse has empty eventRoles list
            java.util.List<com.example.tb.model.entity.EventRole> eventRoles = 
                eventService.getEventRoles(eventId);
            
            logger.info("Fetched {} event roles for event ID: {}", 
                eventRoles != null ? eventRoles.size() : 0, eventId);
            
            // Format datetime information
            String dateTimeInfo = formatEventDateTime(event);
            
            // Format location information
            String locationInfo = formatEventLocation(event);
            
            String welcomeMessage = String.format("""
                    🎉 សូមស្វាគមន៍មកកាន់ការចុះឈ្មោះចូលរួមព្រឹត្តិការណ៍!

                    📋 ព្រឹត្តិការណ៍: %s
                    📝 ការពិពណ៌នា: %s
                    👥 អ្នករៀបចំ: %s%s%s

                    សូមបំពេញព័ត៌មានខាងក្រោមដើម្បីចុះឈ្មោះ៖
                    ✍️ សូមបញ្ចូលឈ្មោះពេញរបស់អ្នក៖""",
                    event.getName(),
                    event.getDescription(),
                    formatOrganizers(eventRoles),
                    dateTimeInfo,
                    locationInfo);

            sendMessage(chatId, welcomeMessage);
        } catch (Exception e) {
            logger.error("Error starting event registration", e);
            sendMessage(chatId, "❌ មានកំហុសក្នុងការចុះឈ្មោះ។ សូមព្យាយាមម្តងទៀត។");
        }
    }    private String formatOrganizers(List<EventRole> roles) {
        if (roles == null || roles.isEmpty()) {
            return "មិនមានអ្នករៀបចំ";
        }

        StringBuilder organizers = new StringBuilder();
        for (EventRole role : roles) {
            if (role.getRole() == EventRole.EventRoleType.OWNER ||
                    role.getRole() == EventRole.EventRoleType.ORGANIZER) {
                if (organizers.length() > 0) {
                    organizers.append(", ");
                }
                
                // Get user ID from event role and fetch full name from Admin table
                String displayName = "Unknown"; // default fallback
                java.util.UUID userId = role.getUser().getId();
                
                logger.info("Fetching organizer info for user ID: {}", userId);
                
                try {
                    // Get fallback username first
                    if (role.getUser().getUsername() != null) {
                        displayName = role.getUser().getUsername();
                    }
                    
                    // Try to get full name from Admin table using user_id
                    java.util.Optional<com.example.tb.model.entity.Admin> adminOptional = 
                        adminRepository.findById(userId);
                    
                    if (adminOptional.isPresent()) {
                        com.example.tb.model.entity.Admin admin = adminOptional.get();
                        logger.info("Found admin record - Username: {}, FullName: {}", 
                            admin.getUsername(), admin.getFullName());
                        
                        if (admin.getFullName() != null && !admin.getFullName().trim().isEmpty()) {
                            displayName = admin.getFullName();
                            logger.info("Using full name: {}", displayName);
                        } else {
                            logger.warn("Admin full name is null or empty for user ID: {}", userId);
                        }
                    } else {
                        logger.warn("No admin record found for user ID: {}", userId);
                    }
                } catch (Exception e) {
                    // Log error but continue with username fallback
                    logger.error("Failed to fetch admin full name for user ID {}: {}", userId, e.getMessage(), e);
                }
                
                organizers.append(displayName);
                logger.info("Added organizer to list: {}", displayName);
            }
        }
        
        String result = organizers.length() > 0 ? organizers.toString() : "មិនមានអ្នករៀបចំ";
        logger.info("Final organizers string: {}", result);
        return result;
    }

    // Debug method to test organizer formatting - REMOVE IN PRODUCTION
    public String debugFormatOrganizers(java.util.UUID eventId) {
        try {
            java.util.Optional<com.example.tb.model.response.EventResponse> eventOptional = eventService.getEventById(eventId);
            if (eventOptional.isPresent()) {
                com.example.tb.model.response.EventResponse event = eventOptional.get();
                logger.info("DEBUG: Event found - Name: {}, Description: {}", event.getName(), event.getDescription());
                
                // Get event roles for this event
                java.util.List<com.example.tb.model.entity.EventRole> roles = eventService.getEventRoles(eventId);
                logger.info("DEBUG: Found {} event roles", roles.size());
                
                for (com.example.tb.model.entity.EventRole role : roles) {
                    logger.info("DEBUG: Role - ID: {}, Type: {}, User ID: {}, Username: {}", 
                        role.getId(), role.getRole(), role.getUser().getId(), role.getUser().getUsername());
                }
                
                return formatOrganizers(roles);
            } else {
                return "Event not found";
            }
        } catch (Exception e) {
            logger.error("DEBUG: Error in debugFormatOrganizers: {}", e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }

    // Validation methods (simplified, you should enhance these)
    private boolean isValidName(String name) {
        return name != null && name.trim().length() >= 2 && name.trim().length() <= 100;
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && phoneNumber.matches("^\\+?[1-9]\\d{1,14}$");
    }

    private boolean isValidGender(String gender) {
        return gender != null &&
                (gender.equalsIgnoreCase("male") ||
                        gender.equalsIgnoreCase("female") ||
                        gender.equalsIgnoreCase("other"));
    }

    private boolean isValidDateOfBirth(String dob) {
        try {
            LocalDate parsedDate = LocalDate.parse(dob);
            return parsedDate.isBefore(LocalDate.now().minusYears(13)); // At least 13 years old
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isValidAddress(String address) {
        return address != null && address.trim().length() >= 5 && address.trim().length() <= 255;
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private boolean isValidOccupation(String occupation) {
        return occupation != null && occupation.trim().length() >= 2 && occupation.trim().length() <= 100;
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending message", e);
        }
    }

    private void handleQrImage(long chatId, String fileId) {
        try {
            // Download the file from Telegram
            GetFile getFile = new GetFile();
            getFile.setFileId(fileId);
            org.telegram.telegrambots.meta.api.objects.File file = execute(getFile);
            String filePath = file.getFilePath();
            java.io.InputStream is = new java.net.URL(
                    "https://api.telegram.org/file/bot" + getBotToken() + "/" + filePath).openStream();

            // Decode QR code (using ZXing)
            BufferedImage bufferedImage = ImageIO.read(is);
            LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = new MultiFormatReader().decode(bitmap);
            String qrContent = result.getText();

            // Parse QR content: eventId|userId|registrationToken
            String[] parts = qrContent.split("\\|");
            if (parts.length != 3) {
                sendMessage(chatId, "❌ Invalid QR code format.");
                return;
            }
            String eventId = parts[0];
            String userId = parts[1];
            String registrationToken = parts[2];

            // Call backend verification endpoint (example using RestTemplate)
            String url = String.format("http://localhost:8080/api/v1/audiences/verify?eventId=%s&userId=%s", eventId,
                    userId);
            RestTemplate restTemplate = new RestTemplate();
            VerificationResponseDTO response = restTemplate.postForObject(url, null, VerificationResponseDTO.class);

            if (response != null && response.isVerified() && response.getUser() != null
                    && registrationToken.equals(response.getUser().getRegistrationToken())) {
                sendMessage(chatId, "✅ Verified!\n" + formatUserInfo(response.getUser()));
            } else if (response != null && response.getMessage() != null
                    && response.getMessage().contains("already checked in")) {
                sendMessage(chatId,
                        "⚠️ This QR code has already been used for check-in.\n" + formatUserInfo(response.getUser()));
            } else {
                sendMessage(chatId, "❌ Not valid or registration token mismatch.");
            }
        } catch (Exception e) {
            sendMessage(chatId, "❌ Failed to process QR code: " + e.getMessage());
            logger.error("QR scan error", e);
        }
    }

    private String formatUserInfo(com.example.tb.model.dto.UserDTO user) {
        if (user == null)
            return "User info not available.";
        return String.format(
                "Full Name: %s\n" +
                        "Phone: %s\n" +
                        "Gender: %s\n" +
                        "Date of Birth: %s\n" +
                        "Address: %s\n" +
                        "Email: %s\n" +
                        "Occupation: %s",
                user.getFullName(),
                user.getPhoneNumber(),
                user.getGender(),
                user.getDateOfBirth(),
                user.getAddress(),
                user.getEmail(),
                user.getOccupation());
    }    /**
     * Formats event datetime information for display in welcome message
     */
    private String formatEventDateTime(EventResponse event) {
        if (event.getStartDateTime() == null && event.getEndDateTime() == null) {
            return "";
        }

        StringBuilder dateTimeInfo = new StringBuilder("\n");
        
        if (event.getStartDateTime() != null) {
            dateTimeInfo.append("📅 ចាប់ផ្តើម: ")
                       .append(event.getStartDateTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        }
        
        if (event.getEndDateTime() != null) {
            if (event.getStartDateTime() != null) {
                dateTimeInfo.append("\n");
            }
            dateTimeInfo.append("🏁 បញ្ចប់: ")
                       .append(event.getEndDateTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        }
        
        return dateTimeInfo.toString();
    }

    /**
     * Formats event location information for display in welcome message
     */
    private String formatEventLocation(EventResponse event) {
        if (event.getLocation() == null || event.getLocation().trim().isEmpty()) {
            return "";
        }
        
        return "\n📍 ទីតាំង: " + event.getLocation();
    }

    /**
     * Sends location as a map if the location string appears to be a valid URL
     */
    private void sendLocationIfPossible(long chatId, String location) {
        try {
            // Check if location is a valid URL (Google Maps, other map services)
            if (isValidMapUrl(location)) {
                // For Google Maps links, we can extract coordinates and send as location
                if (location.contains("maps.google.com") || location.contains("goo.gl/maps")) {
                    sendMapLink(chatId, location);
                } else {
                    // For other URLs, just send as a clickable link
                    sendMessage(chatId, "🗺️ ទីតាំងលើផែនទី: " + location);
                }
            } else {
                // Check if it looks like coordinates (latitude,longitude)
                if (isCoordinateFormat(location)) {
                    sendCoordinatesAsLocation(chatId, location);
                } else {
                    // Send as a regular location text with map search option
                    sendMessage(chatId, "🗺️ ស្វែងរកទីតាំង: https://www.google.com/maps/search/" + 
                               java.net.URLEncoder.encode(location, "UTF-8"));
                }
            }
        } catch (Exception e) {
            logger.error("Error sending location information", e);
            // Fallback: just send the location as text
            sendMessage(chatId, "📍 ទីតាំង: " + location);
        }
    }

    /**
     * Checks if a string is a valid map URL
     */
    private boolean isValidMapUrl(String location) {
        if (location == null || location.trim().isEmpty()) {
            return false;
        }
        
        location = location.toLowerCase();
        return location.startsWith("http://") || location.startsWith("https://") &&
               (location.contains("maps.google.com") || 
                location.contains("goo.gl/maps") ||
                location.contains("google.com/maps") ||
                location.contains("maps.app.goo.gl") ||
                location.contains("openstreetmap.org") ||
                location.contains("waze.com"));
    }

    /**
     * Checks if a string is in coordinate format (latitude,longitude)
     */
    private boolean isCoordinateFormat(String location) {
        if (location == null || location.trim().isEmpty()) {
            return false;
        }
        
        // Check for patterns like "11.5564,104.9282" or "11.5564, 104.9282"
        String pattern = "^-?\\d+\\.\\d+\\s*,\\s*-?\\d+\\.\\d+$";
        return location.trim().matches(pattern);
    }    /**
     * Sends a map link message
     */
    private void sendMapLink(long chatId, String mapUrl) {
        String message = "🗺️ បើកទីតាំងលើផែនទី:\n" + mapUrl;
        sendMessage(chatId, message);
    }

    /**
     * Sends coordinates as a location that can be opened in map apps
     */
    private void sendCoordinatesAsLocation(long chatId, String coordinates) {
        try {
            String[] parts = coordinates.split(",");
            if (parts.length == 2) {
                double lat = Double.parseDouble(parts[0].trim());
                double lng = Double.parseDouble(parts[1].trim());
                
                // Create Google Maps link
                String mapUrl = String.format("https://www.google.com/maps?q=%f,%f", lat, lng);
                sendMessage(chatId, "🗺️ ទីតាំងលើផែនទី:\n" + mapUrl);
                
                // Note: Telegram Bot API also supports sending actual location with sendLocation method
                // but it requires additional setup with coordinates
            }
        } catch (NumberFormatException e) {
            logger.error("Invalid coordinate format: " + coordinates, e);
            sendMessage(chatId, "📍 ទីតាំង: " + coordinates);
        }
    }

}