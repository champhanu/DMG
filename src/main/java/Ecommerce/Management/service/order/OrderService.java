package Ecommerce.Management.service.order;

import Ecommerce.Management.domain.order.Order;
import Ecommerce.Management.domain.order.OrderStatus;
import Ecommerce.Management.domain.payment.PaymentStatus;
import Ecommerce.Management.dto.order.OrderCancelRequest;
import Ecommerce.Management.dto.order.OrderItemResponse;
import Ecommerce.Management.dto.order.OrderResponse;
import Ecommerce.Management.dto.order.OrderReturnRequest;
import Ecommerce.Management.dto.order.OrderStatusUpdateRequest;
import Ecommerce.Management.dto.payment.RefundRequest;
import Ecommerce.Management.exception.InvalidOperationException;
import Ecommerce.Management.exception.ResourceNotFoundException;
import Ecommerce.Management.repository.order.OrderRepository;
import Ecommerce.Management.service.inventory.InventoryService;
import Ecommerce.Management.service.payment.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class OrderService {

	private final OrderRepository orderRepository;
	private final InventoryService inventoryService;
	private final PaymentService paymentService;

	public OrderService(
			OrderRepository orderRepository,
			InventoryService inventoryService,
			PaymentService paymentService) {
		this.orderRepository = orderRepository;
		this.inventoryService = inventoryService;
		this.paymentService = paymentService;
	}

	@Transactional(readOnly = true)
	public OrderResponse getOrder(Long orderId) {
		return toResponse(findOrder(orderId));
	}

	@Transactional(readOnly = true)
	public List<OrderResponse> listCustomerOrders(Long customerId) {
		return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
				.map(this::toResponse)
				.toList();
	}

	public OrderResponse updateStatus(Long orderId, OrderStatusUpdateRequest request) {
		Order order = findOrder(orderId);
		transition(order, request.status(), null);
		return toResponse(order);
	}

	public OrderResponse cancelOrder(Long orderId, OrderCancelRequest request) {
		Order order = findOrder(orderId);
		assertCustomer(order, request.customerId());

		if (!Set.of(OrderStatus.CREATED, OrderStatus.CONFIRMED).contains(order.getStatus())) {
			throw new InvalidOperationException("Order cannot be cancelled in status: " + order.getStatus());
		}

		transition(order, OrderStatus.CANCELLED, request.reason());
		inventoryService.releaseOrderReservations(order);
		refundIfPaid(order, request.reason());
		return toResponse(order);
	}

	public OrderResponse requestReturn(Long orderId, OrderReturnRequest request) {
		Order order = findOrder(orderId);
		assertCustomer(order, request.customerId());

		if (order.getStatus() != OrderStatus.DELIVERED) {
			throw new InvalidOperationException("Only delivered orders can be returned");
		}

		transition(order, OrderStatus.RETURNED, request.reason());
		inventoryService.releaseOrderReservations(order);
		refundIfPaid(order, request.reason());
		return toResponse(order);
	}

	private void transition(Order order, OrderStatus targetStatus, String reason) {
		OrderStateMachine.validateTransition(order.getStatus(), targetStatus);
		order.setStatus(targetStatus);
		if (reason != null && !reason.isBlank()) {
			order.setStatusReason(reason);
		}
	}

	private void refundIfPaid(Order order, String reason) {
		PaymentStatus status = order.getPayment().getStatus();
		if (status == PaymentStatus.SUCCESS || status == PaymentStatus.PARTIALLY_REFUNDED) {
			BigDecimal remaining = order.getTotalAmount().subtract(order.getPayment().getRefundedAmount());
			if (remaining.signum() > 0) {
				paymentService.refundPayment(order.getPayment().getId(), new RefundRequest(remaining, reason));
			}
		}
	}

	private void assertCustomer(Order order, Long customerId) {
		if (!order.getCustomerId().equals(customerId)) {
			throw new InvalidOperationException("Order does not belong to customer: " + customerId);
		}
	}

	private Order findOrder(Long orderId) {
		return orderRepository.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
	}

	private OrderResponse toResponse(Order order) {
		List<OrderItemResponse> items = order.getItems().stream()
				.map(item -> new OrderItemResponse(
						item.getId(),
						item.getProductId(),
						item.getProductName(),
						item.getSku(),
						item.getQuantity(),
						item.getUnitPrice(),
						item.getLineTotal(),
						item.getWarehouseId()))
				.toList();

		return new OrderResponse(
				order.getId(),
				order.getCustomerId(),
				order.getCartId(),
				order.getStatus(),
				order.getStatusReason(),
				OrderStateMachine.allowedNextStatuses(order.getStatus()),
				order.getPayment().getStatus(),
				order.getPayment().getTransactionRef(),
				order.getSubtotal(),
				order.getDiscountAmount(),
				order.getPromoCode(),
				order.getTaxAmount(),
				order.getTotalAmount(),
				items,
				order.getCreatedAt(),
				order.getUpdatedAt());
	}

}
