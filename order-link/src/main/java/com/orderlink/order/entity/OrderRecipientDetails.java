package com.orderlink.order.entity;

import com.orderlink.dto.OrderRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@Embeddable
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderRecipientDetails {

    @Column(name = "recipient_name", nullable = false)
    private String name;

    @Column(name = "recipient_address1", nullable = false)
    private String address1;

    @Column(name = "recipient_address2")
    private String address2;

    @Column(name = "recipient_postal_code", nullable = false)
    private String postalCode;

    @Column(name = "recipient_city", nullable = false)
    private String city;

    @Column(name = "recipient_country", nullable = false)
    private String country;

    public static OrderRecipientDetails fromDto(OrderRequest.Recipient recipient) {
        if (recipient == null) {
            throw new IllegalArgumentException("Recipient details are required");
        }
        return OrderRecipientDetails.builder()
                .name(recipient.name())
                .address1(recipient.address1())
                .address2(recipient.address2())
                .postalCode(recipient.postalCode())
                .city(recipient.city())
                .country(recipient.country())
                .build();
    }

    public OrderRequest.Recipient toDto() {
        return new OrderRequest.Recipient(name, address1, address2, postalCode, city, country);
    }
}
