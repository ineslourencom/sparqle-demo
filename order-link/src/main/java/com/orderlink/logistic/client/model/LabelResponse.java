package com.orderlink.logistic.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LabelResponse(
    @JsonProperty("shipment_id") String shipmentId,
    @JsonProperty("label_pdf_base64") String labelPdfBase64,
    @JsonProperty("content_type") String contentType
) {}