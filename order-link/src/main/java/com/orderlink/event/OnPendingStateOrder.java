package com.orderlink.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.orderlink.dto.OrderState;
import com.orderlink.order.entity.InventoryEntry;
import com.orderlink.order.entity.OrderRequestEntry;
import com.orderlink.order.model.Order;
import com.orderlink.order.service.InventoryService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnPendingStateOrder implements OrderProcessor {

    private final InventoryService inventoryService;
    private final ApplicationEventPublisher eventPublisher;

    @Async
    @EventListener
    public void processOrderEvent(OrderEvent event) {
        
        if(!OrderState.PENDING.equals(event.order().getState())) {
            return;
        }
        log.info("Processing order in PENDING state for merchantRef: {}", event.merchantRef());

        var reservedOrder = reserveInventory(event.order());
        eventPublisher.publishEvent(new OrderEvent(reservedOrder.getMerchantRef(),reservedOrder));
    }

    @Transactional
    public Order reserveInventory(Order order) {
        Order newOrder;
        try {
            var existingInventory = inventoryService.doFindOrderByMerchantRef(order.getMerchantRef());
            InventoryEntry inventoryEntry;
            if (existingInventory.isPresent()) {
                inventoryEntry = refreshExistingEntry(existingInventory.get(), order);
            } else {
                inventoryEntry = buildNewEntry(order);
            }

            inventoryService.save(inventoryEntry);
            newOrder = Order.newInstance(order.getMerchantRef(), order.getOrderRequest(), OrderState.INVENTORY_RESERVED);
        } catch (Exception e) {
            newOrder = Order.newInstance(order.getMerchantRef(), order.getOrderRequest(), OrderState.FAILED_INVENTORY);
            var failedEntry = inventoryService.doFindOrderByMerchantRef(order.getMerchantRef())
                    .orElseGet(() -> InventoryEntry.builder()
                            .orderRequest(OrderRequestEntry.fromDto(order.getOrderRequest()))
                            .build());
            failedEntry.setStatus(OrderState.FAILED_INVENTORY);
            failedEntry.setDetails(OrderState.FAILED_INVENTORY.details);
            failedEntry.setTrackingRef(order.getTrackingRef());
            inventoryService.save(failedEntry);
        }
        return newOrder;
    }

    private InventoryEntry buildNewEntry(Order order) {
        var requestEntry = OrderRequestEntry.fromDto(order.getOrderRequest());
        return InventoryEntry.builder()
                .orderRequest(requestEntry)
                .trackingRef(order.getTrackingRef())
                .status(OrderState.INVENTORY_RESERVED)
                .details(OrderState.INVENTORY_RESERVED.details)
                .build();
    }

    private InventoryEntry refreshExistingEntry(InventoryEntry existingEntry, Order order) {
        existingEntry.getOrderRequest().updateFromDto(order.getOrderRequest());
        existingEntry.setStatus(OrderState.INVENTORY_RESERVED);
        existingEntry.setDetails(OrderState.INVENTORY_RESERVED.details);
        existingEntry.setTrackingRef(order.getTrackingRef());
        return existingEntry;
    }


}
