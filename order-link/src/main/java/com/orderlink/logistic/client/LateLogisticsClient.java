package com.orderlink.logistic.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.orderlink.config.LateLogisticsFeignConfig;
import com.orderlink.logistic.client.model.LabelResponse;
import com.orderlink.logistic.client.model.ShipmentRequest;
import com.orderlink.logistic.client.model.ShipmentResponse;

@FeignClient(
    name = "lateLogisticsClient",
    configuration = LateLogisticsFeignConfig.class
)
public interface LateLogisticsClient {

    @PostMapping("/v1/shipments")
    ShipmentResponse createShipment(
        @RequestHeader("X-API-Key") String apiKey,
        @RequestBody ShipmentRequest shipmentRequest
    );

    @GetMapping("/v1/shipments/{shipment_id}/label")
    LabelResponse getLabel(
        @RequestHeader("X-API-Key") String apiKey,
        @PathVariable("shipment_id") String shipmentId
    );
}