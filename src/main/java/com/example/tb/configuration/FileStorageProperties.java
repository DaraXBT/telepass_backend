package com.example.tb.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "file")
public class FileStorageProperties {

    private String uploadPath;
    public String getUploadPath() {
        return uploadPath;
    }
    public void setUploadPath(String uploadPath) {
        this.uploadPath = uploadPath;
    }
}