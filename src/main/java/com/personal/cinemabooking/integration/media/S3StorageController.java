package com.personal.cinemabooking.integration.media;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/media/s3")
@RequiredArgsConstructor
@Slf4j
public class S3StorageController {

    private final S3StorageService s3StorageService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result = s3StorageService.uploadImage(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to upload image", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<Void> deleteImage(@PathVariable String publicId) {
        try {
            s3StorageService.deleteImage(publicId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to delete image", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listImages() {
        return ResponseEntity.ok(s3StorageService.listImages());
    }
}
