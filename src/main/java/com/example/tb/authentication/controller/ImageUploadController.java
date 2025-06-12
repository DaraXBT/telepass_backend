package com.example.tb.authentication.controller;


import com.example.tb.authentication.service.image.ImageUploadService;
import com.example.tb.exception.RequestIncorrectException;
import com.example.tb.model.response.ApiResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/images")
@CrossOrigin
public class ImageUploadController {
    private final ImageUploadService imageService;

    public ImageUploadController(ImageUploadService imageService) {
        this.imageService = imageService;
    }

    @GetMapping("/getImage")
    public ResponseEntity<?> getImagesByFileName(@RequestParam String fileName) {
        try{
            Resource file = imageService.getImagesByFileName(fileName);
            return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(file);
        }catch (Exception e){
            throw new RequestIncorrectException("Incorrect Name", "this file isn't exist.");
        }
    }

    @PostMapping(value = "/file" , consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse <?> uploadFile(@RequestBody MultipartFile image) throws IOException {
        String fileName = imageService.uploadFile(image);
        return ApiResponse.builder()
                .date(LocalDateTime.now())
                .payload(fileName)
                .message("Image upload successful")
                .build();
    }

//    @PutMapping("/addImage/{id}")
//    public ResponseEntity<?> addImage(@RequestParam String fileName, @PathVariable String id) {
//        imageService.addImage(fileName, id);
//        return new ResponseEntity<>(new ApiResponse<>(
//                "Image added successfully",
//                fileName,
//                LocalDateTime.now()
//        ), HttpStatus.OK);
//    }
}
