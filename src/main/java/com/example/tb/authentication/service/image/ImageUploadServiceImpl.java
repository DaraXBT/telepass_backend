package com.example.tb.authentication.service.image;

import com.example.tb.configuration.FileStorageProperties;
import com.example.tb.exception.RequestIncorrectException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class ImageUploadServiceImpl implements ImageUploadService {
    private final FileStorageProperties fileStorageProperties;

    public ImageUploadServiceImpl(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
    }

    @Override
    public Resource getImagesByFileName(String fileName) throws IOException  {
        Path filePath = Paths.get(fileStorageProperties.getUploadPath(), fileName);
        byte[] fileBytes = Files.readAllBytes(filePath);
        return new ByteArrayResource(fileBytes);
    }

    @Override
    public String uploadFile(MultipartFile image) throws IOException {
        final Path root = Paths.get(fileStorageProperties.getUploadPath());
        try {
            //get file
            String fileName = image.getOriginalFilename().toLowerCase();
            if (fileName != null &&
                    fileName.contains(".jpg") ||
                    fileName.contains(".png") ||
                    fileName.contains(".jpeg")||
                    fileName.contains(".heic")
            ){
                fileName = UUID.randomUUID()+"."+ StringUtils.getFilenameExtension(fileName);
                if (!Files.exists(root)){
                    Files.createDirectories(root);

                }
                Files.copy(image.getInputStream(),root.resolve(fileName) , StandardCopyOption.REPLACE_EXISTING);
                return fileName;
            }else {
                throw  new RequestIncorrectException("Incorrect file","File should be image file");
            }
        }catch (IOException e){
            throw  new RequestIncorrectException("Incorrect file","File should be image file");
        }
    }

//    @Override
//    public void addImage(String fileName, String id) {
//        Optional<Project> optionalProject = projectRepository.findById(id);
//        if (optionalProject.isPresent()) {
//            Project project = optionalProject.get();
//            project.setImagePro(fileName);
//            projectRepository.save(project);
//        } else {
//            throw new NotFoundExceptionClass("Id not found");
//        }
//    }
}

