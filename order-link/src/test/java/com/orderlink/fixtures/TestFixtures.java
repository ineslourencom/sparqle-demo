package com.orderlink.fixtures;

import com.orderlink.dto.OrderRequest;
import com.orderlink.dto.OrderState;
import com.orderlink.logistic.webhook.WebhookPayload;
import com.orderlink.order.model.Order;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

public class TestFixtures {

 public static OrderRequest sampleOrderRequest() {
        return new OrderRequest(
            42L,
            "MERCHANT-" + UUID.randomUUID(),
            "TRACK123456",
            new OrderRequest.Recipient(
                "Jane Doe",
                "123 Any Street",
                "Unit 4",
                "90210",
                "Springfield",
                "US"
            ),
            new OrderRequest.Parcel(
                750L,
                BigDecimal.valueOf(12.5),
                BigDecimal.valueOf(8.4),
                BigDecimal.valueOf(3.2)
            )
        );
    }

    public static Order sampleOrder() {
        OrderRequest request = sampleOrderRequest();
        return Order.builder()
            .merchantRef(request.merchantRef())
            .trackingRef("TRACKING-" + UUID.randomUUID())
            .orderRequest(request)
            .lastUpdatedAt(ZonedDateTime.now())
            .state(OrderState.PENDING)
            .build();
    }

    public static WebhookPayload sampleWebhookPayload() {
        return new WebhookPayload(
            UUID.randomUUID().toString(),
            "SHIP-" + UUID.randomUUID(),
            "TRACK123456",
            "DELIVERED",
            OffsetDateTime.now(),
            new WebhookPayload.Location(40.7128, -74.0060),
            new WebhookPayload.Data("iVBORw0KGgoAAAANSUhEUgAAAAUA"),
            null
        );
    }
}