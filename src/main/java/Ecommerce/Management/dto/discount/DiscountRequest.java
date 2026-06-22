package Ecommerce.Management.dto.discount;

import Ecommerce.Management.domain.discount.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DiscountRequest(
		@NotBlank @Size(max = 32) String code,
		@NotNull DiscountType type,
		@NotNull @DecimalMin("0.01") BigDecimal value,
		BigDecimal minOrderAmount,
		Integer maxUses,
		boolean active
) {
}
