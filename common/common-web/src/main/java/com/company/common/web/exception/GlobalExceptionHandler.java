package com.company.common.web.exception;

import com.company.common.core.enums.CommonErrorCode;
import com.company.common.core.exception.BaseException;
import com.company.common.web.filter.TraceIdFilter;
import com.company.common.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException exception,
                                                                 HttpServletRequest request) {
        log.warn("Business exception happened, code={}, message={}",
            exception.getErrorCode().code(), exception.getMessage());
        return ResponseEntity.status(exception.getHttpStatus())
            .body(ApiResponse.failure(
                exception.getErrorCode().code(),
                exception.getMessage(),
                traceId(request)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                                          HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest()
            .body(ApiResponse.failure(CommonErrorCode.VALIDATION_ERROR.code(), message, traceId(request)));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception,
                                                                       HttpServletRequest request) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.failure(
                CommonErrorCode.VALIDATION_ERROR.code(),
                exception.getMessage(),
                traceId(request)));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
                                                                          HttpServletRequest request) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.failure(
                CommonErrorCode.VALIDATION_ERROR.code(),
                "Request body is required and must be valid JSON",
                traceId(request)));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException exception,
                                                                   HttpServletRequest request) {
        return ResponseEntity.status(404)
            .body(ApiResponse.failure(
                CommonErrorCode.RESOURCE_NOT_FOUND.code(),
                CommonErrorCode.RESOURCE_NOT_FOUND.message(),
                traceId(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception,
                                                                       HttpServletRequest request) {
        log.error("Unexpected exception happened", exception);
        return ResponseEntity.internalServerError()
            .body(ApiResponse.failure(
                CommonErrorCode.INTERNAL_ERROR.code(),
                CommonErrorCode.INTERNAL_ERROR.message(),
                traceId(request)));
    }

    private String traceId(HttpServletRequest request) {
        Object traceId = request.getAttribute(TraceIdFilter.TRACE_ID);
        return traceId == null ? "" : traceId.toString();
    }
}
