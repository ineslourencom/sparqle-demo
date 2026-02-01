package com.orderlink.event;

import com.orderlink.order.model.Order;

public record OrderEvent(String merchantRef, Order order) {
}
