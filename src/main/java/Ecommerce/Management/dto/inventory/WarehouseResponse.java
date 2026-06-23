package Ecommerce.Management.dto.inventory;

import java.time.Instant;

public record WarehouseResponse(
		Long id,
		String name,
		String code,
		String location,
		boolean active,
		Instant createdAt,
		Instant updatedAt
) {
}
