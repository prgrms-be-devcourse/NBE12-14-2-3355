package com.gamelog.nbe121423355.global.exceptionHandler;

import com.gamelog.nbe121423355.global.dto.RsData;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseBody
    public RsData<Void> noSuchElementException(){
        return new RsData<Void>(
                "404-1",
                "존재하지 않는 데이터입니다."
        );
    }

    //DTO 검증 실패(@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public RsData<Void> methodArgumentNotValidException(MethodArgumentNotValidException e){

        String message = e.getBindingResult()
                .getAllErrors()
                .stream()
                .filter(error -> error instanceof FieldError)
                .map(error -> (FieldError) error)
                .map(error -> error.getField() + "-" + error.getCode() + "-" + error.getDefaultMessage())
                .sorted(Comparator.comparing(String::toString))
                .collect(Collectors.joining("\n"));

        return new RsData<Void>(
                "400-1",
                message
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseBody
    public RsData<Void> handleException(HttpMessageNotReadableException e) {
        return new RsData<Void>(
                "400-2",
                "잘못된 형식의 요청 데이터입니다."
        );
    }

    // PathVariable/RequestParam 타입 불일치 (예: /games/abc)
    // ErrorResponse를 구현하지 않는 예외라 아래 Exception 핸들러의 4xx 분기에 걸리지 않아 별도 처리
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseBody
    public RsData<Void> handleException(MethodArgumentTypeMismatchException e) {
        return new RsData<Void>(
                "400-0",
                "잘못된 요청입니다."
        );
    }

    // 이미지 파일 크기 초과시 예외사항
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseBody
    public RsData<Void> handleException(MaxUploadSizeExceededException e) {
        return new RsData<Void>(
                "400-3",
                "파일 크기가 너무 큽니다. 5MB 이하 파일만 업로드할 수 있습니다."
        );
    }

    // 동시 요청으로 인한 DB 유니크 제약조건 위반 (중복확인 직후 동시 가입 경합)
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseBody
    public RsData<Void> handleException(DataIntegrityViolationException e) {
        log.warn("DB 제약조건 위반", e);
        return new RsData<Void>(
                "409-3",
                "이미 존재하거나 처리 중인 데이터입니다. 잠시 후 다시 시도해 주세요."
        );
    }

    @ExceptionHandler(ServiceException.class)
    @ResponseBody
    public RsData<Void> handleException(ServiceException e) {
        if(e.getResultCode().startsWith("500")) {
            log.error("서비스 오류 발생: resultCode={}", e.getResultCode(), e);
        }
        return new RsData<>(
                e.getResultCode(),
                e.getMsg()
        );
    }

    // 위에서 처리하지 못한 나머지 모든 예외 (500)
    // 단, Spring MVC가 던지는 클라이언트 오류(없는 경로, 타입 불일치, 지원하지 않는 메서드 등)는
    // 예외에 담긴 원래 4xx 상태로 응답하고 로그는 남기지 않음
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public RsData<Void> handleException(Exception e) {
        if (e instanceof ErrorResponse errorResponse && errorResponse.getStatusCode().is4xxClientError()) {
            int status = errorResponse.getStatusCode().value();
            return new RsData<Void>(
                    status + "-0",
                    clientErrorMessage(status)
            );
        }

        log.error("처리되지 않은 예외 발생", e);
        return new RsData<Void>(
                "500-1",
                "서버 내부 오류가 발생했습니다."
        );
    }

    private String clientErrorMessage(int status) {
        return switch (status) {
            case 404 -> "요청한 경로를 찾을 수 없습니다.";
            case 405 -> "허용되지 않은 요청 방식입니다.";
            default -> "잘못된 요청입니다.";
        };
    }
}
