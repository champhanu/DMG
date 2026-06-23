package Ecommerce.Management.dto.demo;

import Ecommerce.Management.domain.order.OrderStatus;

import java.util.List;
import java.util.Map;

public record DemoSeedResponse(
		String message,
		int categoriesCreated,
		int productsCreated,
		int warehousesCreated,
		int inventoryRecordsCreated,
		Map<OrderStatus, Long> ordersByStatus,
		List<Long> multiItemOrderIds
) {
}
