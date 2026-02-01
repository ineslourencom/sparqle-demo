package com.orderlink.logistic.webhook;

import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.orderlink.dto.OrderState;
import com.orderlink.event.OrderEvent;
import com.orderlink.order.model.Order;
import com.orderlink.order.service.InventoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class WebhookService {

    private final ApplicationEventPublisher eventPublisher; 
    private final InventoryService inventoryService;

    public void handleLateLogisticsWebhook(WebhookPayload payload) {
        log.info("Handling late logistics webhook eventId={} shipmentId={} barcode={}",
                payload.eventId(), payload.shipmentId(), payload.barcode());

        createOrderEventFromPayload(payload)
            .ifPresentOrElse(eventPublisher::publishEvent, () ->
                log.warn("No inventory entry found for barcode {} while handling event {}", payload.barcode(), payload.eventId())
            );
    }

    private Optional<OrderEvent> createOrderEventFromPayload(WebhookPayload payload) {
        var orderState = mapStatusToOrderState(payload.status());
        if (orderState == null) {
            log.warn("Ignoring webhook event {} with unsupported status '{}'", payload.eventId(), payload.status());
            return Optional.empty();
        }

        var barcode = payload.barcode();
        if (barcode == null || barcode.isBlank()) {
            log.warn("Ignoring webhook event {} due to missing barcode", payload.eventId());
            return Optional.empty();
        }

        return inventoryService.doFindOrderByTrackingRef(barcode)
                .map(entry -> {
                    var orderRequestDto = entry.getOrderRequest().toDto();
                    var merchantRef = orderRequestDto.merchantRef();
                    var lastUpdatedAt = payload.occurredAt() != null
                            ? payload.occurredAt().toZonedDateTime()
                            : ZonedDateTime.now();

                    var trackingRef = entry.getTrackingRef() != null ? entry.getTrackingRef() : barcode;

                    var order = Order.builder()
                            .merchantRef(merchantRef)
                            .trackingRef(trackingRef)
                            .orderRequest(orderRequestDto)
                            .state(orderState)
                            .lastUpdatedAt(lastUpdatedAt)
                            .build();

                    return new OrderEvent(merchantRef, order);
                });
    }

    private OrderState mapStatusToOrderState(String status) {
        if (status == null) {
            return null;
        }

        return switch (status.toUpperCase(Locale.ROOT)) {
            case "DELIVERED" -> OrderState.COMPLETED;
            case "IN_TRANSIT", "OUT_FOR_DELIVERY", "SHIPPED" -> OrderState.LOGISTICS_CONFIRMED;
            case "FAILED", "DELIVERY_FAILED", "CANCELLED" -> OrderState.FAILED;
            default -> null;
        };
    }

}
