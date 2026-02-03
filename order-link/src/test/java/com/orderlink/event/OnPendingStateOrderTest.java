package com.orderlink.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.orderlink.dto.OrderState;
import com.orderlink.fixtures.TestFixtures;
import com.orderlink.order.entity.InventoryEntry;
import com.orderlink.order.model.Order;
import com.orderlink.order.service.InventoryService;

@ExtendWith(MockitoExtension.class)
class OnPendingStateOrderTest {

    @Mock
    private InventoryService inventoryService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private OnPendingStateOrder processor;

    @BeforeEach
    void setUp() {
        processor = new OnPendingStateOrder(inventoryService, eventPublisher);
    }

    @Test
    void processOrderEvent_whenStateIsNotPending_shouldIgnore() {
        var orderRequest = TestFixtures.sampleOrderRequest();
        var order = Order.builder()
                .merchantRef(orderRequest.merchantRef())
                .orderRequest(orderRequest)
                .trackingRef("TRACK-001")
                .state(OrderState.LOGISTICS_CONFIRMED)
                .lastUpdatedAt(ZonedDateTime.now())
                .build();

        processor.processOrderEvent(new OrderEvent(order.getMerchantRef(), order, 0));

        verifyNoInteractions(inventoryService);
        verify(eventPublisher, never()).publishEvent(any(OrderEvent.class));
    }

    @Test
    void processOrderEvent_whenStateIsPending_shouldReserveInventoryAndPublish() {
        var orderRequest = TestFixtures.sampleOrderRequest();
        var order = Order.builder()
                .merchantRef(orderRequest.merchantRef())
                .orderRequest(orderRequest)
                .trackingRef("TRACK-002")
                .state(OrderState.PENDING)
                .lastUpdatedAt(ZonedDateTime.now())
                .build();

        when(inventoryService.doFindOrderByMerchantRef(order.getMerchantRef())).thenReturn(Optional.empty());
        when(inventoryService.save(any(InventoryEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        processor.processOrderEvent(new OrderEvent(order.getMerchantRef(), order, 0));

        verify(inventoryService).doFindOrderByMerchantRef(order.getMerchantRef());
        verify(inventoryService).save(any(InventoryEntry.class));

        ArgumentCaptor<OrderEvent> eventCaptor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        var publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.merchantRef()).isEqualTo(order.getMerchantRef());
        assertThat(publishedEvent.order().getState()).isEqualTo(OrderState.INVENTORY_RESERVED);
    }
}
