package com.orderlink.dto;

import java.time.ZonedDateTime;

import lombok.Builder;

@Builder
public record OrderResponse(
    String merchantRef,
    String trackingRef,
    OrderState state,
    String details,
    ZonedDateTime updatedAt
){}
