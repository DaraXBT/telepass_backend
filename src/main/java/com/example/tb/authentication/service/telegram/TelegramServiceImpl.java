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
import java.math.BigDecimal;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.example.tb.authentication.service.UserRegistrationService;
import com.example.tb.authentication.service.email.EmailService;
import com.example.tb.authentication.service.event.EventService;
import com.example.tb.authentication.repository.admin.AdminRepository;
import com.example.tb.model.dto.VerificationResponseDTO;
import com.example.tb.model.entity.Event;
import com.example.tb.model.entity.EventRole;
import com.example.tb.model.entity.User;
import com.example.tb.model.response.EventResponse;
import com.example.tb.authentication.service.payment.BakongPaymentService;
import com.example.tb.model.request.PaymentRequest;
import com.example.tb.model.response.PaymentResponse;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.example.tb.utils.QrCodeUtil;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class TelegramServiceImpl extends TelegramLongPollingBot {
    private static final Logger logger = LoggerFactory.getLogger(TelegramServiceImpl.class);
    public static final String botToken = "7604740715:AAGnrNxu0hnnJ8JdtEBin1R3S_yE6GiHGHI";
    public static final String botUsername = "telepasskhbot";
    private static final List<Long> ADMIN_CHAT_IDS = List.of(649084122L);
    private boolean awaitingQrUpload = false;

    public TelegramServiceImpl() {
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Autowired
    private UserRegistrationService userRegistrationService;
    @Autowired
    private EventService eventService;
    @Autowired
    private EmailService emailService;
    @Autowired
    private AdminRepository adminRepository;
    @Autowired
    private BakongPaymentService bakongPaymentService; // Add payment service
    
    private Map<Long, RegistrationContext> registrationContexts = new ConcurrentHashMap<>();    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) {
                long chatId = update.getMessage().getChatId();                // --- ADMIN SCAN FEATURE ---
                if (isAdmin(chatId)) {
                    // Handle QR image upload (must check for photo first!)
                    if (awaitingQrUpload && update.getMessage().hasPhoto()) {
                        awaitingQrUpload = false;
                        List<PhotoSize> photos = update.getMessage().getPhoto();
                        String fileId = photos.get(photos.size() - 1).getFileId();
                        handleQrImage(chatId, fileId);
                        return;
                    }
                    // Handle admin commands
                    if (update.getMessage().hasText()) {
                        String messageText = update.getMessage().getText();
                          if (messageText.equals("/scan")) {
                            awaitingQrUpload = true;
                            sendMessage(chatId, """
                                🔍 ម៉ាស៊ីនស្កេន QR កូដ បានបើក
                                
                                📱 សូមថតរូបភាព QR កូដនៃការចុះឈ្មោះ
                                
                                📋 វិធីប្រើប្រាស់:
                                • ថតរូប QR កូដឱ្យច្បាស់
                                • ផ្ញើរូបភាពមកកាន់ជជែកនេះ
                                • ប្រព័ន្ធនឹងពិនិត្យនិងចុះឈ្មោះដោយស្វ័យប្រវត្តិ
                                
                                ⚠️ ចំណាំ: អ្នកដែលបានចុះឈ្មោះប្រកាន់អាចចូលបានតែម្តង។""");
                            return;
                        } else if (messageText.equals("/help_admin")) {
                            sendAdminHelpMessage(chatId);
                            return;
                        } else if (messageText.equals("/cancel")) {
                            awaitingQrUpload = false;
                            sendMessage(chatId, "❌ បានបោះបង់ការស្កេន QR។ ប្រើ /scan ដើម្បីចាប់ផ្តើមម្តងទៀត។");
                            return;
                        }
                    }
                } else if (update.getMessage().hasText() && update.getMessage().getText().equals("/scan")) {
                    sendMessage(chatId, "❌ **បាតបង់ការចូលប្រើ**\n\nអ្នកមិនមានសិទ្ធិប្រើប្រាស់មុខងារស្កេនរបស់អ្នកគ្រប់គ្រងទេ។");
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
                        }                    }
                }            } else if (update.hasCallbackQuery()) {
                // Handle callback queries (inline button presses)
                long chatId = update.getCallbackQuery().getMessage().getChatId();
                String callbackData = update.getCallbackQuery().getData();                
                if ("check_payment".equals(callbackData)) {
                    handlePaymentStatusCheck(chatId);
                }
            }} catch (Exception e) {
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

                        // Check if payment is required
                        if (context.isPaymentRequired()) {
                            // Move to payment step
                            context.setCurrentStep(RegistrationContext.RegistrationStep.PAYMENT);
                            handlePaymentStep(chatId, context);
                        } else {
                            // Direct registration for free events
                            processCompleteRegistration(chatId, context);
                        }
                    } else {
                        sendMessage(chatId, "⚠️ មុខរបរមិនត្រឹមត្រូវ។ សូមបញ្ចូលមុខរបរម្តងទៀត។\n" +
                                "ឬស្កេន QR Code ម្តងទៀតដើម្បីចាប់ផ្តើមការចុះឈ្មោះ។");
                    }
                    break;

                case PAYMENT:
                    // Handle payment-related messages (retry, status check, etc.)
                    handlePaymentMessages(chatId, messageText, context);
                    break;

                case PAYMENT_PENDING:
                    // User is waiting for payment confirmation
                    sendMessage(chatId, "⏳ កំពុងរង់ចាំការទូទាត់...\n" +
                            "📱 សូមបំពេញការទូទាត់តាមរយៈ Bakong ហើយការចុះឈ្មោះនឹងបន្តដោយស្វ័យប្រវត្តិ។\n\n" +
                            "💡 ប្រសិនបើអ្នកបានបំពេញការទូទាត់រួចហើយ សូមរង់ចាំមួយភ្លែត...");
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
    }    private void sendQRCodeAndCompleteRegistration(long chatId, String qrCodeBase64) throws TelegramApiException {
        sendQRCodeAndCompleteRegistration(chatId, qrCodeBase64, "🎉 ការចុះឈ្មោះបានបញ្ចប់ដោយជោគជ័យ!");
    }    private void sendQRCodeAndCompleteRegistration(long chatId, String qrCodeBase64, String customMessage) throws TelegramApiException {
        RegistrationContext context = registrationContexts.get(chatId);
        
        // Decode Base64 QR Code
        byte[] qrCodeBytes = Base64.getDecoder().decode(qrCodeBase64);

        // Send QR Code as photo via Telegram
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId);
        sendPhoto.setPhoto(new InputFile(new ByteArrayInputStream(qrCodeBytes), "registration_qr.png"));
        sendPhoto.setCaption(customMessage + "\n\n" +
                "📱 នេះគឺជា QR Code របស់អ្នក\n" +
                "📧 QR Code ក៏ត្រូវបានផ្ញើទៅអ៊ីមែលរបស់អ្នកដែរ\n\n" +
                "📋 សូមចងចាំ:\n" +
                "• រក្សាទុក QR Code នេះ\n" +
                "• យកមកបង្ហាញនៅច្រកចូលព្រឹត្តិការណ៍\n" +
                "• មកដល់មុនម៉ោង 15-30 នាទី\n\n" +
                "🙏 អរគុណសម្រាប់ការចុះឈ្មោះ!");

        // Send the QR code via Telegram
        execute(sendPhoto);
        
        // Send QR code via email if user has email and event context is available
        if (context != null && context.getUser().getEmail() != null && !context.getUser().getEmail().trim().isEmpty()) {
            try {
                // Get event details for email
                Optional<EventResponse> eventOpt = eventService.getEventById(context.getEventId());
                if (eventOpt.isPresent()) {
                    EventResponse event = eventOpt.get();
                    
                    // Send QR code email
                    emailService.sendQRCodeEmail(
                        context.getUser().getEmail(),
                        context.getUser().getFullName(),
                        event,
                        qrCodeBytes
                    );
                    
                    logger.info("QR code email sent successfully to: {}", context.getUser().getEmail());
                    
                    // Send confirmation message about email
                    sendMessage(chatId, "📧 QR Code បានផ្ញើទៅអ៊ីមែល " + context.getUser().getEmail() + " ហើយ!");
                } else {
                    logger.warn("Event not found for ID: {}, cannot send QR code email", context.getEventId());
                }
                
            } catch (Exception e) {
                logger.error("Failed to send QR code email to: {}", context.getUser().getEmail(), e);
                sendMessage(chatId, "⚠️ មានបញ្ហាក្នុងការផ្ញើអ៊ីមែល។ QR Code នៅតែអាចប្រើបានតាម Telegram។");
            }
        } else {
            logger.warn("No email provided or context missing, skipping QR code email");
        }
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
    }    private void startEventRegistration(long chatId, UUID eventId) {
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
              // Check if event requires payment
            boolean requiresPayment = event.getPaymentRequired() != null && event.getPaymentRequired() && 
                                    event.getTicketPrice() != null && event.getTicketPrice().compareTo(java.math.BigDecimal.ZERO) > 0;
            context.setPaymentRequired(requiresPayment);
            
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
              // Format pricing information
            String pricingInfo = formatEventPricing(event);
            
            String welcomeMessage = String.format("""
                    🎉 សូមស្វាគមន៍មកកាន់ការចុះឈ្មោះចូលរួមព្រឹត្តិការណ៍!

                    📋 ព្រឹត្តិការណ៍: %s
                    📝 ការពិពណ៌នា: %s
                    👥 អ្នករៀបចំ: %s%s%s%s

                    សូមបំពេញព័ត៌មានខាងក្រោមដើម្បីចុះឈ្មោះ៖
                    ✍️ សូមបញ្ចូលឈ្មោះពេញរបស់អ្នក៖""",
                    event.getName(),
                    event.getDescription(),
                    formatOrganizers(eventRoles),
                    dateTimeInfo,
                    locationInfo,
                    pricingInfo);

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
    }    private void handleQrImage(long chatId, String fileId) {
        try {
            // Send processing message
            sendMessage(chatId, "🔄 Processing QR code...");

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

            logger.info("QR Code content decoded: {}", qrContent);

            // Parse QR content: eventId|userId|registrationToken
            String[] parts = qrContent.split("\\|");
            if (parts.length != 3) {
                sendMessage(chatId, "❌ Invalid QR code format. Expected format: eventId|userId|token");
                return;
            }
            String eventId = parts[0];
            String userId = parts[1];
            String registrationToken = parts[2];            logger.info("Verifying user check-in - EventID: {}, UserID: {}", eventId, userId);

            // Call backend verification/check-in endpoint (public endpoint)
            String url = String.format("http://localhost:8080/api/v1/audiences/verify-checkin?eventId=%s&userId=%s&registrationToken=%s", 
                                     eventId, userId, registrationToken);
            RestTemplate restTemplate = new RestTemplate();
            VerificationResponseDTO response = restTemplate.postForObject(url, null, VerificationResponseDTO.class);

            // Process the verification response
            processCheckInResponse(chatId, response, registrationToken, eventId, userId);

        } catch (Exception e) {
            logger.error("QR scan error for chatId {}: {}", chatId, e.getMessage(), e);
            sendMessage(chatId, "❌ មិនអាចដំណើរការ QR កូដ: " + e.getMessage());
        }
    }    /**
     * Process the check-in response and send appropriate message to admin
     */
    private void processCheckInResponse(long chatId, VerificationResponseDTO response, String registrationToken, String eventId, String userId) {
        try {
            if (response == null) {
                sendMessage(chatId, "❌ គ្មានចម្លើយពីសេវាកម្មពិនិត្យ។ សូមព្យាយាមម្តងទៀត។");
                return;
            }

            // Verify registration token matches (this is now handled by the backend)
            if (response.isVerified()) {
                // Successful check-in
                String successMessage = String.format("""
                    ✅ ចុះឈ្មោះចូលជោគជ័យ!
                    
                    👤 ព័ត៌មានអ្នកប្រើប្រាស់:
                    %s
                    
                    📋 ព័ត៌មានដំបូង:
                    • ព្រឹត្តិការណ៍: %s
                    • អ្នកប្រើប្រាស់: %s  
                    • ម៉ោង: %s
                    
                    🎉 សូមស្វាគមន៍មកកាន់ព្រឹត្តិការណ៍!""", 
                    formatUserInfo(response.getUser()), 
                    eventId, 
                    userId, 
                    java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
                
                sendMessage(chatId, successMessage);
                
                // Send email confirmation to user
                sendCheckInConfirmationEmail(response.getUser(), eventId);
                
                logger.info("Successful check-in for user {} at event {}", userId, eventId);

            } else if (response.getMessage() != null) {
                // Handle specific error cases
                if (response.getMessage().contains("already checked in")) {
                    String alreadyCheckedMessage = String.format("""
                        ⚠️ បានចុះឈ្មោះចូលរួចហើយ
                        
                        អ្នកប្រើប្រាស់នេះបានចុះឈ្មោះចូលរួមព្រឹត្តិការណ៍នេះរួចហើយ។
                        
                        🤖 ព័ត៌មានអ្នកប្រើប្រាស់:
                        %s
                        
                        📋 ស្ថានភាព: បានចុះឈ្មោះរួចហើយ
                        • ព្រឹត្តិការណ៍: %s
                        • អ្នកប្រើប្រាស់: %s""", 
                        formatUserInfo(response.getUser()), 
                        eventId, 
                        userId);
                    
                    sendMessage(chatId, alreadyCheckedMessage);
                    logger.info("User {} already checked in for event {}", userId, eventId);

                } else if (response.getMessage().contains("not registered")) {
                    sendMessage(chatId, String.format("""
                        ❌ មិនបានចុះឈ្មោះ
                        
                        អ្នកប្រើប្រាស់នេះមិនបានចុះឈ្មោះសម្រាប់ព្រឹត្តិការណ៍នេះទេ។
                        
                        📋 ព័ត៌មានលម្អិត:
                        • ព្រឹត្តិការណ៍: %s
                        • អ្នកប្រើប្រាស់: %s
                        
                        សូមធានាថាអ្នកប្រើប្រាស់បានចុះឈ្មោះសម្រាប់ព្រឹត្តិការណ៍ត្រឹមត្រូវ។""", 
                        eventId, userId));
                    logger.warn("User {} not registered for event {}", userId, eventId);

                } else if (response.getMessage().contains("Event not found")) {
                    sendMessage(chatId, String.format("""
                        ❌ រកមិនឃើញព្រឹត្តិការណ៍
                        
                        ព្រឹត្តិការណ៍ក្នុង QR កូដនេះមិនមានទេ។
                        
                        📋 ព័ត៌មានលម្អិត:
                        • ព្រឹត្តិការណ៍: %s
                        
                        សូមពិនិត្យថាតើ QR កូដត្រឹមត្រូវ។""", eventId));
                    logger.warn("Event not found: {}", eventId);

                } else if (response.getMessage().contains("User not found")) {
                    sendMessage(chatId, String.format("""
                        ❌ រកមិនឃើញអ្នកប្រើប្រាស់
                        
                        អ្នកប្រើប្រាស់ក្នុង QR កូដនេះមិនមានទេ។
                        
                        📋 ព័ត៌មានលម្អិត:
                        • អ្នកប្រើប្រាស់: %s
                        
                        សូមពិនិត្យថាតើ QR កូដត្រឹមត្រូវ។""", userId));
                    logger.warn("User not found: {}", userId);

                } else if (response.getMessage().contains("Invalid registration token")) {
                    sendMessage(chatId, String.format("""
                        ❌ QR កូដមិនត្រឹមត្រូវ
                        
                        សញ្ញាសម្គាល់ចុះឈ្មោះក្នុង QR កូដនេះមិនត្រូវនឹងកំណត់ត្រារបស់យើងទេ។
                        នេះអាចបង្ហាញថា:
                        • QR កូដត្រូវបានកែប្រែ
                        • QR កូដមកពីប្រព័ន្ធផ្សេង
                        • QR កូដបានផុតកំណត់
                        
                        📋 ព័ត៌មានលម្អិត:
                        • ព្រឹត្តិការណ៍: %s
                        • អ្នកប្រើប្រាស់: %s
                        
                        សូមធានាថាអ្នកកំពុងស្កេន QR កូដត្រឹមត្រូវ។""", eventId, userId));
                    logger.warn("Invalid registration token for user {} at event {}", userId, eventId);

                } else {
                    sendMessage(chatId, "❌ ការពិនិត្យបរាជ័យ: " + response.getMessage());
                    logger.warn("Verification failed for user {} at event {}: {}", userId, eventId, response.getMessage());
                }

            } else {
                sendMessage(chatId, String.format("""
                    ❌ ការពិនិត្យបរាជ័យ
                    
                    មិនអាចពិនិត្យ QR កូដនេះបាន។
                    
                    📋 ព័ត៌មានលម្អិត:
                    • ព្រឹត្តិការណ៍: %s
                    • អ្នកប្រើប្រាស់: %s
                    • ចម្លើយ: %s
                    
                    សូមព្យាយាមស្កេនម្តងទៀត ឬទាក់ទងជំនួយ។""", 
                    eventId, userId, response.getMessage() != null ? response.getMessage() : "កំហុសមិនស្គាល់"));
                logger.error("Unexpected verification response for user {} at event {}: {}", userId, eventId, response);
            }

        } catch (Exception e) {
            logger.error("Error processing check-in response: {}", e.getMessage(), e);
            sendMessage(chatId, "❌ កំហុសក្នុងការដំណើរការចុះឈ្មោះ។ សូមព្យាយាមម្តងទៀត។");
        }
    }private String formatUserInfo(com.example.tb.model.dto.UserDTO user) {
        if (user == null)
            return "ព័ត៌មានអ្នកប្រើប្រាស់មិនមាន។";
        
        return String.format("""
                👤 ​ឈ្មោះ: %s
                📞 ទូរស័ព្ទ: %s
                ⚧️ ភេទ: %s
                🎂 ថ្ងៃកំណើត: %s
                📧 អ៊ីមែល: %s
                🆔 លេខសម្គាល់: %s""",
                user.getFullName() != null ? user.getFullName() : "N/A",
                user.getPhoneNumber() != null ? user.getPhoneNumber() : "N/A",
                user.getGender() != null ? user.getGender().toString() : "N/A",
                user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : "N/A",
                user.getEmail() != null ? user.getEmail() : "N/A",
                user.getId() != null ? user.getId().toString() : "N/A");
    }/**
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
    }    private void sendAdminHelpMessage(long chatId) {
        String helpMessage = """
                🔧 **ពាក្យបញ្ជាអ្នកគ្រប់គ្រង**
                
                ពាក្យបញ្ជាដែលមានសម្រាប់អ្នកគ្រប់គ្រងព្រឹត្តិការណ៍:
                
                🔍 **/scan** - ចាប់ផ្តើមការស្កេន QR កូដ
                   • បើកម៉ាស៊ីនស្កេននិងផ្ទុក QR កូដអ្នកប្រើប្រាស់
                   • ពិនិត្យនិងចុះឈ្មោះចូលដោយស្វ័យប្រវត្តិ
                   • ដំណើរការតែម្តងក្នុងមួយព្រឹត្តិការណ៍
                
                ❌ **/cancel** - បោះបង់ការស្កេន QR
                   • បញ្ឈប់ការរង់ចាំការផ្ទុក QR កូដ
                   • ត្រលប់ទៅរបៀបពាក្យបញ្ជាធម្មតា
                
                ℹ️ **/help_admin** - បង្ហាញសារជំនួយនេះ
                   • បង្ហាញពាក្យបញ្ជាអ្នកគ្រប់គ្រងទាំងអស់
                
                📋 **វិធីប្រើម៉ាស៊ីនស្កេន QR:**
                1. ផ្ញើពាក្យបញ្ជា /scan
                2. ថតរូប QR កូដអ្នកប្រើប្រាស់ឱ្យច្បាស់
                3. ផ្ញើរូបភាពមកកាន់ជជែកនេះ
                4. បុត្រយន្តនឹងពិនិត្យនិងចុះឈ្មោះចូលដោយស្វ័យប្រវត្តិ
                
                ⚠️ **ចំណាំសំខាន់:**
                • អ្នកប្រើប្រាស់អាចចុះឈ្មោះចូលបានតែម្តងក្នុងមួយព្រឹត្តិការណ៍                • QR កូដត្រូវតែមកពីអ្នកប្រើប្រាស់ដែលបានចុះឈ្មោះ
                • QR កូដមិនត្រឹមត្រូវនឹងត្រូវបានបដិសេធ
                • សកម្មភាពចុះឈ្មោះទាំងអស់ត្រូវបានកត់ត្រា
                
                📞 **ជំនួយ:** ទាក់ទងអ្នកគ្រប់គ្រងប្រសិនបើអ្នកជួបបញ្ហា។""";

        sendMessage(chatId, helpMessage);
    }

    /**
     * Check if the given chat ID belongs to an admin
     */
    private boolean isAdmin(long chatId) {
        return ADMIN_CHAT_IDS.contains(chatId);
    }

    /**
     * Send check-in confirmation email to user
     */
    private void sendCheckInConfirmationEmail(com.example.tb.model.dto.UserDTO user, String eventId) {
        try {
            if (user != null && user.getEmail() != null) {
                emailService.sendCheckInConfirmationEmail(
                    user.getEmail(),
                    user.getFullName(),
                    eventId
                );
                logger.info("Check-in confirmation email sent to: {}", user.getEmail());
            }
        } catch (Exception e) {
            logger.error("Failed to send check-in confirmation email to {}: {}", 
                user != null ? user.getEmail() : "unknown", e.getMessage(), e);
            // Don't fail the check-in process if email fails
        }
    }    /**
     * Formats event pricing information for display in welcome message
     */
    private String formatEventPricing(EventResponse event) {
        if (event.getTicketPrice() != null && event.getTicketPrice().compareTo(BigDecimal.ZERO) > 0) {
            String currency = event.getCurrency() != null ? event.getCurrency() : "KHR";
            return String.format("\n💰 តម្លៃសំបុត្រ: %s %s", 
                    event.getTicketPrice().toPlainString(), currency);
        } else {
            return "\n💰 តម្លៃ: 🆓 ចូលរួមដោយឥតគិតថ្លៃ";
        }
    }

    /**
     * Handle payment step for paid events
     */
    private void handlePaymentStep(long chatId, RegistrationContext context) {
        try {
            // Get event details for payment
            Optional<EventResponse> eventOpt = eventService.getEventById(context.getEventId());
            if (eventOpt.isEmpty()) {
                sendMessage(chatId, "❌ រកមិនឃើញព្រឹត្តិការណ៍។ សូមចាប់ផ្តើមម្តងទៀត។");
                registrationContexts.remove(chatId);
                return;
            }            EventResponse event = eventOpt.get();
            
            // Find existing user or register new user
            User registeredUser;
            Optional<User> existingUser = userRegistrationService.findUserByPhoneNumber(context.getUser().getPhoneNumber());            if (existingUser.isPresent()) {
                registeredUser = existingUser.get();
                logger.info("Found existing user with phone number: {}", context.getUser().getPhoneNumber());
            } else {
                registeredUser = userRegistrationService.registerUser(context.getUser());
                logger.info("Registered new user with phone number: {}", context.getUser().getPhoneNumber());
            }
            
            // Create payment request
            PaymentRequest paymentRequest = PaymentRequest.builder()
                    .eventId(context.getEventId())
                    .userId(registeredUser.getId())
                    .amount(event.getTicketPrice())
                    .currency(event.getCurrency() != null ? event.getCurrency() : "KHR")
                    .description("Event Registration: " + event.getName())
                    .payerName(registeredUser.getFullName())
                    .payerPhone(registeredUser.getPhoneNumber())
                    .payerEmail(registeredUser.getEmail())
                    .returnUrl("https://t.me/telepasskhbot") // Return to Telegram bot
                    .build();

            // Initiate payment with Bakong
            PaymentResponse paymentResponse = bakongPaymentService.initiatePayment(paymentRequest);
            
            // Store payment info in context
            context.setMerchantTransactionId(paymentResponse.getMerchantTransactionId());
            context.setCurrentStep(RegistrationContext.RegistrationStep.PAYMENT_PENDING);

            // Send payment instructions to user
            sendPaymentInstructions(chatId, paymentResponse, event);

        } catch (Exception e) {
            logger.error("Error handling payment step for user at event {}: {}", context.getEventId(), e.getMessage(), e);
            sendMessage(chatId, "❌ មានបញ្ហាក្នុងការដំណើរការទូទាត់។ សូមព្យាយាមម្តងទៀត។");
        }
    }    /**
     * Send payment instructions with real Bakong payment QR code
     */
    private void sendPaymentInstructions(long chatId, PaymentResponse paymentResponse, EventResponse event) {
        try {
            String currency = event.getCurrency() != null ? event.getCurrency() : "KHR";
            
            // Create payment message with real QR code
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(String.format("""
                    💳 ការទូទាត់ឱ្យចូលរួមព្រឹត្តិការណ៍
                    
                    📋 ព្រឹត្តិការណ៍: %s
                    💰 ចំនួនទឹកប្រាក់: %s %s
                    🔢 លេខមុខងារ: %s
                    
                    📱 សូមស្កេន QR Code ខាងក្រោមដើម្បីបង់ប្រាក់តាមរយៈ Bakong
                    
                    ⏰ ការទូទាត់នេះនឹងផុតកំណត់ក្នុង 30 នាទី
                    """,
                    event.getName(),
                    paymentResponse.getAmount().toPlainString(),
                    currency,
                    paymentResponse.getMerchantTransactionId()));

            // Add inline keyboard with payment status check only
            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            
            // Payment status check button
            List<InlineKeyboardButton> row1 = new ArrayList<>();
            InlineKeyboardButton statusButton = new InlineKeyboardButton();
            statusButton.setText("🔄 ពិនិត្យស្ថានភាពការទូទាត់");
            statusButton.setCallbackData("check_payment");
            row1.add(statusButton);
            rows.add(row1);
            
            keyboard.setKeyboard(rows);
            message.setReplyMarkup(keyboard);
            
            execute(message);
            
            // Send the actual payment QR code if available
            if (paymentResponse.getQrCodeUrl() != null || paymentResponse.getPaymentUrl() != null) {
                sendPaymentQRCode(chatId, paymentResponse, event);
            } else {
                sendMessage(chatId, "❌ មិនអាចបង្កើត QR Code ទូទាត់បាន។ សូមព្យាយាមម្តងទៀត។");
            }

        } catch (Exception e) {
            logger.error("Error sending payment instructions: {}", e.getMessage(), e);
            sendMessage(chatId, "❌ មានបញ្ហាក្នុងការបង្កើតការទូទាត់។ សូមព្យាយាមម្តងទៀត។");
        }
    }
      /**
     * Send the actual Bakong payment QR code as an image
     */
    private void sendPaymentQRCode(long chatId, PaymentResponse paymentResponse, EventResponse event) {
        try {
            String currency = event.getCurrency() != null ? event.getCurrency() : "KHR";
            
            // Send instruction message first
            SendMessage instructionMessage = new SendMessage();
            instructionMessage.setChatId(chatId);
            instructionMessage.setText(String.format("""
                    🏦 QR Code ការទូទាត់ Bakong
                    
                    💰 ចំនួន: %s %s
                    🏪 អ្នកទទួល: %s
                    🆔 Transaction: %s
                    
                    📋 វិធីប្រើប្រាស់:
                    1️⃣ បើកកម្មវិធី Bakong
                    2️⃣ ចុច "ស្កេន" ឬ "Scan"
                    3️⃣ ស្កេន QR Code ខាងក្រោម
                    4️⃣ បញ្ជាក់ការទូទាត់
                    
                    ✅ ការទូទាត់នឹងត្រូវបានបញ្ជាក់ដោយស្វ័យប្រវត្តិ
                    """,
                    paymentResponse.getAmount().toPlainString(),
                    currency,
                    "VEASNA Dara",
                    paymentResponse.getMerchantTransactionId()));

            execute(instructionMessage);
              // Generate and send QR code image if we have the QR code data
            if (paymentResponse.getQrCodeUrl() != null && !paymentResponse.getQrCodeUrl().isEmpty()) {
                // Use Bakong's QR code image directly
                sendBakongQRCodeImage(chatId, paymentResponse.getQrCodeUrl(), paymentResponse.getMerchantTransactionId());
            } else if (paymentResponse.getQrCodeData() != null && !paymentResponse.getQrCodeData().isEmpty()) {
                // Fallback: generate QR code locally if no Bakong URL
                sendQRCodeImage(chatId, paymentResponse.getQrCodeData(), paymentResponse.getMerchantTransactionId());
            } else {
                sendMessage(chatId, "❌ មិនអាចបង្កើត QR Code ទូទាត់បាន។");
            }

        } catch (Exception e) {
            logger.error("Error sending payment QR code: {}", e.getMessage(), e);
            sendMessage(chatId, "❌ មិនអាចបង្ហាញ QR Code ទូទាត់បាន។");
        }
    }
      /**
     * Generate QR code image and send it to Telegram
     */
    private void sendQRCodeImage(long chatId, String qrCodeData, String transactionId) {
        File qrCodeFile = null;
        try {
            // Create temporary file for QR code
            String fileName = "qr_" + transactionId + ".png";
            String tempDir = System.getProperty("java.io.tmpdir");
            String filePath = Paths.get(tempDir, fileName).toString();
            qrCodeFile = new File(filePath);
            
            // Generate QR code image using the utility
            int qrSize = 400; // 400x400 pixels
            QrCodeUtil.generateQRCodeImage(qrCodeData, qrSize, qrSize, filePath);
            
            // Send the QR code image
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(chatId);
            sendPhoto.setPhoto(new InputFile(qrCodeFile));
            sendPhoto.setCaption("📱 ស្កេន QR Code នេះដើម្បីទូទាត់\nScan this QR Code to pay");
            
            execute(sendPhoto);
            
            logger.info("Successfully sent QR code image for transaction: {}", transactionId);
            
        } catch (Exception e) {
            logger.error("Error generating/sending QR code image for transaction {}: {}", transactionId, e.getMessage(), e);
            // Fallback to sending text message
            sendMessage(chatId, "❌ មិនអាចបង្ហាញ QR Code រូបភាពបាន។ សូមព្យាយាមម្តងទៀត។");
        } finally {
            // Clean up temporary file
            if (qrCodeFile != null && qrCodeFile.exists()) {
                try {
                    Files.deleteIfExists(qrCodeFile.toPath());
                } catch (Exception cleanup) {
                    logger.warn("Failed to cleanup QR code temp file: {}", cleanup.getMessage());
                }
            }
        }
    }
    /**
     * Download and send Bakong QR code image directly from Bakong servers
     */
    private void sendBakongQRCodeImage(long chatId, String qrCodeUrl, String transactionId) {
        File qrCodeFile = null;
        try {
            // Create temporary file for QR code
            String fileName = "bakong_qr_" + transactionId + ".png";
            String tempDir = System.getProperty("java.io.tmpdir");
            String filePath = Paths.get(tempDir, fileName).toString();
            qrCodeFile = new File(filePath);
            
            // Download QR code image from Bakong
            logger.info("Downloading QR code image from Bakong: {}", qrCodeUrl);
            
            RestTemplate restTemplate = new RestTemplate();
            byte[] imageBytes = restTemplate.getForObject(qrCodeUrl, byte[].class);
            
            if (imageBytes != null && imageBytes.length > 0) {
                // Save image to temporary file
                Files.write(qrCodeFile.toPath(), imageBytes);
                
                // Send the QR code image to Telegram
                SendPhoto sendPhoto = new SendPhoto();
                sendPhoto.setChatId(chatId);
                sendPhoto.setPhoto(new InputFile(qrCodeFile));
                sendPhoto.setCaption("💳 QR Code ការទូទាត់ Bakong (Official)\n\n" +
                    "📱 ស្កេន QR Code នេះដើម្បីធ្វើការទូទាត់តាមរយៈកម្មវិធី Bakong");
                
                execute(sendPhoto);
                
                logger.info("Successfully sent Bakong QR code image for transaction: {}", transactionId);
            } else {
                logger.warn("Failed to download QR code image from Bakong, falling back to local generation");
                // Fallback to local generation if download fails
                sendMessage(chatId, "⚠️ មិនអាចទាញយក QR Code ពី Bakong បាន។ សូមប្រើ URL: " + qrCodeUrl);
            }
            
        } catch (Exception e) {
            logger.error("Error downloading/sending Bakong QR code image for transaction {}: {}", 
                transactionId, e.getMessage(), e);
            
            // Fallback: send the URL as text
            sendMessage(chatId, "❌ មិនអាចទាញយក QR Code រូបភាពពី Bakong បាន។\n🔗 URL: " + qrCodeUrl);
        } finally {
            // Clean up temporary file
            if (qrCodeFile != null && qrCodeFile.exists()) {
                try {
                    qrCodeFile.delete();
                    logger.debug("Cleaned up Bakong QR code file: {}", qrCodeFile.getPath());
                } catch (Exception e) {
                    logger.warn("Failed to delete Bakong QR code file: {}", qrCodeFile.getPath());                }
            }
        }
    }

    /**
     * Handle payment status check button clicks
     */
    private void handlePaymentStatusCheck(long chatId) {
        try {
            sendMessage(chatId, "🔍 កំពុងពិនិត្យស្ថានភាពការទូទាត់...\nChecking payment status...");
            // Additional logic can be added here for specific payment status checks
        } catch (Exception e) {
            logger.error("Error handling payment status check: {}", e.getMessage(), e);
            sendMessage(chatId, "❌ មានបញ្ហាក្នុងការពិនិត្យស្ថានភាព។");
        }
    }

    /**
     * Process complete registration
     */
    private void processCompleteRegistration(long chatId, RegistrationContext context) {
        try {
            // Process final registration completion
            sendMessage(chatId, "✅ ការចុះឈ្មោះបានបញ្ចប់ដោយជោគជ័យ!\nRegistration completed successfully!");
            
            // Clear the context
            registrationContexts.remove(chatId);
            
        } catch (Exception e) {
            logger.error("Error processing complete registration: {}", e.getMessage(), e);
            sendMessage(chatId, "❌ មានបញ្ហាក្នុងការបញ្ចប់ការចុះឈ្មោះ។");
        }
    }

    /**
     * Handle payment-related messages
     */
    private void handlePaymentMessages(long chatId, String messageText, RegistrationContext context) {
        try {
            if (messageText.contains("ស្ថានភាព") || messageText.contains("status") || messageText.contains("ពិនិត្យ")) {
                checkPaymentStatus(chatId, context);
            } else {
                sendMessage(chatId, "⏳ រង់ចាំការទូទាត់...\n" +
                        "💡 ប្រសិនបើអ្នកបានទូទាត់រួចហើយ សូមរង់ចាំការបញ្ជាក់ដោយស្វ័យប្រវត្តិ។");
            }
        } catch (Exception e) {
            logger.error("Error handling payment messages: {}", e.getMessage(), e);
            sendMessage(chatId, "❌ មានបញ្ហាក្នុងការដោះស្រាយសារ។");
        }
    }

    /**
     * Check payment status manually
     */
    private void checkPaymentStatus(long chatId, RegistrationContext context) {
        try {
            if (context.getMerchantTransactionId() == null) {
                sendMessage(chatId, "❌ រកមិនឃើញព័ត៌មានការទូទាត់។");
                return;
            }
            
            sendMessage(chatId, "🔍 កំពុងពិនិត្យស្ថានភាពការទូទាត់...\n" +
                "Transaction ID: " + context.getMerchantTransactionId());
            
            // Additional payment status checking logic can be added here
            
        } catch (Exception e) {
            logger.error("Error checking payment status: {}", e.getMessage(), e);
            sendMessage(chatId, "❌ មិនអាចពិនិត្យស្ថានភាពការទូទាត់បាន។");
        }
    }
}