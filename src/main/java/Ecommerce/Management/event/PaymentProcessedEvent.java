package Ecommerce.Management.event;

import Ecommerce.Management.domain.payment.PaymentStatus;

public record PaymentProcessedEvent(
		Long paymentId,
		Long orderId,
		Long customerId,
		PaymentStatus status,
		String transactionRef
) {
}
