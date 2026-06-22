package Ecommerce.Management.event;

import Ecommerce.Management.service.audit.AuditService;
import Ecommerce.Management.service.fulfillment.FulfillmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderPlacedEventListener {

	private static final Logger log = LoggerFactory.getLogger(OrderPlacedEventListener.class);

	private final AuditService auditService;
	private final FulfillmentService fulfillmentService;

	public OrderPlacedEventListener(AuditService auditService, FulfillmentService fulfillmentService) {
		this.auditService = auditService;
		this.fulfillmentService = fulfillmentService;
	}

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleOrderPlaced(OrderPlacedEvent event) {
		auditService.record(
				"ORDER_PLACED",
				"ORDER",
				event.orderId(),
				event.customerId(),
				"Order placed and confirmed");
		fulfillmentService.routeOrder(event.orderId());
		log.info("NOTIFICATION orderConfirmed customerId={} orderId={}", event.customerId(), event.orderId());
	}

}
