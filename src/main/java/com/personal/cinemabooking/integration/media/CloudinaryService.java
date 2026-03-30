package com.personal.cinemabooking.integration.media;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for uploading, deleting and listing images on Cloudinary.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    @Value("${cloudinary.folder:store-api}")
    private String folder;

    /**
     * Upload an image to Cloudinary.
     *
     * @param file the image file to upload
     * @return upload result map containing url, public_id, etc.
     * @throws IOException if upload fails
     */
    public Map<String, Object> uploadImage(MultipartFile file) throws IOException {
        log.info("Uploading image: {} to Cloudinary", file.getOriginalFilename());

        Map<String, Object> uploadParams = ObjectUtils.asMap(
                "folder", folder,
                "resource_type", "image"
        );

        return cloudinary.uploader().upload(file.getBytes(), uploadParams);
    }

    /**
     * Delete an image from Cloudinary by public ID.
     *
     * @param publicId the public ID of the image to delete
     * @throws IOException if delete fails
     */
    public void deleteImage(String publicId) throws IOException {
        log.info("Deleting image: {} from Cloudinary", publicId);

        Map<String, Object> deleteResult = cloudinary.uploader()
                .destroy(publicId, ObjectUtils.asMap("resource_type", "image"));

        if (!"ok".equals(deleteResult.get("result"))) {
            log.error("Failed to delete image: {}. Result: {}", publicId, deleteResult.get("result"));
            throw new IOException("Failed to delete image: " + publicId);
        }

        log.info("Successfully deleted image: {}", publicId);
    }

    /**
     * List all images in the Cloudinary folder.
     *
     * @return list of maps containing image details (url, public_id, etc.)
     * @throws Exception if listing fails
     */
    public List<Map<String, Object>> listImages() throws Exception {
        log.info("Listing images from Cloudinary folder: {}", folder);

        Map<String, Object> params = ObjectUtils.asMap(
                "prefix", folder + "/",
                "resource_type", "image",
                "max_results", 500
        );

        Map<String, Object> resources = cloudinary.api().resources(params);
        List<Map<String, Object>> images = new ArrayList<>();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> resourcesList = (List<Map<String, Object>>) resources.get("resources");

        if (resourcesList != null) {
            for (Map<String, Object> resource : resourcesList) {
                Map<String, Object> imageInfo = new HashMap<>();
                imageInfo.put("publicId", resource.get("public_id"));
                imageInfo.put("url", resource.get("secure_url"));
                imageInfo.put("format", resource.get("format"));
                imageInfo.put("width", resource.get("width"));
                imageInfo.put("height", resource.get("height"));
                imageInfo.put("bytes", resource.get("bytes"));
                imageInfo.put("createdAt", resource.get("created_at"));
                images.add(imageInfo);
            }
        }

        log.info("Found {} images in Cloudinary", images.size());
        return images;
    }
}
