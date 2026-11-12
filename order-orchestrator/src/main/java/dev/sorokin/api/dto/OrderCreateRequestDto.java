package dev.sorokin.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record OrderCreateRequestDto(
        @Schema(description = "Адрес", defaultValue = "Москва,3-я улица Строителей, дом 25")
        String address,

        @Schema(description = "Сумма платежа", defaultValue = "10000")
        BigDecimal clientEstimate
) { }
