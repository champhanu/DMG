package Ecommerce.Management.dto.catalog;

import java.time.Instant;

public record CategoryResponse(
		Long id,
		String name,
		String slug,
		String description,
		Long parentId,
		boolean active,
		Instant createdAt,
		Instant updatedAt
) {
}
