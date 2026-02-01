package com.orderlink.logistic.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ShipmentResponse(
    @JsonProperty("shipment_id") String shipmentId,
    @JsonProperty("tracking_code") String trackingCode,
    String barcode
) {}
