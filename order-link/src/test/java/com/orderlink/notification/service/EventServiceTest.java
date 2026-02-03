package com.orderlink.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.orderlink.dto.OrderResponse;
import com.orderlink.dto.OrderState;
import com.orderlink.event.OrderEvent;
import com.orderlink.fixtures.TestFixtures;
import com.orderlink.order.model.Order;

import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class EventServiceTest {

    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventService();
    }

    @Test
    void processOrderEvent_shouldEmitOrderUpdate() throws Exception {
        var emitter = new RecordingEmitter();
        eventService.addEmitter(emitter);

        var orderRequest = TestFixtures.sampleOrderRequest();
        var now = ZonedDateTime.now();
        var order = Order.builder()
                .merchantRef(orderRequest.merchantRef())
                .orderRequest(orderRequest)
                .trackingRef("TRACK-EMIT")
                .state(OrderState.COMPLETED)
                .lastUpdatedAt(now)
                .build();

        var event = new OrderEvent(order.getMerchantRef(), order, 0);

        eventService.processOrderEvent(event);

        assertThat(emitter.lastEvents).isNotNull();
        var response = extractResponse(emitter.lastEvents);
        assertThat(response.merchantRef()).isEqualTo(order.getMerchantRef());
        assertThat(response.state()).isEqualTo(order.getState());
        assertThat(response.updatedAt()).isEqualTo(order.getLastUpdatedAt());
    }

    private OrderResponse extractResponse(Set<ResponseBodyEmitter.DataWithMediaType> events) {
        return events.stream()
                .map(ResponseBodyEmitter.DataWithMediaType::getData)
                .filter(OrderResponse.class::isInstance)
                .map(OrderResponse.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("OrderResponse payload not emitted"));
    }

    private static class RecordingEmitter extends SseEmitter {
        private Set<ResponseBodyEmitter.DataWithMediaType> lastEvents;

        RecordingEmitter() {
            super(0L);
        }

        @Override
        public synchronized void send(SseEventBuilder builder) throws IOException {
            this.lastEvents = builder.build();
        }
    }
}
