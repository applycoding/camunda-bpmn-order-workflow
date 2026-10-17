package com.arpanbom.orderworkflow.api;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record StartOrderRequest(
        @NotBlank
        @Size(max = 64)
        @Pattern(regexp = "[A-Za-z0-9._-]+", message = "Use letters, numbers, dots, underscores, or hyphens.")
        String orderId,
        @NotBlank
        @Size(max = 64)
        @Pattern(regexp = "[A-Za-z0-9._-]+", message = "Use letters, numbers, dots, underscores, or hyphens.")
        String customerId,
        @NotNull
        @Positive
        @Digits(integer = 12, fraction = 2)
        BigDecimal amount,
        @NotBlank
        @Pattern(regexp = "[A-Z]{3}", message = "Use a three-letter ISO currency code.")
        String currency) {}
