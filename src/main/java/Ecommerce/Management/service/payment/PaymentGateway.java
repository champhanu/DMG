package Ecommerce.Management.service.payment;

import Ecommerce.Management.dto.payment.PaymentChargeRequest;
import Ecommerce.Management.dto.payment.PaymentGatewayResult;

public interface PaymentGateway {

	PaymentGatewayResult charge(PaymentChargeRequest request);

}
