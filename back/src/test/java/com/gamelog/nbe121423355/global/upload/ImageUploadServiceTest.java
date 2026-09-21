package com.gamelog.nbe121423355.global.upload;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageUploadServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    private ImageUploadService imageUploadService;

    @BeforeEach
    void setUp() {
        imageUploadService = new ImageUploadService(cloudinary);
    }

    @Test
    @DisplayName("uploadImage - 빈 파일이면 Cloudinary 호출 없이 예외")
    void uploadImage_fail_emptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> imageUploadService.uploadImage(emptyFile))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getResultCode()).isEqualTo("400-4"));

        verifyNoInteractions(cloudinary);
    }

    @Test
    @DisplayName("uploadImage - 이미지가 아닌 파일이면 Cloudinary 호출 없이 예외")
    void uploadImage_fail_notImage() {
        MockMultipartFile textFile = new MockMultipartFile("file", "readme.txt", "text/plain", "hello".getBytes());

        assertThatThrownBy(() -> imageUploadService.uploadImage(textFile))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getResultCode()).isEqualTo("400-5"));

        verifyNoInteractions(cloudinary);
    }

    @Test
    @DisplayName("uploadImage - 성공하면 secure_url 반환")
    void uploadImage_success() throws IOException {
        MockMultipartFile imageFile = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "fake-image-bytes".getBytes());
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap()))
                .thenReturn(Map.of("secure_url", "https://res.cloudinary.com/t0pb5sje/image/upload/v1/abc123.jpg"));

        String url = imageUploadService.uploadImage(imageFile);

        assertThat(url).isEqualTo("https://res.cloudinary.com/t0pb5sje/image/upload/v1/abc123.jpg");
    }

    @Test
    @DisplayName("uploadImage - Cloudinary가 예외를 던지면 ServiceException으로 감쌈")
    void uploadImage_fail_cloudinaryError() throws IOException {
        MockMultipartFile imageFile = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "fake-image-bytes".getBytes());
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenThrow(new RuntimeException("Invalid Signature"));

        assertThatThrownBy(() -> imageUploadService.uploadImage(imageFile))
                .isInstanceOf(ServiceException.class)
                .satisfies(e -> assertThat(((ServiceException) e).getResultCode()).isEqualTo("500-2"));
    }

    @Test
    @DisplayName("deleteImage - 버전 세그먼트 있는 URL에서 public_id를 추출해 destroy 호출")
    void deleteImage_success_withVersion() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);

        imageUploadService.deleteImage("https://res.cloudinary.com/t0pb5sje/image/upload/v1789972470/nnw0f7uly9mohes2kwws.jpg");

        verify(uploader).destroy(eq("nnw0f7uly9mohes2kwws"), anyMap());
    }

    @Test
    @DisplayName("deleteImage - 버전 세그먼트 없는 URL도 처리")
    void deleteImage_success_withoutVersion() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);

        imageUploadService.deleteImage("https://res.cloudinary.com/t0pb5sje/image/upload/nnw0f7uly9mohes2kwws.jpg");

        verify(uploader).destroy(eq("nnw0f7uly9mohes2kwws"), anyMap());
    }

    @Test
    @DisplayName("deleteImage - null이면 Cloudinary 호출 안 함")
    void deleteImage_null_doesNothing() {
        imageUploadService.deleteImage(null);

        verifyNoInteractions(cloudinary);
    }

    @Test
    @DisplayName("deleteImage - Cloudinary URL 형식이 아니면 호출 안 함")
    void deleteImage_malformedUrl_doesNothing() {
        imageUploadService.deleteImage("not-a-cloudinary-url");

        verifyNoInteractions(cloudinary);
    }

    @Test
    @DisplayName("deleteImage - destroy가 예외를 던져도 밖으로 전파되지 않음")
    void deleteImage_swallowsException() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(anyString(), anyMap())).thenThrow(new RuntimeException("network error"));

        imageUploadService.deleteImage("https://res.cloudinary.com/t0pb5sje/image/upload/v1/abc123.jpg");
    }
}
