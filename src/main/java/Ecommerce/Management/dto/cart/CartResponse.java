package Ecommerce.Management.dto.cart;

import Ecommerce.Management.domain.cart.CartStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CartResponse(
		Long id,
		Long customerId,
		CartStatus status,
		List<CartItemResponse> items,
		int lineCount,
		int totalItems,
		BigDecimal subtotal,
		Instant updatedAt
) {
}
