package Ecommerce.Management.dto.payment;

import Ecommerce.Management.domain.payment.PaymentMethod;
import Ecommerce.Management.domain.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
		Long id,
		Long orderId,
		Long customerId,
		BigDecimal amount,
		BigDecimal refundedAmount,
		String currency,
		PaymentStatus status,
		PaymentMethod paymentMethod,
		String transactionRef,
		String failureReason,
		Instant createdAt,
		Instant updatedAt
) {
}
