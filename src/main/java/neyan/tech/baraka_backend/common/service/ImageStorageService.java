package neyan.tech.baraka_backend.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import neyan.tech.baraka_backend.common.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageStorageService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    @Value("${file.upload.dir:${user.home}/baraka/uploads}")
    private String uploadDir;

    /**
     * Store images for a basket
     * @param basketId The basket ID
     * @param files Array of image files to store
     * @return List of relative URLs for the stored images
     */
    public List<String> storeBasketImages(UUID basketId, MultipartFile[] files) {
        log.debug("storeBasketImages called - basketId: {}, files: {}", basketId, files != null ? files.length : 0);
        
        if (files == null || files.length == 0) {
            log.error("No files provided for basket: {}", basketId);
            throw new BadRequestException("No files provided");
        }

        List<String> imageUrls = new ArrayList<>();

        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            log.debug("Processing file {} of {} - name: {}, size: {}, contentType: {}", 
                i + 1, files.length, file.getOriginalFilename(), file.getSize(), file.getContentType());
            
            try {
                validateFile(file);
                log.debug("File {} validated successfully", i + 1);
            } catch (Exception ex) {
                log.error("File validation failed for file {} - name: {}, error: {}", 
                    i + 1, file.getOriginalFilename(), ex.getMessage(), ex);
                throw ex;
            }
            
            try {
                String imageUrl = storeBasketImage(basketId, file);
                imageUrls.add(imageUrl);
                log.debug("File {} stored successfully: {}", i + 1, imageUrl);
            } catch (Exception ex) {
                log.error("Failed to store file {} - name: {}, error: {}", 
                    i + 1, file.getOriginalFilename(), ex.getMessage(), ex);
                throw ex;
            }
        }

        log.info("Stored {} images for basket {}", imageUrls.size(), basketId);
        return imageUrls;
    }

    /**
     * Store a single image for a basket
     */
    private String storeBasketImage(UUID basketId, MultipartFile file) {
        try {
            log.debug("storeBasketImage - basketId: {}, uploadDir: {}, filename: {}", basketId, uploadDir, file.getOriginalFilename());
            
            // Create basket-specific directory
            Path basketDir = Paths.get(uploadDir, "baskets", basketId.toString());
            log.debug("Basket directory path: {}", basketDir);
            
            try {
                Files.createDirectories(basketDir);
                log.debug("Basket directory created/exists: {}", basketDir);
            } catch (Exception ex) {
                log.error("Failed to create basket directory: {}, error: {}", basketDir, ex.getMessage(), ex);
                throw new BadRequestException("Failed to create directory: " + ex.getMessage());
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String filename = UUID.randomUUID() + "." + extension;
            Path targetPath = basketDir.resolve(filename);
            log.debug("Target path: {}", targetPath);

            // Copy file
            try {
                Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                log.debug("File copied successfully to: {}", targetPath);
            } catch (IOException e) {
                log.error("Failed to copy file to: {}, error: {}", targetPath, e.getMessage(), e);
                throw new BadRequestException("Failed to copy file: " + e.getMessage());
            }

            // Return relative URL
            String imageUrl = String.format("/api/files/baskets/%s/%s", basketId, filename);
            log.debug("Stored image for basket {}: {}", basketId, imageUrl);

            return imageUrl;

        } catch (BadRequestException e) {
            log.error("BadRequestException in storeBasketImage for basket {}", basketId, e);
            throw e;
        } catch (IOException e) {
            log.error("IOException in storeBasketImage for basket {}", basketId, e);
            throw new BadRequestException("Failed to store image: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected exception in storeBasketImage for basket {}", basketId, e);
            throw new BadRequestException("Failed to store image: " + e.getMessage());
        }
    }

    /**
     * Delete an image file
     */
    public void deleteImage(String imageUrl) {
        try {
            // Extract path from URL: /api/files/baskets/{basketId}/{filename}
            String pathPart = imageUrl.replace("/api/files/", "");
            Path imagePath = Paths.get(uploadDir, pathPart);

            if (Files.exists(imagePath)) {
                Files.delete(imagePath);
                log.debug("Deleted image: {}", imagePath);
            } else {
                log.warn("Image file not found for deletion: {}", imagePath);
            }
        } catch (IOException e) {
            log.error("Failed to delete image: {}", imageUrl, e);
            // Don't throw exception, just log - file might already be deleted
        }
    }

    /**
     * Delete all images for a basket
     */
    public void deleteBasketImages(UUID basketId) {
        try {
            Path basketDir = Paths.get(uploadDir, "baskets", basketId.toString());
            if (Files.exists(basketDir)) {
                Files.walk(basketDir)
                        .sorted((a, b) -> -a.compareTo(b)) // Delete files before directories
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.warn("Failed to delete path: {}", path, e);
                            }
                        });
                log.debug("Deleted all images for basket {}", basketId);
            }
        } catch (IOException e) {
            log.error("Failed to delete basket images directory: {}", basketId, e);
            // Don't throw exception, just log
        }
    }

    /**
     * Validate file before storing
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File size exceeds maximum allowed size (5MB)");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Invalid file type. Allowed types: JPEG, PNG, WebP");
        }
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "jpg"; // Default extension
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    /**
     * Get the upload directory path
     */
    public Path getUploadDir() {
        return Paths.get(uploadDir);
    }
}
