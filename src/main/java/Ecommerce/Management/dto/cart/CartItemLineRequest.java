package Ecommerce.Management.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemLineRequest(
		@NotNull Long productId,
		@NotNull @Min(1) Integer quantity
) {
}
