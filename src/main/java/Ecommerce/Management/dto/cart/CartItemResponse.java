package Ecommerce.Management.dto.cart;

import java.math.BigDecimal;

public record CartItemResponse(
		Long id,
		Long productId,
		String productName,
		String sku,
		Long categoryId,
		String categoryName,
		int quantity,
		BigDecimal unitPrice,
		BigDecimal lineTotal
) {
}
