package Ecommerce.Management.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderPlacedEventListener {

	private static final Logger log = LoggerFactory.getLogger(OrderPlacedEventListener.class);

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleOrderPlaced(OrderPlacedEvent event) {
		log.info("AUDIT orderPlaced orderId={} customerId={}", event.orderId(), event.customerId());
		log.info("NOTIFICATION orderConfirmed customerId={} orderId={}", event.customerId(), event.orderId());
	}

}
