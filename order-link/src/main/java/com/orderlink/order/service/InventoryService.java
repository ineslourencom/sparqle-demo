package com.orderlink.order.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.orderlink.order.entity.InventoryEntry;
import com.orderlink.order.repository.InventoryEntryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryEntryRepository inventoryEntryRepository;


    public Optional<InventoryEntry> doFindOrderByMerchantRef(String merchantRef) {
        return inventoryEntryRepository.findByOrderRequest_MerchantRef(merchantRef);
    }
 
    public Optional<InventoryEntry> doFindOrderByTrackingRef(String trackingRef) {
        return inventoryEntryRepository.findByTrackingRef(trackingRef);
    }

    public InventoryEntry save(InventoryEntry inventoryEntry) {
        return inventoryEntryRepository.save(inventoryEntry);
    }

    public Page<InventoryEntry> getAllOrders(int page, int size) {
    return inventoryEntryRepository.findAll(Pageable.ofSize(size)
            .withPage(page));
    }

}
