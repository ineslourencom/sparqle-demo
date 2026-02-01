package com.orderlink.order.model;

import java.time.ZonedDateTime;

import com.orderlink.dto.OrderRequest;
import com.orderlink.dto.OrderState;

import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class Order {

    private final String merchantRef;
    private final String trackingRef;
    @Builder.Default
    private final OrderState state = OrderState.PENDING;
    private final OrderRequest orderRequest;
    private final ZonedDateTime lastUpdatedAt;
    
    public static Order newInstance(String merchantRef, OrderRequest orderRequest, OrderState state) {
        return Order.builder()
                .merchantRef(merchantRef)
                .orderRequest(orderRequest)
                .state(state)
                .lastUpdatedAt(ZonedDateTime.now())
                .build();
    }

    public static Order newInstance(String merchantRef, OrderRequest orderRequest, OrderState state, String trackingRef) {
        return Order.builder()
                .merchantRef(merchantRef)
                .orderRequest(orderRequest)
                .trackingRef(trackingRef)
                .state(state)
                .lastUpdatedAt(ZonedDateTime.now())
                .build();
    }

    boolean isCancelable() {
        return !(this.state == OrderState.LOGISTICS_CONFIRMED || this.state == OrderState.COMPLETED);
    }
}
