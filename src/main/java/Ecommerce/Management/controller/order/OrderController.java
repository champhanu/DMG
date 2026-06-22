package Ecommerce.Management.controller.order;

import Ecommerce.Management.dto.order.OrderCancelRequest;
import Ecommerce.Management.dto.order.OrderResponse;
import Ecommerce.Management.dto.order.OrderReturnRequest;
import Ecommerce.Management.dto.order.OrderStatusUpdateRequest;
import Ecommerce.Management.service.order.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Validated
public class OrderController {

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@GetMapping
	public List<OrderResponse> listOrders(@RequestParam @NotNull Long customerId) {
		return orderService.listCustomerOrders(customerId);
	}

	@GetMapping("/{orderId}")
	public OrderResponse getOrder(@PathVariable Long orderId) {
		return orderService.getOrder(orderId);
	}

	@PatchMapping("/{orderId}/status")
	public OrderResponse updateStatus(
			@PathVariable Long orderId,
			@Valid @RequestBody OrderStatusUpdateRequest request) {
		return orderService.updateStatus(orderId, request);
	}

	@PostMapping("/{orderId}/cancel")
	public OrderResponse cancelOrder(
			@PathVariable Long orderId,
			@Valid @RequestBody OrderCancelRequest request) {
		return orderService.cancelOrder(orderId, request);
	}

	@PostMapping("/{orderId}/return")
	public OrderResponse requestReturn(
			@PathVariable Long orderId,
			@Valid @RequestBody OrderReturnRequest request) {
		return orderService.requestReturn(orderId, request);
	}

}
