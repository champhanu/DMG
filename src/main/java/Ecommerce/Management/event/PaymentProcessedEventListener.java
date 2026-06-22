package Ecommerce.Management.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PaymentProcessedEventListener {

	private static final Logger log = LoggerFactory.getLogger(PaymentProcessedEventListener.class);

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handlePaymentProcessed(PaymentProcessedEvent event) {
		log.info("AUDIT paymentProcessed paymentId={} orderId={} customerId={} status={} ref={}",
				event.paymentId(),
				event.orderId(),
				event.customerId(),
				event.status(),
				event.transactionRef());
	}

}
