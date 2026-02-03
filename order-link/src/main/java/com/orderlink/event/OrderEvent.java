package com.orderlink.event;

import com.orderlink.order.model.Order;

public record OrderEvent(String merchantRef, Order order, int retryCount) {


public OrderEvent(String merchantRef, Order order, int retryCount) {
    this.merchantRef = merchantRef;
    this.order = order;
    this.retryCount = retryCount;
}

public int getRetryCount() {
    return retryCount;
}
}
