package com.example.tb.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "bakong")
@Data
public class BakongConfig {
    
    private Api api = new Api();
    private Payment payment = new Payment();
    private Monitoring monitoring = new Monitoring();
    private Merchant merchant = new Merchant();
    private Qr qr = new Qr();
    @Data
    public static class Merchant {
        private String id;
        private String name;
        private String city;
        private String phone;
        private String acquiringBank;
        private String categoryCode = "5999";
    }
      @Data
    public static class Api {
        private String token;
        private String baseUrl;
        private String sandboxUrl;
        private String version;
        private String merchantId;
        private String secretKey;
        private String publicKey;
        private String callbackUrl;        private String returnUrl;
        private boolean sandbox = true;
        private boolean mockMode = false;
        private int timeout = 30000;
        private Retry retry = new Retry();
        private Signature signature = new Signature();
        private Webhook webhook = new Webhook();
          public String getEffectiveBaseUrl() {
            // Always use the main base URL since sandbox URL is not accessible
            // The sandbox mode will be handled by mock-mode flag instead
            return baseUrl;
        }
    }
    
    @Data
    public static class Retry {
        private int maxAttempts = 3;
        private long delay = 1000;
    }
    
    @Data
    public static class Payment {
        private String currency = "KHR";
        private long minAmount = 1000;
        private long maxAmount = 50000000;
        private int expiryMinutes = 15;
        private int autoConfirmTimeout = 300;
        private Notification notification = new Notification();
    }
    
    @Data
    public static class Signature {
        private String algorithm = "SHA256withRSA";
        private String charset = "UTF-8";
    }
    
    @Data
    public static class Webhook {
        private boolean verifySignature = true;
        private int timeout = 10000;
        private List<String> ipWhitelist = new ArrayList<>();
    }
      @Data
    public static class Qr {
        private int size = 300;
        private String format = "PNG";
        private String errorCorrection = "M";
        private boolean enableKhmerLanguage = false;
    }
    
    @Data
    public static class Notification {
        private boolean enabled = true;
        private String successTemplate = "payment-success";
        private String failureTemplate = "payment-failed";
    }
    
    @Data
    public static class Monitoring {
        private boolean metricsEnabled = true;
        private boolean auditLogEnabled = true;
        private int transactionLogRetentionDays = 90;
        private AlertThresholds alertThresholds = new AlertThresholds();
    }
    
    @Data
    public static class AlertThresholds {
        private double failureRate = 0.05;
        private long responseTime = 5000;
        private int concurrentTransactions = 100;
    }
}
