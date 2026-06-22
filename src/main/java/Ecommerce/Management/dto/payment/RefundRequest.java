package Ecommerce.Management.dto.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RefundRequest(
		@NotNull @DecimalMin(value = "0.01", message = "must be greater than zero") BigDecimal amount,
		@NotNull String reason
) {
}
