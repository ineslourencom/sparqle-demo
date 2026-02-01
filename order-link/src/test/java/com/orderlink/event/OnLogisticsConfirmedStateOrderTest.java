package com.orderlink.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.orderlink.dto.OrderState;
import com.orderlink.fixtures.TestFixtures;
import com.orderlink.order.entity.InventoryEntry;
import com.orderlink.order.entity.OrderRequestEntry;
import com.orderlink.order.model.Order;
import com.orderlink.order.service.InventoryService;

@ExtendWith(MockitoExtension.class)
class OnLogisticsConfirmedStateOrderTest {

    @Mock
    private InventoryService inventoryService;

    private OnLogisticsConfirmedStateOrder processor;

    @BeforeEach
    void setUp() {
        processor = new OnLogisticsConfirmedStateOrder(inventoryService);
    }

    @Test
    void processOrderEvent_whenStateIsNotLogisticsConfirmed_shouldIgnore() {
        var orderRequest = TestFixtures.sampleOrderRequest();
        var order = Order.newInstance(orderRequest.merchantRef(), orderRequest, OrderState.PENDING, "TRACK-IGNORE");

        processor.processOrderEvent(new OrderEvent(order.getMerchantRef(), order));

        verifyNoInteractions(inventoryService);
    }

    @Test
    void processOrderEvent_whenEntryExists_shouldUpdateAndSave() {
        var orderRequest = TestFixtures.sampleOrderRequest();
        var order = Order.newInstance(orderRequest.merchantRef(), orderRequest, OrderState.LOGISTICS_CONFIRMED, "TRACK-123");

        var entry = InventoryEntry.builder()
                .id(UUID.randomUUID())
                .orderRequest(OrderRequestEntry.fromDto(orderRequest))
                .status(OrderState.PENDING)
                .trackingRef("OLD-TRACK")
                .details("old details")
                .createdAt(ZonedDateTime.now().minusDays(1))
                .updatedAt(ZonedDateTime.now().minusHours(1))
                .build();

        when(inventoryService.doFindOrderByMerchantRef(order.getMerchantRef()))
                .thenReturn(Optional.of(entry));
        when(inventoryService.save(entry)).thenReturn(entry);

        processor.processOrderEvent(new OrderEvent(order.getMerchantRef(), order));

        assertThat(entry.getStatus()).isEqualTo(OrderState.LOGISTICS_CONFIRMED);
        assertThat(entry.getTrackingRef()).isEqualTo(order.getTrackingRef());
        assertThat(entry.getDetails()).isEqualTo(OrderState.LOGISTICS_CONFIRMED.details);
        verify(inventoryService).save(entry);
    }

    @Test
    void processOrderEvent_whenEntryMissing_shouldOnlyLogWarning() {
        var orderRequest = TestFixtures.sampleOrderRequest();
        var order = Order.newInstance(orderRequest.merchantRef(), orderRequest, OrderState.LOGISTICS_CONFIRMED, "TRACK-999");

        when(inventoryService.doFindOrderByMerchantRef(order.getMerchantRef()))
                .thenReturn(Optional.empty());

        processor.processOrderEvent(new OrderEvent(order.getMerchantRef(), order));

        verify(inventoryService).doFindOrderByMerchantRef(order.getMerchantRef());
        verify(inventoryService, never()).save(any());
    }
}
