package com.orderlink.order.entity;

import com.orderlink.dto.OrderRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@Entity
@EqualsAndHashCode(of = "id")
@Table(name = "order_request_entries")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderRequestEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_request_id")
    private Long id;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "merchant_ref", nullable = false, unique = true, length = 255)
    private String merchantRef;

    @Column(name = "barcode", length = 16)
    private String barcode;

    @Embedded
    private OrderRecipientDetails recipient;

    @Embedded
    private ParcelDetails parcel;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    public static OrderRequestEntry fromDto(OrderRequest orderRequestDto) {
        if (orderRequestDto == null) {
            throw new IllegalArgumentException("Order request payload is required");
        }
        return OrderRequestEntry.builder()
                .merchantId(orderRequestDto.merchantId())
                .merchantRef(orderRequestDto.merchantRef())
                .barcode(orderRequestDto.barcode())
                .recipient(OrderRecipientDetails.fromDto(orderRequestDto.recipient()))
                .parcel(ParcelDetails.fromDto(orderRequestDto.parcel()))
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();
    }

    public void updateFromDto(OrderRequest orderRequestDto) {
        if (orderRequestDto == null) {
            throw new IllegalArgumentException("Order request payload is required");
        }
        merchantId = orderRequestDto.merchantId();
        merchantRef = orderRequestDto.merchantRef();
        barcode = orderRequestDto.barcode();
        recipient = OrderRecipientDetails.fromDto(orderRequestDto.recipient());
        parcel = ParcelDetails.fromDto(orderRequestDto.parcel());
        updatedAt = ZonedDateTime.now();
    }

    public OrderRequest toDto() {
        return new OrderRequest(
                merchantId,
                merchantRef,
                barcode,
                recipient != null ? recipient.toDto() : null,
                parcel != null ? parcel.toDto() : null
        );
    }

    @PrePersist
    void onCreate() {
        var now = ZonedDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = ZonedDateTime.now();
    }
}
