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
        if (files == null || files.length == 0) {
            throw new BadRequestException("No files provided");
        }

        List<String> imageUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            validateFile(file);
            String imageUrl = storeBasketImage(basketId, file);
            imageUrls.add(imageUrl);
        }

        log.info("Stored {} images for basket {}", imageUrls.size(), basketId);
        return imageUrls;
    }

    /**
     * Store a single image for a basket
     */
    private String storeBasketImage(UUID basketId, MultipartFile file) {
        try {
            // Create basket-specific directory
            Path basketDir = Paths.get(uploadDir, "baskets", basketId.toString());
            Files.createDirectories(basketDir);

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String filename = UUID.randomUUID() + "." + extension;
            Path targetPath = basketDir.resolve(filename);

            // Copy file
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // Return relative URL
            String imageUrl = String.format("/api/files/baskets/%s/%s", basketId, filename);
            log.debug("Stored image for basket {}: {}", basketId, imageUrl);

            return imageUrl;

        } catch (IOException e) {
            log.error("Failed to store image for basket {}", basketId, e);
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
