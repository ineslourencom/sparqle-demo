package com.orderlink.dto;


public enum OrderState {
    PENDING("Order was created, pending processing"),
    INVENTORY_RESERVED("Order was processed by our system, sending instructions to shipment"),
    LOGISTICS_CONFIRMED("Order was shipped, waiting for delivery confirmation"),
    COMPLETED("Order was successfully delivered"),
    FAILED_INVENTORY("Order failed due to internal issues. Retrying"),
    FAILED_LOGISTICS("Order failed due to shipping issues. Retrying"),
    FAILED("Order failed due to unknown issues"),
    CANCELLED("Order was cancelled by merchant");

    public final String details;

    OrderState(String details) {
        this.details = details;
    }

    public boolean isCancelable() {
        return !(this == LOGISTICS_CONFIRMED || this == COMPLETED);
    }
}