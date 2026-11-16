package com.mansur.orderplatform.orderservice.common.exception;

/**
 * A request that is valid in form but breaks a business rule (answered as 409 Conflict).
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
