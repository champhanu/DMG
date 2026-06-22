package Ecommerce.Management.dto.order;

import Ecommerce.Management.domain.order.OrderStatus;
import Ecommerce.Management.domain.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public record OrderResponse(
		Long id,
		Long customerId,
		Long cartId,
		OrderStatus status,
		String statusReason,
		Set<OrderStatus> allowedNextStatuses,
		PaymentStatus paymentStatus,
		String transactionRef,
		BigDecimal subtotal,
		BigDecimal taxAmount,
		BigDecimal totalAmount,
		List<OrderItemResponse> items,
		Instant createdAt,
		Instant updatedAt
) {
}
