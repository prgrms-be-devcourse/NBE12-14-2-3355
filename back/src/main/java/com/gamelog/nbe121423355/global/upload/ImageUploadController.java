package com.gamelog.nbe121423355.global.upload;

import com.gamelog.nbe121423355.global.dto.RsData;
import com.gamelog.nbe121423355.global.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class ImageUploadController {

    private final ImageUploadService imageUploadService;

    @PostMapping("/images")
    public RsData<String> uploadImage(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal SecurityUser user
    ) {
        String url = imageUploadService.uploadImage(file);

        return new RsData<>(
                "200-1",
                "이미지를 업로드했습니다.",
                url
        );
    }
}
