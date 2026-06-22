package Ecommerce.Management.dto.order;

import java.math.BigDecimal;

public record OrderItemResponse(
		Long id,
		Long productId,
		String productName,
		String sku,
		int quantity,
		BigDecimal unitPrice,
		BigDecimal lineTotal,
		Long warehouseId
) {
}
