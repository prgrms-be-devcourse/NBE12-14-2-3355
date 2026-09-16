package com.gamelog.nbe121423355.global.exceptionHandler;

import com.gamelog.nbe121423355.global.dto.RsData;
import com.gamelog.nbe121423355.global.exception.ServiceException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

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
        return new RsData<Void>(
                "500-1",
                "서버 내부 오류가 발생했습니다."
        );
    }
}
