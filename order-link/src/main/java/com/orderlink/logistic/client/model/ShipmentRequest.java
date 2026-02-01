package com.orderlink.logistic.client.model;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;

@Builder
public record ShipmentRequest(
    String reference,
    @JsonProperty("webhook_url") String webhookUrl,
    String barcode,
    Recipient recipient,
    Parcel parcel
) {
    @Builder
    public record Recipient(
        String name,
        String address1,
        String address2,
        @JsonProperty("postal_code") String postalCode,
        String city,
        String country
    ) {}

    @Builder
    public record Parcel(
        @JsonProperty("weight_grams") long weightGrams,
        @JsonProperty("length_cm") BigDecimal lengthCm,
        @JsonProperty("width_cm") BigDecimal widthCm,
        @JsonProperty("height_cm") BigDecimal heightCm
    ) {}
}