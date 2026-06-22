package Ecommerce.Management.service.payment;

import Ecommerce.Management.domain.order.Order;
import Ecommerce.Management.domain.payment.Payment;
import Ecommerce.Management.domain.payment.PaymentMethod;
import Ecommerce.Management.domain.payment.PaymentStatus;
import Ecommerce.Management.dto.payment.PaymentChargeRequest;
import Ecommerce.Management.dto.payment.PaymentGatewayResult;
import Ecommerce.Management.dto.payment.PaymentResponse;
import Ecommerce.Management.dto.payment.RefundRequest;
import Ecommerce.Management.event.PaymentProcessedEvent;
import Ecommerce.Management.exception.InvalidOperationException;
import Ecommerce.Management.exception.PaymentFailedException;
import Ecommerce.Management.exception.ResourceNotFoundException;
import Ecommerce.Management.repository.payment.PaymentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional
public class PaymentService {

	private final PaymentRepository paymentRepository;
	private final PaymentGateway paymentGateway;
	private final ApplicationEventPublisher eventPublisher;

	public PaymentService(
			PaymentRepository paymentRepository,
			PaymentGateway paymentGateway,
			ApplicationEventPublisher eventPublisher) {
		this.paymentRepository = paymentRepository;
		this.paymentGateway = paymentGateway;
		this.eventPublisher = eventPublisher;
	}

	public Payment processPaymentForOrder(Order order, String paymentMethod, boolean simulateFailure) {
		Payment payment = new Payment();
		payment.setAmount(order.getTotalAmount());
		payment.setCurrency("USD");
		payment.setPaymentMethod(parsePaymentMethod(paymentMethod));
		payment.setStatus(PaymentStatus.PENDING);
		payment.setRefundedAmount(BigDecimal.ZERO);
		order.setPayment(payment);

		PaymentChargeRequest chargeRequest = new PaymentChargeRequest(
				order.getId(),
				order.getCustomerId(),
				order.getTotalAmount(),
				"USD",
				payment.getPaymentMethod().name(),
				simulateFailure);

		PaymentGatewayResult result = paymentGateway.charge(chargeRequest);
		if (!result.success()) {
			payment.setStatus(PaymentStatus.FAILED);
			payment.setFailureReason(result.failureReason());
			throw new PaymentFailedException(result.failureReason());
		}

		payment.setStatus(PaymentStatus.SUCCESS);
		payment.setTransactionRef(result.transactionRef());
		return payment;
	}

	public void publishPaymentProcessed(Payment payment) {
		eventPublisher.publishEvent(new PaymentProcessedEvent(
				payment.getId(),
				payment.getOrder().getId(),
				payment.getOrder().getCustomerId(),
				payment.getStatus(),
				payment.getTransactionRef()));
	}

	@Transactional(readOnly = true)
	public PaymentResponse getPayment(Long paymentId) {
		return toResponse(findPayment(paymentId));
	}

	@Transactional(readOnly = true)
	public PaymentResponse getPaymentByOrderId(Long orderId) {
		return paymentRepository.findByOrderId(orderId)
				.map(this::toResponse)
				.orElseThrow(() -> new ResourceNotFoundException("Payment not found for order: " + orderId));
	}

	@Transactional(readOnly = true)
	public List<PaymentResponse> listCustomerPayments(Long customerId) {
		return paymentRepository.findByCustomerId(customerId).stream()
				.map(this::toResponse)
				.toList();
	}

	public PaymentResponse refundPayment(Long paymentId, RefundRequest request) {
		Payment payment = findPayment(paymentId);
		if (payment.getStatus() != PaymentStatus.SUCCESS
				&& payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
			throw new InvalidOperationException("Only successful payments can be refunded");
		}

		BigDecimal refundAmount = request.amount().setScale(2, RoundingMode.HALF_UP);
		BigDecimal remaining = payment.getAmount().subtract(payment.getRefundedAmount());
		if (refundAmount.compareTo(remaining) > 0) {
			throw new InvalidOperationException("Refund amount exceeds remaining balance");
		}

		BigDecimal newRefundedTotal = payment.getRefundedAmount().add(refundAmount);
		payment.setRefundedAmount(newRefundedTotal);
		if (newRefundedTotal.compareTo(payment.getAmount()) == 0) {
			payment.setStatus(PaymentStatus.REFUNDED);
		}
		else {
			payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
		}

		publishPaymentProcessed(payment);
		return toResponse(payment);
	}

	private Payment findPayment(Long paymentId) {
		return paymentRepository.findById(paymentId)
				.orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
	}

	private PaymentMethod parsePaymentMethod(String paymentMethod) {
		if (paymentMethod == null || paymentMethod.isBlank()) {
			return PaymentMethod.CARD;
		}
		try {
			return PaymentMethod.valueOf(paymentMethod.trim().toUpperCase());
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidOperationException("Unsupported payment method: " + paymentMethod);
		}
	}

	private PaymentResponse toResponse(Payment payment) {
		return new PaymentResponse(
				payment.getId(),
				payment.getOrder().getId(),
				payment.getOrder().getCustomerId(),
				payment.getAmount(),
				payment.getRefundedAmount(),
				payment.getCurrency(),
				payment.getStatus(),
				payment.getPaymentMethod(),
				payment.getTransactionRef(),
				payment.getFailureReason(),
				payment.getCreatedAt(),
				payment.getUpdatedAt());
	}

}
