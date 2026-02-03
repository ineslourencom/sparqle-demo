package com.orderlink.order.controller;


import org.springframework.data.domain.Page;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.orderlink.dto.OrderRequest;
import com.orderlink.dto.OrderResponse;
import com.orderlink.order.service.OrderServiceFacade;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderServiceFacade orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest orderRequest) {
        var order = orderService.createOrder(orderRequest);
        return ResponseEntity
        .status(HttpStatus.ACCEPTED)
        .body(order);
    }

    
    @GetMapping("/{merchantRef}")
    public ResponseEntity<OrderResponse> getOrderByRef(@PathVariable("merchantRef") String merchantRef) {
        try {
            return ResponseEntity.ok(orderService.getOrderByMerchantRef(merchantRef));
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(orderService.getAllOrders(page, size));
    }

    @DeleteMapping("/{merchantRef}")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable("merchantRef") String merchantRef) throws NotFoundException {
        var order = orderService.cancelOrderByMerchantRef(merchantRef);
        return ResponseEntity.ok(order);
    }

}
