package Ecommerce.Management.dto.checkout;

import java.math.BigDecimal;

public record CheckoutItemResponse(
		Long productId,
		String productName,
		String sku,
		int quantity,
		BigDecimal unitPrice,
		BigDecimal lineTotal,
		Long warehouseId
) {
}
