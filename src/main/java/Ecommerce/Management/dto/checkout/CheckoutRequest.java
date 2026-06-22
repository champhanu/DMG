package Ecommerce.Management.dto.checkout;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CheckoutRequest(
		@NotNull Long customerId,
		@Size(max = 32) String paymentMethod,
		Boolean simulatePaymentFailure
) {
}
