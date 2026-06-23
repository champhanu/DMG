package Ecommerce.Management.dto.payment;

import java.math.BigDecimal;

public record PaymentChargeRequest(
		Long orderId,
		Long customerId,
		BigDecimal amount,
		String currency,
		String paymentMethod,
		boolean simulateFailure
) {
}
