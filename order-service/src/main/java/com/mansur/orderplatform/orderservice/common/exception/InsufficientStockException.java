package com.mansur.orderplatform.orderservice.common.exception;

public class InsufficientStockException extends BusinessException {

    public InsufficientStockException(String productName, int available, int requested) {
        super("Not enough stock for '" + productName + "': available " + available + ", requested " + requested);
    }
}
