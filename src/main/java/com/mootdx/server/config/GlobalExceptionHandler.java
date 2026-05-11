package com.mootdx.server.config;

import com.mootdx.server.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 功能：统一处理应用中的异常，返回标准化的错误响应
 *
 * @author MooTDX Java Team
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数校验异常（@Valid）
     *
     * @param ex 校验异常
     * @return API响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("[参数校验失败] {}", message);
        return ApiResponse.error(400, message);
    }

    /**
     * 处理参数绑定异常
     *
     * @param ex 绑定异常
     * @return API响应
     */
    @ExceptionHandler(BindException.class)
    public ApiResponse<Void> handleBindException(BindException ex) {
        String message = ex.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("[参数绑定失败] {}", message);
        return ApiResponse.error(400, message);
    }

    /**
     * 处理约束校验异常（@Validated）
     *
     * @param ex 约束校验异常
     * @return API响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraintViolationException(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.warn("[约束校验失败] {}", message);
        return ApiResponse.error(400, message);
    }

    /**
     * 处理运行时异常
     *
     * @param ex 运行时异常
     * @return API响应
     */
    @ExceptionHandler(RuntimeException.class)
    public ApiResponse<Void> handleRuntimeException(RuntimeException ex) {
        log.error("[运行时异常] {}", ex.getMessage(), ex);
        return ApiResponse.error(500, ex.getMessage());
    }

    /**
     * 处理所有其他异常
     *
     * @param ex 异常
     * @return API响应
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception ex) {
        log.error("[系统异常] {}", ex.getMessage(), ex);
        return ApiResponse.error(500, "系统内部错误");
    }
}
