package Ecommerce.Management.dto.inventory;

import java.time.Instant;

public record InventoryResponse(
		Long id,
		Long warehouseId,
		String warehouseCode,
		String warehouseName,
		Long productId,
		String productName,
		String sku,
		int quantityAvailable,
		int quantityReserved,
		int totalOnHand,
		Instant updatedAt
) {
}
