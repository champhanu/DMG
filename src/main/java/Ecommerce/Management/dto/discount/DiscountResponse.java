package Ecommerce.Management.dto.discount;

import Ecommerce.Management.domain.discount.DiscountType;

import java.math.BigDecimal;
import java.time.Instant;

public record DiscountResponse(
		Long id,
		String code,
		DiscountType type,
		BigDecimal value,
		BigDecimal minOrderAmount,
		Integer maxUses,
		int usedCount,
		boolean active,
		Instant createdAt,
		Instant updatedAt
) {
}
