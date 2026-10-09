package com.personal.cinemabooking.integration.media;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class S3StorageService {

    private final S3Client s3Client;

    @Value("${s3-client.bucket}")
    private String bucketName;

    @Value("${s3-client.endpoint}")
    private String endpoint;

    @Value("${s3-client.public-url}")
    private String publicUrl;

    public Map<String, Object> uploadImage(MultipartFile file) {
        try {
            // Check if bucket exists
            try {
                s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            } catch (S3Exception e) {
                if (e instanceof NoSuchBucketException || e.getMessage().contains("Not Found")) {
                    s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
                } else {
                    throw e;
                }
            }

            String filename = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
            
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filename)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String secureUrl = String.format("%s/%s", publicUrl, filename);
            
            Map<String, Object> result = new HashMap<>();
            result.put("secure_url", secureUrl);
            result.put("public_id", filename);
            return result;
        } catch (IOException e) {
            log.error("Error reading file during upload to S3/MinIO", e);
            throw new RuntimeException("Error reading file: " + e.getMessage());
        } catch (S3Exception e) {
            log.error("Error uploading to S3/MinIO", e);
            throw new RuntimeException("Error uploading to S3/MinIO: " + e.getMessage());
        }
    }

    public void deleteImage(String publicId) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(publicId)
                    .build();
            s3Client.deleteObject(request);
        } catch (S3Exception e) {
            log.error("Error deleting from S3/MinIO", e);
            throw new RuntimeException("Error deleting from S3/MinIO: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> listImages() {
        List<Map<String, Object>> images = new ArrayList<>();
        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .build();
            
            ListObjectsV2Response response = s3Client.listObjectsV2(request);
            for (S3Object s3Object : response.contents()) {
                Map<String, Object> map = new HashMap<>();
                map.put("public_id", s3Object.key());
                map.put("secure_url", String.format("%s/%s", publicUrl, s3Object.key()));
                images.add(map);
            }
        } catch (S3Exception e) {
            log.error("Error listing images from S3/MinIO", e);
        }
        return images;
    }
}
