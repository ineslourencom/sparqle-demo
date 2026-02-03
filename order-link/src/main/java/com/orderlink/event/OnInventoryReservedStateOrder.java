package com.orderlink.event;

import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.orderlink.config.LogisticsClientConfig;
import com.orderlink.dto.OrderRequest;
import com.orderlink.dto.OrderState;
import com.orderlink.logistic.client.LateLogisticsClient;
import com.orderlink.logistic.client.model.ShipmentRequest;
import com.orderlink.logistic.client.model.ShipmentResponse;
import com.orderlink.order.model.Order;

import feign.FeignException.FeignClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnInventoryReservedStateOrder implements OrderProcessor {

    private final ApplicationEventPublisher eventPublisher;
    private final LateLogisticsClient lateLogisticsClient;
    private final LogisticsClientConfig config;

    private static final int MAX_RETRIES = 3;

    @Async
    @EventListener
    public void processOrderEvent(OrderEvent event) {

        var state = event.order().getState();

        if (!(OrderState.INVENTORY_RESERVED.equals(state)
                || OrderState.FAILED_LOGISTICS.equals(state))) {
            return;
        }

        if (event.getRetryCount() >= MAX_RETRIES) {
            log.error("Max retries reached for order {}", event.merchantRef());
            return;
        }

        log.info("Processing order in {} state for merchantRef: {}, retry {}", event.order().getState(),
                event.merchantRef(), event.getRetryCount());

        Optional<ShipmentResponse> responseOpt;
        try {
            responseOpt = createShipment(event.order());
        } catch (FeignClientException e) {
            log.error("Error occurred while creating shipment for order {}: {}", event.merchantRef(), e.getMessage());
            publishOrderEvent(event, OrderState.FAILED_LOGISTICS, null);
            return;
        }

        if (responseOpt.isEmpty()) {
            log.error("Shipment creation returned empty for order {}", event.merchantRef());
            publishOrderEvent(event, OrderState.FAILED_LOGISTICS, null);
        } else {
            var response = responseOpt.get();
            publishOrderEvent(event, OrderState.LOGISTICS_CONFIRMED, response.trackingCode());
            log.info("Successfully sent order {} to third-party.", event.merchantRef());
        }
    }

    private void publishOrderEvent(OrderEvent event, OrderState state, String trackingCode) {
        var updatedOrder = Order.newInstance(
                event.order().getMerchantRef(),
                event.order().getOrderRequest(),
                state,
                trackingCode);
        eventPublisher.publishEvent(
        new OrderEvent(event.merchantRef(), updatedOrder, event.getRetryCount() + 1)
    );
    }

    public Optional<ShipmentResponse> createShipment(Order order) {

        ShipmentRequest shipmentRequest = fromOrder(order, config.getApiConfig().getWebhookUrl());

        return Optional.ofNullable(lateLogisticsClient.createShipment(config.getApiConfig().getApiKey(), shipmentRequest));

    }

    private static ShipmentRequest fromOrder(Order order, String webhookUrl) {
        var orderRequest = order.getOrderRequest();
        OrderRequest.Recipient recipient = orderRequest.recipient();
        var parcelRequest = orderRequest.parcel();

        var shipmentRecipient = ShipmentRequest.Recipient.builder()
                .name(recipient.name())
                .address1(recipient.address1())
                .address2(recipient.address2())
                .postalCode(recipient.postalCode())
                .city(recipient.city())
                .country(recipient.country())
                .build();

        var parcel = ShipmentRequest.Parcel.builder()
                .weightGrams(parcelRequest.weightGrams())
                .lengthCm(parcelRequest.lengthCm())
                .widthCm(parcelRequest.widthCm())
                .heightCm(parcelRequest.heightCm())
                .build();

        return ShipmentRequest.builder()
                .reference(order.getMerchantRef())
                .webhookUrl(webhookUrl)
                .barcode(order.getTrackingRef())
                .recipient(shipmentRecipient)
                .parcel(parcel)
                .build();
    }

}
