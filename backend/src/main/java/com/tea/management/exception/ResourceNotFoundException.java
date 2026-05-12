package com.tea.management.exception;
/**
 * ResourceNotFoundException 异常相关类，用来统一表达业务错误或系统错误。
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
