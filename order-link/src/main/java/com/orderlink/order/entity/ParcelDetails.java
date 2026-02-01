package com.orderlink.order.entity;

import com.orderlink.dto.OrderRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
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
public class ParcelDetails {

    @Column(name = "parcel_weight_grams", nullable = false)
    private Long weightGrams;

    @Column(name = "parcel_length_cm", nullable = false, precision = 10, scale = 2)
    private BigDecimal lengthCm;

    @Column(name = "parcel_width_cm", nullable = false, precision = 10, scale = 2)
    private BigDecimal widthCm;

    @Column(name = "parcel_height_cm", nullable = false, precision = 10, scale = 2)
    private BigDecimal heightCm;

    public static ParcelDetails fromDto(OrderRequest.Parcel parcel) {
        if (parcel == null) {
            throw new IllegalArgumentException("Parcel details are required");
        }
        return ParcelDetails.builder()
                .weightGrams(parcel.weightGrams())
                .lengthCm(parcel.lengthCm())
                .widthCm(parcel.widthCm())
                .heightCm(parcel.heightCm())
                .build();
    }

    public OrderRequest.Parcel toDto() {
        return new OrderRequest.Parcel(weightGrams, lengthCm, widthCm, heightCm);
    }
}
