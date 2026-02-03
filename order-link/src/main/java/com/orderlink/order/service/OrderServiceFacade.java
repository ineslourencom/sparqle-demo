package com.orderlink.order.service;

import java.time.ZonedDateTime;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orderlink.dto.OrderRequest;
import com.orderlink.dto.OrderResponse;
import com.orderlink.dto.OrderState;
import com.orderlink.event.OrderEvent;
import com.orderlink.exception.OrderCancellationException;
import com.orderlink.order.model.Order;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderServiceFacade {

    private final ApplicationEventPublisher eventPublisher;
    private final InventoryService inventoryService;


    public OrderResponse createOrder(OrderRequest orderRequest) {

        var internalOrder = Order.builder()
                .merchantRef(orderRequest.merchantRef())
                .orderRequest(orderRequest)
                .build();

        eventPublisher.publishEvent(new OrderEvent(orderRequest.merchantRef(), internalOrder, 0));

        return OrderResponse.builder()
                .merchantRef(internalOrder.getMerchantRef())
                .state(OrderState.PENDING)
                .details(OrderState.PENDING.details)
                .updatedAt(ZonedDateTime.now())
                .build();
    }

    public OrderResponse getOrderByMerchantRef(String merchantRef) throws NotFoundException {
        return inventoryService.doFindOrderByMerchantRef(merchantRef)
                .map(order -> OrderResponse.builder()
                    .merchantRef(order.getMerchantRef())
                    .state(order.getStatus())
                    .details(order.getStatus().details)
                    .updatedAt(order.getUpdatedAt())
                    .build())
                .orElseThrow(() -> new NotFoundException());
    }  
        

                
    public Page<OrderResponse> getAllOrders(int page, int size) {
        return inventoryService.getAllOrders(page, size)
            .map(order -> OrderResponse.builder()
                .merchantRef(order.getMerchantRef())
                .state(order.getStatus())
                .details(order.getStatus().details)
                .updatedAt(order.getUpdatedAt())
                .build());
    }

    @Transactional
    public OrderResponse cancelOrderByMerchantRef(String merchantRef) throws NotFoundException {
        var inventoryEntry = inventoryService.doFindOrderByMerchantRef(merchantRef)
                .orElseThrow(() -> new NotFoundException());

        if (!inventoryEntry.getStatus().isCancelable()) {
            throw new OrderCancellationException("Cannot cancel order with status " + inventoryEntry.getStatus());
        }

        inventoryEntry.setStatus(OrderState.CANCELLED);
        inventoryEntry.setDetails(OrderState.CANCELLED.details);
        inventoryService.save(inventoryEntry);

        return OrderResponse.builder()
                .merchantRef(merchantRef)
                .state(OrderState.CANCELLED)
                .details(OrderState.CANCELLED.details)
                .updatedAt(ZonedDateTime.now())
                .build();
    }

}
