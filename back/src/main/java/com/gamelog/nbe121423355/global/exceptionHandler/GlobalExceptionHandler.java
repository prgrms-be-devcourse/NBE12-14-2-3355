package com.gamelog.nbe121423355.global.exceptionHandler;

import com.gamelog.nbe121423355.global.dto.RsData;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
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

    // 이미지 파일 크기 초과시 예외사항
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseBody
    public RsData<Void> handleException(MaxUploadSizeExceededException e) {
        return new RsData<Void>(
                "400-3",
                "파일 크기가 너무 큽니다. 5MB 이하 파일만 업로드할 수 있습니다."
        );
    }

    @ExceptionHandler(ServiceException.class)
    @ResponseBody
    public RsData<Void> handleException(ServiceException e) {
        return new RsData<>(
                e.getResultCode(),
                e.getMsg()
        );
    }

    // 위에서 처리하지 못한 나머지 모든 예외 (500)
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public RsData<Void> handleException(Exception e) {
        log.error("처리되지 않은 예외 발생", e);
        return new RsData<Void>(
                "500-1",
                "서버 내부 오류가 발생했습니다."
        );
    }
}
