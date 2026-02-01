package com.orderlink.logistic.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record WebhookPayload(
    @JsonProperty("event_id") String eventId,
    @JsonProperty("shipment_id") String shipmentId,
    String barcode,
    String status,
    @JsonProperty("occurred_at") OffsetDateTime occurredAt,
    Location location,
    Data data,
    String failedReason
) {
    public record Location(
        double latitude,
        double longitude
    ) {}

    public record Data(
        @JsonProperty("signature_png_base64") String signaturePngBase64
    ) {}
}
