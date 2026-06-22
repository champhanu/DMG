package Ecommerce.Management.dto.inventory;

public record InventoryResponse(
		Long id,
		Long warehouseId,
		String warehouseCode,
		Long productId,
		String sku,
		int quantityAvailable,
		int quantityReserved
) {
}
