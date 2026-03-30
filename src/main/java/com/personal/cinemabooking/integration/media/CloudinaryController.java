package com.personal.cinemabooking.integration.media;

import com.personal.cinemabooking.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for Cloudinary image operations.
 * Handles upload, delete, and list operations for images stored in Cloudinary.
 */
@RestController
@RequestMapping("/api/v1/media/cloudinary")
@Slf4j
public class CloudinaryController {

    private final CloudinaryService cloudinaryService;

    @Autowired
    public CloudinaryController(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }

    /**
     * Upload an image to Cloudinary.
     *
     * @param file the image file to upload
     * @return response with upload result containing url, public_id, etc.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Uploading image: {}", file.getOriginalFilename());

            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "image.upload.empty", null));
            }

            Map<String, Object> result = cloudinaryService.uploadImage(file);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(true, "image.upload.success", result));

        } catch (IOException e) {
            log.error("Error uploading image: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "image.upload.error", e.getMessage()));
        }
    }

    /**
     * Delete an image from Cloudinary by public ID.
     *
     * @param publicId the public ID of the image to delete
     * @return response with delete status
     */
    @DeleteMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> deleteImage(@RequestParam("publicId") String publicId) {
        try {
            log.info("Deleting image with publicId: {}", publicId);

            cloudinaryService.deleteImage(publicId);

            Map<String, String> result = new HashMap<>();
            result.put("publicId", publicId);
            result.put("status", "deleted");

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(true, "image.delete.success", result));

        } catch (IOException e) {
            log.error("Error deleting image: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "image.delete.error", e.getMessage()));
        }
    }

    /**
     * List all images in the Cloudinary folder.
     *
     * @return list of images with their details
     */
    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> listImages() {
        try {
            log.info("Listing all images from Cloudinary");

            List<Map<String, Object>> images = cloudinaryService.listImages();

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(true, "image.list.success", images));

        } catch (Exception e) {
            log.error("Error listing images: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "image.list.error", e.getMessage()));
        }
    }
}
