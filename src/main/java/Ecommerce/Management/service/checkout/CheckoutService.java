package Ecommerce.Management.service.checkout;

import Ecommerce.Management.domain.cart.Cart;
import Ecommerce.Management.domain.cart.CartItem;
import Ecommerce.Management.domain.cart.CartStatus;
import Ecommerce.Management.domain.order.Order;
import Ecommerce.Management.domain.order.OrderItem;
import Ecommerce.Management.domain.order.OrderStatus;
import Ecommerce.Management.domain.payment.Payment;
import Ecommerce.Management.dto.checkout.CheckoutItemResponse;
import Ecommerce.Management.dto.checkout.CheckoutRequest;
import Ecommerce.Management.dto.checkout.CheckoutResponse;
import Ecommerce.Management.event.OrderPlacedEvent;
import Ecommerce.Management.exception.InvalidOperationException;
import Ecommerce.Management.exception.ResourceNotFoundException;
import Ecommerce.Management.repository.cart.CartRepository;
import Ecommerce.Management.repository.order.OrderRepository;
import Ecommerce.Management.service.inventory.InventoryService;
import Ecommerce.Management.service.inventory.InventoryService.ReservationLine;
import Ecommerce.Management.service.payment.PaymentService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class CheckoutService {

	private static final BigDecimal TAX_RATE = new BigDecimal("0.10");

	private final CartRepository cartRepository;
	private final OrderRepository orderRepository;
	private final InventoryService inventoryService;
	private final PaymentService paymentService;
	private final ApplicationEventPublisher eventPublisher;

	public CheckoutService(
			CartRepository cartRepository,
			OrderRepository orderRepository,
			InventoryService inventoryService,
			PaymentService paymentService,
			ApplicationEventPublisher eventPublisher) {
		this.cartRepository = cartRepository;
		this.orderRepository = orderRepository;
		this.inventoryService = inventoryService;
		this.paymentService = paymentService;
		this.eventPublisher = eventPublisher;
	}

	@Transactional
	public CheckoutResponse checkout(CheckoutRequest request) {
		Cart cart = cartRepository.findByCustomerIdAndStatus(request.customerId(), CartStatus.ACTIVE)
				.orElseThrow(() -> new ResourceNotFoundException("Cart not found for customer: " + request.customerId()));

		if (cart.getItems().isEmpty()) {
			throw new InvalidOperationException("Cannot checkout an empty cart");
		}

		List<OrderItem> orderItems = new ArrayList<>();
		BigDecimal subtotal = BigDecimal.ZERO;

		for (CartItem cartItem : cart.getItems()) {
			List<ReservationLine> reservations = inventoryService.reserveProduct(
					cartItem.getProduct().getId(),
					cartItem.getQuantity());

			for (ReservationLine reservation : reservations) {
				BigDecimal lineTotal = cartItem.getUnitPrice()
						.multiply(BigDecimal.valueOf(reservation.quantity()));
				OrderItem orderItem = new OrderItem();
				orderItem.setProductId(cartItem.getProduct().getId());
				orderItem.setProductName(cartItem.getProduct().getName());
				orderItem.setSku(cartItem.getProduct().getSku());
				orderItem.setQuantity(reservation.quantity());
				orderItem.setUnitPrice(cartItem.getUnitPrice());
				orderItem.setLineTotal(lineTotal);
				orderItem.setWarehouseId(reservation.warehouseId());
				orderItems.add(orderItem);
				subtotal = subtotal.add(lineTotal);
			}
		}

		subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
		BigDecimal taxAmount = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
		BigDecimal totalAmount = subtotal.add(taxAmount);

		Order order = new Order();
		order.setCustomerId(request.customerId());
		order.setCartId(cart.getId());
		order.setStatus(OrderStatus.PLACED);
		order.setSubtotal(subtotal);
		order.setTaxAmount(taxAmount);
		order.setTotalAmount(totalAmount);
		orderItems.forEach(order::addItem);

		Payment payment = paymentService.processPaymentForOrder(
				order,
				request.paymentMethod(),
				Boolean.TRUE.equals(request.simulatePaymentFailure()));

		order.setStatus(OrderStatus.CONFIRMED);

		Order savedOrder = orderRepository.save(order);
		cart.setStatus(CartStatus.CHECKED_OUT);

		paymentService.publishPaymentProcessed(savedOrder.getPayment());
		eventPublisher.publishEvent(new OrderPlacedEvent(savedOrder.getId(), savedOrder.getCustomerId()));

		return toCheckoutResponse(savedOrder);
	}

	private CheckoutResponse toCheckoutResponse(Order order) {
		List<CheckoutItemResponse> items = order.getItems().stream()
				.map(item -> new CheckoutItemResponse(
						item.getProductId(),
						item.getProductName(),
						item.getSku(),
						item.getQuantity(),
						item.getUnitPrice(),
						item.getLineTotal(),
						item.getWarehouseId()))
				.toList();

		return new CheckoutResponse(
				order.getId(),
				order.getCustomerId(),
				order.getStatus(),
				order.getPayment().getStatus(),
				order.getPayment().getTransactionRef(),
				order.getSubtotal(),
				order.getTaxAmount(),
				order.getTotalAmount(),
				items,
				order.getCreatedAt());
	}

}
