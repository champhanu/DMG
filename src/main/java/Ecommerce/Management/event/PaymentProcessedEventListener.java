package Ecommerce.Management.event;

import Ecommerce.Management.service.audit.AuditService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PaymentProcessedEventListener {

	private final AuditService auditService;

	public PaymentProcessedEventListener(AuditService auditService) {
		this.auditService = auditService;
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handlePaymentProcessed(PaymentProcessedEvent event) {
		auditService.record(
				"PAYMENT_PROCESSED",
				"PAYMENT",
				event.paymentId(),
				event.customerId(),
				"Payment " + event.status() + " for order " + event.orderId() + " ref=" + event.transactionRef());
	}

}
