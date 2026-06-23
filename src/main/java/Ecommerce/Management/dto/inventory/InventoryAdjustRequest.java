package Ecommerce.Management.dto.inventory;

import jakarta.validation.constraints.NotNull;

public record InventoryAdjustRequest(
		@NotNull Long warehouseId,
		@NotNull Long productId,
		@NotNull Integer delta
) {
}
