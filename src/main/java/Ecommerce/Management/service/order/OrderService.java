package Ecommerce.Management.service.order;

import Ecommerce.Management.domain.order.Order;
import Ecommerce.Management.dto.order.OrderItemResponse;
import Ecommerce.Management.dto.order.OrderResponse;
import Ecommerce.Management.exception.ResourceNotFoundException;
import Ecommerce.Management.repository.order.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class OrderService {

	private final OrderRepository orderRepository;

	public OrderService(OrderRepository orderRepository) {
		this.orderRepository = orderRepository;
	}

	public OrderResponse getOrder(Long orderId) {
		return toResponse(findOrder(orderId));
	}

	public List<OrderResponse> listCustomerOrders(Long customerId) {
		return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
				.map(this::toResponse)
				.toList();
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
				order.getPayment().getStatus(),
				order.getPayment().getTransactionRef(),
				order.getSubtotal(),
				order.getTaxAmount(),
				order.getTotalAmount(),
				items,
				order.getCreatedAt(),
				order.getUpdatedAt());
	}

}
