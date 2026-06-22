package Ecommerce.Management.dto.checkout;

import Ecommerce.Management.domain.order.OrderStatus;
import Ecommerce.Management.domain.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CheckoutResponse(
		Long orderId,
		Long customerId,
		OrderStatus status,
		PaymentStatus paymentStatus,
		String transactionRef,
		BigDecimal subtotal,
		BigDecimal taxAmount,
		BigDecimal totalAmount,
		List<CheckoutItemResponse> items,
		Instant createdAt
) {
}
