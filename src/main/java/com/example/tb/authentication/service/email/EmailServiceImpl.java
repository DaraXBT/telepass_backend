package com.example.tb.authentication.service.email;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;

@Service
public class EmailServiceImpl implements EmailService {
    private final TemplateEngine emailTemplateEngine;
    private final JavaMailSender emailSender;
    @Value("${spring.mail.username}")
    private String sender;
    private static final String MESSAGE = "otp-send";
    private static final String OTPEMAIL = "otp-email";
    private static final String VERIFIED = "verified-email";

    public EmailServiceImpl(TemplateEngine emailTemplateEngine, JavaMailSender emailSender) {
        this.emailTemplateEngine = emailTemplateEngine;
        this.emailSender = emailSender;
    }

    public void sendRegistrationInvitation(String email, String name, String msg) throws MessagingException, UnsupportedEncodingException {
        MimeMessage mimeMessage = emailSender.createMimeMessage();
        MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        boolean html = true;


        Context thymeleafContext = new Context(LocaleContextHolder.getLocale());
        thymeleafContext.setVariable("name", name);
        thymeleafContext.setVariable("msg", msg);

        final String emailContent = this.emailTemplateEngine.process(MESSAGE, thymeleafContext);

        messageHelper.setFrom(sender, "Telepass");
        messageHelper.setTo(email);
        messageHelper.setSubject("Otp code verification");
        messageHelper.setText(emailContent, html);
        emailSender.send(mimeMessage);
    }

    @Override
    public void sendVerificationEmail(String email, String verificationUrl, String otp) throws MessagingException, UnsupportedEncodingException {
        MimeMessage mimeMessage = emailSender.createMimeMessage();
        MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        boolean html = true;


        Context thymeleafContext = new Context(LocaleContextHolder.getLocale());
        thymeleafContext.setVariable("verificationUrl", verificationUrl);
        thymeleafContext.setVariable("otp", otp);
        final String emailContent = this.emailTemplateEngine.process(VERIFIED, thymeleafContext);

        messageHelper.setFrom(sender, "Gigi - Deployment");
        messageHelper.setTo(email);
        messageHelper.setSubject("Email Verification");
        messageHelper.setText(emailContent, html);
        emailSender.send(mimeMessage);
    }

    @Override
    public void sendOtpToEmail(String email, String otp) throws MessagingException, UnsupportedEncodingException {
        MimeMessage mimeMessage = emailSender.createMimeMessage();
        MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        boolean html = true;


        Context thymeleafContext = new Context(LocaleContextHolder.getLocale());
        thymeleafContext.setVariable("otp", otp);

        final String emailContent = this.emailTemplateEngine.process(OTPEMAIL, thymeleafContext);

        messageHelper.setFrom(sender, "Telepass");
        messageHelper.setTo(email);
        messageHelper.setSubject("Otp code verification");
        messageHelper.setText(emailContent, html);
        emailSender.send(mimeMessage);

    }
}
