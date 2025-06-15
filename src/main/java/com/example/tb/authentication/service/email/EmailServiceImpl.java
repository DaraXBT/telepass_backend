package com.example.tb.authentication.service.email;

import com.example.tb.configuration.OtpConfig;
import com.example.tb.model.response.EventResponse;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ByteArrayResource;

import java.io.UnsupportedEncodingException;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mail.debug.mode", havingValue = "false", matchIfMissing = true)
public class EmailServiceImpl implements EmailService {
    
    private final JavaMailSender mailSender;
    private final OtpConfig otpConfig;
    
    @Value("${WebBaseUrl:http://localhost:3000/}")
    private String webBaseUrl;
    
    @Value("${email.from.address:noreply@telepass.com}")
    private String fromAddress;
    
    @Value("${email.from.name:Telepass Team}")
    private String fromName;

    @Override
    public void sendRegistrationInvitation(String email, String name, String msg) throws MessagingException, UnsupportedEncodingException {        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromAddress, fromName);
            helper.setTo(email);
            helper.setSubject("Registration Invitation - Telepass");
            
            String content = String.format(
                "Dear %s,\n\n%s\n\nPlease visit %s to complete your registration.\n\nBest regards,\nTelepass Team",
                name, msg, webBaseUrl
            );
              helper.setText(content);
            mailSender.send(message);
            
            log.info("Registration invitation sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send registration invitation to: {}", email, e);
            throw e;
        }
    }

    @Override
    public void sendVerificationEmail(String email, String verificationUrl, String otp) throws MessagingException, UnsupportedEncodingException {        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromAddress, fromName);
            helper.setTo(email);
            helper.setSubject("Email Verification - Telepass");
            
            String content = String.format(
                "Dear User,\n\n" +
                "Please verify your email address using either method below:\n\n" +
                "Method 1: Click the verification link:\n%s\n\n" +
                "Method 2: Use this verification code: %s\n\n" +
                "This code will expire in %d minutes.\n\n" +
                "If you didn't request this verification, please ignore this email.\n\n" +
                "Best regards,\nTelepass Team",
                verificationUrl, otp, otpConfig.getExpirationTime() / 60
            );
            
            helper.setText(content);
            mailSender.send(message);
            
            log.info("Verification email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", email, e);
            throw e;
        }
    }

    @Override
    public void sendOtpToEmail(String email, String otp) throws MessagingException, UnsupportedEncodingException {        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromAddress, fromName);
            helper.setTo(email);
            helper.setSubject(otpConfig.getEmail().getSubject());
            
            String content = otpConfig.getEmail().getTemplate()
                .replace("{otp}", otp)
                .replace("{expiration}", String.valueOf(otpConfig.getExpirationTime() / 60));
            
            helper.setText(content);
            mailSender.send(message);
            
            log.info("OTP email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", email, e);
            throw e;
        }
    }

    @Override
    public void sendPasswordResetEmail(String email, String resetUrl) throws MessagingException, UnsupportedEncodingException {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromAddress, fromName);
            helper.setTo(email);
            helper.setSubject("Password Reset - Telepass");
            
            String content = String.format(
                "Dear User,\n\n" +
                "You have requested to reset your password for your Telepass account.\n\n" +
                "Click the following link to reset your password:\n%s\n\n" +
                "This link will expire in 24 hours for security reasons.\n\n" +
                "If you did not request this password reset, please ignore this email and your password will remain unchanged.\n\n" +
                "For security reasons, please do not share this link with anyone.\n\n" +
                "Best regards,\nTelepass Team",
                resetUrl
            );
            
            helper.setText(content);
            mailSender.send(message);
            
            log.info("Password reset email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", email, e);
            throw e;
        }
    }    @Override
    public void sendQRCodeEmail(String email, String userName, EventResponse event, byte[] qrCodeBytes) throws MessagingException, UnsupportedEncodingException {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromAddress, fromName);
            helper.setTo(email);
            helper.setSubject("Event Registration QR Code - Telepass");
            
            // Format datetime strings
            String startDateTimeStr = event.getStartDateTime() != null 
                ? event.getStartDateTime().format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy 'at' hh:mm a"))
                : "To be announced";
            String endDateTimeStr = event.getEndDateTime() != null 
                ? event.getEndDateTime().format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy 'at' hh:mm a"))
                : "To be announced";
            
            // Format location
            String locationDisplay = "";
            if (event.getLocation() != null && !event.getLocation().trim().isEmpty()) {
                String location = event.getLocation().trim();
                if (location.startsWith("http://") || location.startsWith("https://")) {
                    locationDisplay = String.format("<a href=\"%s\" style=\"color: #3498db; text-decoration: none;\">📍 View on Map</a>", location);
                } else {
                    locationDisplay = "📍 " + location;
                }
            } else {
                locationDisplay = "📍 To be announced";
            }
            
            // Create HTML content with comprehensive event details
            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Event Registration QR Code</title>
                </head>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f4f4f4;">
                    <div style="background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0 0 10px rgba(0,0,0,0.1);">
                        <!-- Header -->
                        <div style="text-align: center; margin-bottom: 30px;">
                            <h1 style="color: #2c3e50; margin-bottom: 10px;">🎉 Registration Successful!</h1>
                            <h2 style="color: #34495e; font-weight: normal; margin-bottom: 5px;">%s</h2>
                            <div style="background-color: #3498db; color: white; padding: 8px 16px; border-radius: 20px; display: inline-block; font-size: 14px; font-weight: bold;">
                                %s
                            </div>
                        </div>
                        
                        <!-- User Greeting -->
                        <div style="text-align: center; margin: 30px 0;">
                            <p style="font-size: 18px; color: #2c3e50; margin-bottom: 15px;">
                                <strong>Dear %s,</strong>
                            </p>
                            <p style="font-size: 16px; color: #555; margin-bottom: 20px;">
                                Congratulations! You have successfully registered for this amazing event. 
                                Below is your personal QR code and complete event information.
                            </p>
                        </div>
                        
                        <!-- Event Details Section -->
                        <div style="background-color: #f8f9fa; padding: 25px; border-radius: 10px; margin: 30px 0; border-left: 5px solid #3498db;">
                            <h3 style="color: #2c3e50; margin-top: 0; margin-bottom: 20px;">📋 Event Details</h3>
                            
                            <div style="margin-bottom: 15px;">
                                <strong style="color: #34495e;">📝 Description:</strong>
                                <p style="margin: 5px 0; color: #555; font-style: italic;">%s</p>
                            </div>
                            
                            <div style="display: table; width: 100%%; margin-bottom: 15px;">
                                <div style="display: table-row;">
                                    <div style="display: table-cell; width: 50%%; padding-right: 10px;">
                                        <strong style="color: #e74c3c;">🕐 Start Time:</strong><br>
                                        <span style="color: #555;">%s</span>
                                    </div>
                                    <div style="display: table-cell; width: 50%%; padding-left: 10px;">
                                        <strong style="color: #e74c3c;">🕐 End Time:</strong><br>
                                        <span style="color: #555;">%s</span>
                                    </div>
                                </div>
                            </div>
                            
                            <div style="margin-bottom: 15px;">
                                <strong style="color: #34495e;">📍 Location:</strong><br>
                                <span style="color: #555;">%s</span>
                            </div>
                            
                            <div style="display: table; width: 100%%;">
                                <div style="display: table-row;">
                                    <div style="display: table-cell; width: 50%%; padding-right: 10px;">
                                        <strong style="color: #27ae60;">👥 Capacity:</strong><br>
                                        <span style="color: #555;">%d people</span>
                                    </div>
                                    <div style="display: table-cell; width: 50%%; padding-left: 10px;">
                                        <strong style="color: #f39c12;">✅ Registered:</strong><br>
                                        <span style="color: #555;">%d people</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                        
                        <!-- QR Code Section -->
                        <div style="text-align: center; margin: 40px 0; padding: 30px; background-color: #f8f9fa; border-radius: 15px; border: 3px dashed #3498db;">
                            <p style="font-size: 20px; font-weight: bold; color: #2c3e50; margin-bottom: 20px;">
                                📱 Your Event QR Code
                            </p>
                            <img src="cid:qrcode" alt="Event QR Code" 
                                 style="width: 400px; height: 400px; border: 4px solid #3498db; border-radius: 15px; display: block; margin: 0 auto; box-shadow: 0 4px 8px rgba(0,0,0,0.2);">
                            <p style="font-size: 14px; color: #7f8c8d; margin-top: 15px; font-style: italic;">
                                Present this QR code at the event entrance
                            </p>
                        </div>
                        
                        <!-- Instructions -->
                        <div style="background-color: #ecf0f1; padding: 20px; border-radius: 8px; margin: 30px 0;">
                            <h3 style="color: #e74c3c; margin-top: 0;">📋 Important Instructions:</h3>
                            <ul style="margin: 0; padding-left: 20px;">
                                <li style="margin-bottom: 8px;">🔒 <strong>Keep this QR code safe and accessible</strong></li>
                                <li style="margin-bottom: 8px;">🚪 <strong>Present it at the event entrance for quick check-in</strong></li>
                                <li style="margin-bottom: 8px;">🚫 <strong>Do not share this QR code with others</strong></li>
                                <li style="margin-bottom: 8px;">📱 <strong>Save this image to your phone for easy access</strong></li>
                                <li style="margin-bottom: 8px;">⏰ <strong>Arrive 15-30 minutes early for smooth check-in</strong></li>
                            </ul>
                        </div>
                        
                        <!-- Registration Status -->
                        <div style="text-align: center; margin: 30px 0; padding: 20px; background-color: #d4edda; border-radius: 8px; border: 1px solid #c3e6cb;">
                            <p style="font-size: 16px; color: #155724; margin: 0; font-weight: bold;">
                                ✅ Registration Status: <span style="color: #28a745;">CONFIRMED</span>
                            </p>
                            <p style="font-size: 14px; color: #155724; margin: 10px 0 0 0;">
                                You are successfully registered for this event!
                            </p>
                        </div>
                        
                        <!-- Footer -->
                        <div style="text-align: center; margin-top: 40px; padding-top: 20px; border-top: 2px solid #ecf0f1;">
                            <p style="font-size: 16px; color: #2c3e50; margin-bottom: 5px;">
                                We look forward to seeing you at the event! 🎉
                            </p>
                            <p style="font-size: 14px; color: #7f8c8d;">
                                Best regards,<br>
                                <strong>Telepass Team</strong>
                            </p>
                        </div>
                        
                        <!-- Footer Note -->
                        <div style="text-align: center; margin-top: 30px; padding: 15px; background-color: #f8f9fa; border-radius: 8px;">
                            <p style="font-size: 12px; color: #95a5a6; margin: 0;">
                                This is an automated email. Please do not reply to this message.<br>
                                For questions, please contact the event organizers.
                            </p>
                        </div>
                    </div>
                </body>
                </html>
                """, 
                event.getName() != null ? event.getName() : "Event",
                event.getCategory() != null ? event.getCategory().toUpperCase() : "GENERAL",
                userName != null ? userName : "User",
                event.getDescription() != null ? event.getDescription() : "No description available",
                startDateTimeStr,
                endDateTimeStr,
                locationDisplay,
                event.getCapacity(),
                event.getRegistered()
            );
            
            // Set HTML content
            helper.setText(htmlContent, true);
            
            // Embed QR code as inline image
            ByteArrayResource qrCodeResource = new ByteArrayResource(qrCodeBytes);
            helper.addInline("qrcode", qrCodeResource, "image/png");
            
            mailSender.send(message);
            
            log.info("Enhanced QR code email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send QR code email to: {}", email, e);
            throw e;
        }
    }

    @Override
    public void sendCheckInConfirmationEmail(String email, String userName, String eventId) throws MessagingException, UnsupportedEncodingException {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromAddress, fromName);
            helper.setTo(email);
            helper.setSubject("✅ ការចុះឈ្មោះចូលរួមជោគជ័យ - Check-in Confirmation");
            
            String currentDateTime = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            
            String htmlContent = String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                        .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                        .success-badge { background: #28a745; color: white; padding: 10px 20px; border-radius: 25px; display: inline-block; margin: 20px 0; }
                        .info-box { background: white; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #667eea; }
                        .footer { text-align: center; margin-top: 30px; padding: 20px; color: #666; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🎉 ចុះឈ្មោះចូលរួមជោគជ័យ!</h1>
                            <p>Check-in Successful!</p>
                        </div>
                        <div class="content">
                            <div class="success-badge">
                                ✅ បានបញ្ជាក់ការចូលរួម
                            </div>
                            
                            <div class="info-box">
                                <h3>👤 ព័ត៌មានអ្នកចូលរួម</h3>
                                <p><strong>ឈ្មោះ:</strong> %s</p>
                                <p><strong>អ៊ីមែល:</strong> %s</p>
                                <p><strong>ព្រឹត្តិការណ៍:</strong> %s</p>
                                <p><strong>ម៉ោងចុះឈ្មោះ:</strong> %s</p>
                            </div>
                            
                            <div class="info-box">
                                <h3>📋 សេចក្តីណែនាំ</h3>
                                <ul>
                                    <li>អ្នកបានចុះឈ្មោះចូលរួមព្រឹត្តិការណ៍ដោយជោគជ័យ</li>
                                    <li>សូមរក្សាទុកអ៊ីមែលនេះសម្រាប់ការយោង</li>
                                    <li>ប្រសិនបើមានសំណួរ សូមទាក់ទងអ្នកគ្រប់គ្រង</li>
                                </ul>
                            </div>
                            
                            <p style="text-align: center; margin-top: 30px;">
                                <strong>🎊 សូមស្វាគមន៍មកកាន់ព្រឹត្តិការណ៍!</strong>
                            </p>
                        </div>
                        <div class="footer">
                            <p>© 2025 Telepass - Event Management System</p>
                            <p>This is an automated message. Please do not reply.</p>
                        </div>
                    </div>
                </body>
                </html>""",
                userName != null ? userName : "N/A",
                email,
                eventId,
                currentDateTime
            );
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            
            log.info("Check-in confirmation email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send check-in confirmation email to: {}", email, e);
            throw e;
        }
    }
}
