package com.orderlink.event;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.orderlink.dto.OrderState;
import com.orderlink.order.service.InventoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class OnLogisticsConfirmedStateOrder implements OrderProcessor {

    private final InventoryService inventoryService;

    @Async
    @EventListener
    public void processOrderEvent(OrderEvent event) {
        
        if(!OrderState.LOGISTICS_CONFIRMED.equals(event.order().getState())) {
            return;
        }
        log.info("Processing order in LOGISTICS_CONFIRMED state for merchantRef: {}", event.merchantRef());

        inventoryService.doFindOrderByMerchantRef(event.merchantRef())
                .ifPresentOrElse(entry -> {
                    entry.setStatus(OrderState.LOGISTICS_CONFIRMED);
                    entry.setTrackingRef(event.order().getTrackingRef());
                    entry.setDetails(OrderState.LOGISTICS_CONFIRMED.details);
                    inventoryService.save(entry);
                },
                () -> log.warn("Inventory entry not found for merchantRef {} when confirming logistics", event.merchantRef()));
    }

    
}
