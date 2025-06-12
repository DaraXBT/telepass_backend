package com.example.tb.authentication.service.image;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ImageUploadService {
    Resource getImagesByFileName(String fileName) throws IOException;

    String uploadFile(MultipartFile image) throws IOException;

//    void addImage(String fileName, String id);
}

