package Ecommerce.Management.service.order;

import Ecommerce.Management.exception.InvalidOrderStateTransitionException;
import Ecommerce.Management.domain.order.OrderStatus;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class OrderStateMachine {

	private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
			OrderStatus.CREATED, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
			OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.PACKED, OrderStatus.CANCELLED),
			OrderStatus.PACKED, EnumSet.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED),
			OrderStatus.DELIVERED, EnumSet.of(OrderStatus.RETURNED),
			OrderStatus.RETURNED, EnumSet.noneOf(OrderStatus.class),
			OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));

	private OrderStateMachine() {
	}

	public static void validateTransition(OrderStatus from, OrderStatus to) {
		if (!canTransition(from, to)) {
			throw new InvalidOrderStateTransitionException(from, to);
		}
	}

	public static boolean canTransition(OrderStatus from, OrderStatus to) {
		return ALLOWED.getOrDefault(from, Set.of()).contains(to);
	}

	public static Set<OrderStatus> allowedNextStatuses(OrderStatus current) {
		return ALLOWED.getOrDefault(current, Set.of());
	}

}
