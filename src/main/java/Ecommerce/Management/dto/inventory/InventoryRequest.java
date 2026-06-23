package Ecommerce.Management.dto.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventoryRequest(
		@NotNull Long warehouseId,
		@NotNull Long productId,
		@NotNull @Min(1) Integer quantity
) {
}
