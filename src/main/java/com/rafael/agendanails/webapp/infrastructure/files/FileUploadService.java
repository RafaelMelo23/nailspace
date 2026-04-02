package com.rafael.agendanails.webapp.infrastructure.files;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

@Log4j2
@Component
public class FileUploadService {

    @Value("${file.upload.dir}")
    private String uploadDir;

    @Value("${domain.url}")
    private String baseUrl;

    private Path getUploadPath() {
        Path path = Paths.get(uploadDir);
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + uploadDir, e);
        }
        return path;
    }

    public String getFileUrl(String filename) {
        if (filename == null) return null;
        return baseUrl + "/uploads/" + filename;
    }

    public String uploadBase64Image(String base64Image) throws IOException {
        if (base64Image == null || base64Image.isEmpty()) {
            throw new RuntimeException("Base64 image data is empty.");
        }

        base64Image = base64Image.trim();
        if (base64Image.startsWith("\"") && base64Image.endsWith("\"")) {
            base64Image = base64Image.substring(1, base64Image.length() - 1);
        }

        if (base64Image.contains(",")) {
            base64Image = base64Image.split(",")[1];
        }

        byte[] imageBytes = Base64.getMimeDecoder().decode(base64Image.trim());
        String filename = UUID.randomUUID() + ".png";
        Path destinationFile = getUploadPath().resolve(filename).normalize().toAbsolutePath();
        log.info("Supposed to upload image to {}", destinationFile);

        Path destinationFileWritten = Files.write(destinationFile, imageBytes);
        log.info("Uploaded image to: {}", destinationFileWritten);
        return filename;
    }

    public void delete(String filename) throws IOException {
        Path fileToDelete = getUploadPath().resolve(filename);

        if (Files.exists(fileToDelete) &&
                Files.isRegularFile(fileToDelete)) {

            Files.delete(fileToDelete);
        } else {
            log.error("Could not delete file: " + fileToDelete);
        }
    }
}
