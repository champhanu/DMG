package Ecommerce.Management.exception;

import Ecommerce.Management.domain.order.OrderStatus;

public class InvalidOrderStateTransitionException extends RuntimeException {

	public InvalidOrderStateTransitionException(OrderStatus from, OrderStatus to) {
		super("Invalid order transition from " + from + " to " + to);
	}

}
