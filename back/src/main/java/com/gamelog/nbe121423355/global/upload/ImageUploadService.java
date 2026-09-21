package com.gamelog.nbe121423355.global.upload;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
public class ImageUploadService {

    private final Cloudinary cloudinary;

    public ImageUploadService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    // 이미지 업로드메소드
    public String uploadImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ServiceException("400-4", "업로드할 파일이 없습니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ServiceException("400-5", "이미지 파일만 업로드할 수 있습니다.");
        }

        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
            return (String) result.get("secure_url");
        } catch (IOException | RuntimeException e) {
            throw new ServiceException("500-2", "이미지 업로드에 실패했습니다.");
        }
    }

    // 이미지 삭제 메소드
    public void deleteImage(String imageUrl) {
        String publicId = extractPublicId(imageUrl);
        if (publicId == null) {
            return;
        }

        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException | RuntimeException e) {
            log.warn("이미지 삭제 실패: {}", imageUrl, e);
        }
    }

    private String extractPublicId(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }

        int uploadIndex = imageUrl.indexOf("/upload/");
        if (uploadIndex == -1) {
            return null;
        }

        String afterUpload = imageUrl.substring(uploadIndex + "/upload/".length());
        String[] parts = afterUpload.split("/", 2);
        String rest = (parts.length > 1 && parts[0].matches("v\\d+")) ? parts[1] : afterUpload;

        int dotIndex = rest.lastIndexOf(".");
        return dotIndex == -1 ? rest : rest.substring(0, dotIndex);
    }
}
