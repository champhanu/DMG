package Ecommerce.Management.dto.inventory;

import java.time.Instant;
import java.util.List;

public record ProductStockResponse(
		Long productId,
		String productName,
		String sku,
		int totalAvailable,
		int totalReserved,
		int totalOnHand,
		List<InventoryResponse> warehouses
) {
}
