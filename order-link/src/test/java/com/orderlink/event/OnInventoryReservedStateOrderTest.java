package com.orderlink.event;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;

import com.orderlink.config.LogisticsClientConfig;
import com.orderlink.dto.OrderState;
import com.orderlink.fixtures.TestFixtures;
import com.orderlink.logistic.client.LateLogisticsClient;
import com.orderlink.logistic.client.model.ShipmentRequest;
import com.orderlink.logistic.client.model.ShipmentResponse;
import com.orderlink.order.model.Order;

@ExtendWith(MockitoExtension.class)
class OnInventoryReservedStateOrderTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private LateLogisticsClient lateLogisticsClient;

    private OnInventoryReservedStateOrder processor;

    @Autowired
    private LogisticsClientConfig config;

    ArgumentCaptor<OrderEvent> eventCaptor = ArgumentCaptor.forClass(OrderEvent.class);

    @BeforeEach
    void setUp() {
        var apiConfig = new LogisticsClientConfig.ApiConfig();
        apiConfig.setBaseUrl("https://dummy-env.com");
        apiConfig.setWebhookUrl("https://localhost/logistics/webhook");
        apiConfig.setApiKey("api-key");

        config = new LogisticsClientConfig();
        config.setApiConfig(apiConfig);

        processor = new OnInventoryReservedStateOrder(eventPublisher, lateLogisticsClient, config);
    }

    @Test
    void processOrderEvent_whenStateInventoryReserved_shouldInvokeShipmentCreation() {
        //given
        var orderRequest = TestFixtures.sampleOrderRequest();
        var order = Order.newInstance(orderRequest.merchantRef(), 
            orderRequest, OrderState.INVENTORY_RESERVED, "TRACK-IR");

        when(lateLogisticsClient.createShipment(any(), any(ShipmentRequest.class)))
                .thenReturn(new ShipmentResponse("SHIP-1", "TRACKING", order.getTrackingRef()));


        //when
        processor.processOrderEvent(new OrderEvent(order.getMerchantRef(), order, 0));

        //then
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        var capturedOrder = eventCaptor.getValue().order();
        assertThat(capturedOrder.getMerchantRef()).isEqualTo(order.getMerchantRef());
        assertThat(capturedOrder.getState()).isEqualTo(OrderState.LOGISTICS_CONFIRMED);
        assertThat(capturedOrder.getTrackingRef()).isEqualTo("TRACKING");
        verify(lateLogisticsClient).createShipment(any(), any(ShipmentRequest.class));
    }

    @Test
    void processOrderEvent_whenStateFailedLogistics_shouldRetryShipment() {
        var orderRequest = TestFixtures.sampleOrderRequest();
        var order = Order.newInstance(orderRequest.merchantRef(), 
        orderRequest, OrderState.FAILED_LOGISTICS, "TRACK-IR");

        when(lateLogisticsClient.createShipment(any(), any(ShipmentRequest.class)))
                .thenReturn(new ShipmentResponse("SHIP-1", "TRACKING", order.getTrackingRef()));


        processor.processOrderEvent(new OrderEvent(order.getMerchantRef(), order, 0));

        verify(eventPublisher).publishEvent(eventCaptor.capture());
        var capturedOrder = eventCaptor.getValue().order();
        assertThat(capturedOrder.getMerchantRef()).isEqualTo(order.getMerchantRef());
        assertThat(capturedOrder.getState()).isEqualTo(OrderState.LOGISTICS_CONFIRMED);
        assertThat(capturedOrder.getTrackingRef()).isEqualTo("TRACKING");
        verify(lateLogisticsClient).createShipment(any(), any(ShipmentRequest.class));
    }

    @Test
    void processOrderEvent_whenStateNotSupported_shouldSkipShipment() {
        var orderRequest = TestFixtures.sampleOrderRequest();
        var order = Order.newInstance(orderRequest.merchantRef(), orderRequest, OrderState.PENDING, "TRACK-NOPE");
        assertThat(order.getState()).isEqualTo(OrderState.PENDING);


        processor.processOrderEvent(new OrderEvent(order.getMerchantRef(), order, 0));

        verify(eventPublisher, never()).publishEvent(any(OrderEvent.class));
        verify(lateLogisticsClient, never()).createShipment(any(), any(ShipmentRequest.class));
    }

    @Test
    void createShipment_shouldDelegateToClient() {
        var orderRequest = TestFixtures.sampleOrderRequest();
        var order = Order.newInstance(orderRequest.merchantRef(), orderRequest, OrderState.INVENTORY_RESERVED, "TRACK-DIRECT");

        when(lateLogisticsClient.createShipment(eq("api-key"), any(ShipmentRequest.class)))
                .thenReturn(new ShipmentResponse("SHIP-2", "TRACKING", order.getTrackingRef()));

        processor.createShipment(order);

        verify(lateLogisticsClient).createShipment(eq("api-key"), any(ShipmentRequest.class));
    }
}
