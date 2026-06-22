package Ecommerce.Management.dto.catalog;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
		Long id,
		String name,
		String sku,
		String description,
		BigDecimal price,
		Long categoryId,
		String categoryName,
		boolean active,
		Instant createdAt,
		Instant updatedAt
) {
}
