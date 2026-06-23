package Ecommerce.Management.dto.payment;

import java.math.BigDecimal;

public record PaymentRefundRequest(
		String transactionRef,
		BigDecimal amount,
		String reason
) {
}
