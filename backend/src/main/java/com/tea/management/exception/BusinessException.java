package com.tea.management.exception;
/**
 * BusinessException 异常相关类，用来统一表达业务错误或系统错误。
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
