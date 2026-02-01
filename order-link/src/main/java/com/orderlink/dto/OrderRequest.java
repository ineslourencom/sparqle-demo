package com.orderlink.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OrderRequest(

    @NotNull(message = "Merchant ID cannot be null")
    Long merchantId,

    @NotBlank(message = "Order reference cannot be blank")
    @Size(max = 255, message = "Order reference must not exceed 255 characters")
    String merchantRef,

    @Pattern(
        regexp = "^[A-Z0-9]{10,16}$",
        message = "Barcode must be uppercase alphanumeric and 10-16 characters long"
    )
    String barcode,

    OrderRequest.Recipient recipient,
    
    OrderRequest.Parcel parcel

) {
    public record Recipient(
        @NotBlank(message = "Recipient name cannot be blank")
        String name,
        
        @NotBlank(message = "Address1 cannot be blank")
        String address1,
        
        String address2,

        @NotBlank(message = "Postal code cannot be blank")
        String postalCode,
        
        @NotBlank(message = "City cannot be blank")
        String city,
        
        @NotBlank(message = "Country cannot be blank")
        String country
    ) {}

    public record Parcel(
        @NotNull(message = "Weight in grams is required")
        @Min(value = 1, message = "Weight in grams must be at least 1")
        long weightGrams,

        @NotNull(message = "Length in cm is required")
        @DecimalMin(value = "0.1", inclusive = true, message = "Length in cm must be at least 0.1")
        BigDecimal lengthCm,

        @NotNull(message = "Width in cm is required")
        @DecimalMin(value = "0.1", inclusive = true, message = "Width in cm must be at least 0.1")
        BigDecimal widthCm,

        @NotNull(message = "Height in cm is required")
        @DecimalMin(value = "0.1", inclusive = true, message = "Height in cm must be at least 0.1")
        BigDecimal heightCm
    ) {}
}
