package com.mall.ai.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 * 
 * <p>统一处理应用中的各类异常，返回标准化的错误响应。
 * 确保异常信息不泄露敏感数据，同时提供足够的调试信息。</p>
 * 
 * <p>处理的异常类型：</p>
 * <ul>
 *   <li>参数校验异常：{@link MethodArgumentNotValidException}</li>
 *   <li>业务异常：{@link BusinessException}</li>
 *   <li>AI 服务异常：超时、限流、网络错误等</li>
 *   <li>未知异常：兜底处理</li>
 * </ul>
 * 
 * @author onlineShopAI Team
 * @version 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数校验异常。
     *
     * @param ex 方法参数校验异常
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex) {
        
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        
        log.warn("Validation error: {}", errorMessage);
        
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", errorMessage);
    }

    /**
     * 处理业务异常。
     *
     * @param ex 业务异常
     * @return 错误响应
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex) {
        log.warn("Business exception: code={}, message={}", ex.getErrorCode(), ex.getMessage());
        
        return buildErrorResponse(ex.getHttpStatus(), ex.getErrorCode(), ex.getMessage());
    }

    /**
     * 处理 AI 服务限流异常。
     *
     * @param ex 限流异常
     * @return 错误响应
     */
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitException(RateLimitException ex) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());
        
        return buildErrorResponse(
                HttpStatus.TOO_MANY_REQUESTS, 
                "RATE_LIMIT_EXCEEDED", 
                "AI 服务繁忙，请稍后重试"
        );
    }

    /**
     * 处理 AI 服务超时异常。
     *
     * @param ex 超时异常
     * @return 错误响应
     */
    @ExceptionHandler(AiTimeoutException.class)
    public ResponseEntity<Map<String, Object>> handleAiTimeoutException(AiTimeoutException ex) {
        log.error("AI service timeout: {}", ex.getMessage());
        
        return buildErrorResponse(
                HttpStatus.GATEWAY_TIMEOUT, 
                "AI_TIMEOUT", 
                "AI 服务响应超时，请稍后重试"
        );
    }

    /**
     * 处理非法参数异常。
     *
     * @param ex 非法参数异常
     * @return 错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", ex.getMessage());
    }

    /**
     * 兜底异常处理。
     *
     * @param ex 未知异常
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR, 
                "INTERNAL_ERROR", 
                "服务内部错误，请稍后重试"
        );
    }

    /**
     * 构建标准错误响应。
     *
     * @param status        HTTP 状态码
     * @param errorCode     业务错误码
     * @param errorMessage  错误信息
     * @return 错误响应实体
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status, String errorCode, String errorMessage) {
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toEpochMilli());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("errorCode", errorCode);
        body.put("message", errorMessage);
        
        return ResponseEntity.status(status).body(body);
    }
}
