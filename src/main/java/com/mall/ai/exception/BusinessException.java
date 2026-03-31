package com.mall.ai.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 业务异常基类。
 * 
 * <p>封装业务逻辑中的可预期异常，提供统一的错误码和 HTTP 状态码。
 * 配合 {@link GlobalExceptionHandler} 实现标准化的异常响应。</p>
 * 
 * <p>使用示例：</p>
 * <pre>{@code
 * if (user == null) {
 *     throw new BusinessException("USER_NOT_FOUND", "用户不存在", HttpStatus.NOT_FOUND);
 * }
 * }</pre>
 * 
 * @author onlineShopAI Team
 * @version 1.0.0
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 业务错误码。
     */
    private final String errorCode;

    /**
     * HTTP 状态码。
     */
    private final HttpStatus httpStatus;

    /**
     * 构造业务异常。
     *
     * @param errorCode   业务错误码
     * @param message     错误信息
     * @param httpStatus  HTTP 状态码
     */
    public BusinessException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    /**
     * 构造业务异常（默认 400 状态码）。
     *
     * @param errorCode 业务错误码
     * @param message   错误信息
     */
    public BusinessException(String errorCode, String message) {
        this(errorCode, message, HttpStatus.BAD_REQUEST);
    }

    /**
     * 构造业务异常（带原因）。
     *
     * @param errorCode   业务错误码
     * @param message     错误信息
     * @param httpStatus  HTTP 状态码
     * @param cause       原始异常
     */
    public BusinessException(String errorCode, String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
