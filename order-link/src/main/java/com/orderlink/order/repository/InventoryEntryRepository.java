package com.orderlink.order.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.orderlink.order.entity.InventoryEntry;


@Repository
public interface InventoryEntryRepository extends JpaRepository<InventoryEntry, UUID> {

    Optional<InventoryEntry> findByOrderRequest_MerchantRef(String merchantRef);

    Optional<InventoryEntry> findByTrackingRef(String trackingRef);
}
