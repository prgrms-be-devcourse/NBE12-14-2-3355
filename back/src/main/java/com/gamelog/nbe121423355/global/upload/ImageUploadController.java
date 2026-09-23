package com.gamelog.nbe121423355.global.upload;

import com.gamelog.nbe121423355.global.dto.RsData;
import com.gamelog.nbe121423355.global.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "image-upload-controller", description = "이미지 업로드 API")
public class ImageUploadController {

    private final ImageUploadService imageUploadService;

    @PostMapping("/images")
    @Operation(summary = "이미지 업로드", description = "이미지 파일을 업로드하고 접근 가능한 URL을 반환합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    public RsData<String> uploadImage(
            @Parameter(description = "업로드할 이미지 파일", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(hidden = true)
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
