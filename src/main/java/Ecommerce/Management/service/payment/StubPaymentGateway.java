package Ecommerce.Management.service.payment;

import Ecommerce.Management.dto.payment.PaymentChargeRequest;
import Ecommerce.Management.dto.payment.PaymentGatewayResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class StubPaymentGateway implements PaymentGateway {

	@Override
	public PaymentGatewayResult charge(PaymentChargeRequest request) {
		if (request.simulateFailure()) {
			return PaymentGatewayResult.failure("Payment declined by gateway");
		}
		if (request.amount() == null || request.amount().signum() <= 0) {
			return PaymentGatewayResult.failure("Invalid payment amount");
		}
		return PaymentGatewayResult.success("TXN-" + UUID.randomUUID());
	}

}
