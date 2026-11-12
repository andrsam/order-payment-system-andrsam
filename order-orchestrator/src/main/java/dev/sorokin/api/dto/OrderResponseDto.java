package dev.sorokin.api.dto;

import dev.sorokin.enums.PaymentStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record OrderResponseDto(
        UUID id,
        String address,
        BigDecimal clientEstimate,
        BigDecimal authorizedAmount,
        BigDecimal finalAmount,
        BigDecimal capturedAmount,
        PaymentStatus paymentStatus

) { }
