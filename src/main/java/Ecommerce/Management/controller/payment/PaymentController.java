package Ecommerce.Management.controller.payment;

import Ecommerce.Management.dto.payment.PaymentResponse;
import Ecommerce.Management.dto.payment.RefundRequest;
import Ecommerce.Management.service.payment.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@Validated
public class PaymentController {

	private final PaymentService paymentService;

	public PaymentController(PaymentService paymentService) {
		this.paymentService = paymentService;
	}

	@GetMapping("/{paymentId}")
	public PaymentResponse getPayment(@PathVariable Long paymentId) {
		return paymentService.getPayment(paymentId);
	}

	@GetMapping("/order/{orderId}")
	public PaymentResponse getPaymentByOrder(@PathVariable Long orderId) {
		return paymentService.getPaymentByOrderId(orderId);
	}

	@GetMapping
	public List<PaymentResponse> listPayments(@RequestParam @NotNull Long customerId) {
		return paymentService.listCustomerPayments(customerId);
	}

	@PostMapping("/{paymentId}/refund")
	public PaymentResponse refundPayment(
			@PathVariable Long paymentId,
			@Valid @RequestBody RefundRequest request) {
		return paymentService.refundPayment(paymentId, request);
	}

}
