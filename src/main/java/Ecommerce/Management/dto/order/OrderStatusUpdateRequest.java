package Ecommerce.Management.dto.order;

import Ecommerce.Management.domain.order.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(
		@NotNull OrderStatus status
) {
}
