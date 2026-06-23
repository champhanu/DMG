package Ecommerce.Management.service.payment;

import Ecommerce.Management.dto.payment.PaymentChargeRequest;
import Ecommerce.Management.dto.payment.PaymentGatewayResult;
import Ecommerce.Management.dto.payment.PaymentRefundRequest;

public interface PaymentGateway {

	PaymentGatewayResult charge(PaymentChargeRequest request);

	PaymentGatewayResult refund(PaymentRefundRequest request);

}
