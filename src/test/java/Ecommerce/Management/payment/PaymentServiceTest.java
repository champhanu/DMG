package Ecommerce.Management.payment;

import Ecommerce.Management.domain.order.Order;
import Ecommerce.Management.domain.payment.Payment;
import Ecommerce.Management.domain.payment.PaymentMethod;
import Ecommerce.Management.domain.payment.PaymentStatus;
import Ecommerce.Management.dto.payment.PaymentChargeRequest;
import Ecommerce.Management.dto.payment.RefundRequest;
import Ecommerce.Management.exception.InvalidOperationException;
import Ecommerce.Management.exception.PaymentFailedException;
import Ecommerce.Management.repository.payment.PaymentRepository;
import Ecommerce.Management.service.payment.PaymentGateway;
import Ecommerce.Management.service.payment.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private PaymentGateway paymentGateway;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private PaymentService paymentService;

	@Test
	void processPaymentThrowsWhenGatewayDeclines() {
		Order order = buildOrder();
		when(paymentGateway.charge(any(PaymentChargeRequest.class)))
				.thenReturn(Ecommerce.Management.dto.payment.PaymentGatewayResult.failure("Declined"));

		assertThatThrownBy(() -> paymentService.processPaymentForOrder(order, "CARD", true))
				.isInstanceOf(PaymentFailedException.class)
				.hasMessage("Declined");
	}

	@Test
	void refundRejectsWhenPaymentNotSuccessful() {
		Payment payment = buildSuccessfulPayment();
		payment.setStatus(PaymentStatus.FAILED);
		when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

		assertThatThrownBy(() -> paymentService.refundPayment(1L, new RefundRequest(new BigDecimal("10.00"), "test")))
				.isInstanceOf(InvalidOperationException.class);
	}

	private Order buildOrder() {
		Order order = new Order();
		order.setCustomerId(1L);
		order.setTotalAmount(new BigDecimal("100.00"));
		return order;
	}

	private Payment buildSuccessfulPayment() {
		Order order = buildOrder();
		Payment payment = new Payment();
		payment.setOrder(order);
		payment.setAmount(new BigDecimal("100.00"));
		payment.setRefundedAmount(BigDecimal.ZERO);
		payment.setPaymentMethod(PaymentMethod.CARD);
		payment.setStatus(PaymentStatus.SUCCESS);
		return payment;
	}

}
