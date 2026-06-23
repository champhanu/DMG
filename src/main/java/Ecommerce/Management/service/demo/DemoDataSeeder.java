package Ecommerce.Management.service.demo;

import Ecommerce.Management.domain.cart.Cart;
import Ecommerce.Management.domain.cart.CartStatus;
import Ecommerce.Management.domain.catalog.Category;
import Ecommerce.Management.domain.catalog.Product;
import Ecommerce.Management.domain.inventory.Warehouse;
import Ecommerce.Management.domain.order.Order;
import Ecommerce.Management.domain.order.OrderItem;
import Ecommerce.Management.domain.order.OrderStatus;
import Ecommerce.Management.domain.payment.Payment;
import Ecommerce.Management.domain.payment.PaymentMethod;
import Ecommerce.Management.domain.payment.PaymentStatus;
import Ecommerce.Management.dto.cart.AddCartItemsRequest;
import Ecommerce.Management.dto.cart.CartItemLineRequest;
import Ecommerce.Management.dto.checkout.CheckoutRequest;
import Ecommerce.Management.dto.checkout.CheckoutResponse;
import Ecommerce.Management.dto.demo.DemoSeedResponse;
import Ecommerce.Management.dto.inventory.InventoryRequest;
import Ecommerce.Management.dto.order.OrderCancelRequest;
import Ecommerce.Management.dto.order.OrderReturnRequest;
import Ecommerce.Management.dto.order.OrderStatusUpdateRequest;
import Ecommerce.Management.repository.cart.CartRepository;
import Ecommerce.Management.repository.catalog.CategoryRepository;
import Ecommerce.Management.repository.catalog.ProductRepository;
import Ecommerce.Management.repository.inventory.WarehouseRepository;
import Ecommerce.Management.repository.order.OrderRepository;
import Ecommerce.Management.service.cart.CartService;
import Ecommerce.Management.service.checkout.CheckoutService;
import Ecommerce.Management.service.inventory.InventoryService;
import Ecommerce.Management.service.inventory.InventoryService.ReservationLine;
import Ecommerce.Management.service.order.OrderService;
import Ecommerce.Management.service.tax.TaxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DemoDataSeeder {

	private static final String SEED_MARKER_SLUG = "demo-electronics";

	private final CategoryRepository categoryRepository;
	private final ProductRepository productRepository;
	private final WarehouseRepository warehouseRepository;
	private final CartRepository cartRepository;
	private final OrderRepository orderRepository;
	private final InventoryService inventoryService;
	private final CartService cartService;
	private final CheckoutService checkoutService;
	private final OrderService orderService;
	private final TaxService taxService;

	public DemoDataSeeder(
			CategoryRepository categoryRepository,
			ProductRepository productRepository,
			WarehouseRepository warehouseRepository,
			CartRepository cartRepository,
			OrderRepository orderRepository,
			InventoryService inventoryService,
			CartService cartService,
			CheckoutService checkoutService,
			OrderService orderService,
			TaxService taxService) {
		this.categoryRepository = categoryRepository;
		this.productRepository = productRepository;
		this.warehouseRepository = warehouseRepository;
		this.cartRepository = cartRepository;
		this.orderRepository = orderRepository;
		this.inventoryService = inventoryService;
		this.cartService = cartService;
		this.checkoutService = checkoutService;
		this.orderService = orderService;
		this.taxService = taxService;
	}

	@Transactional
	public DemoSeedResponse seed(boolean force) {
		if (!force && categoryRepository.existsBySlug(SEED_MARKER_SLUG)) {
			return buildSummary("Demo data already seeded (use ?force=true to re-seed)");
		}

		Map<OrderStatus, Long> ordersByStatus = new EnumMap<>(OrderStatus.class);
		List<Long> multiItemOrderIds = new ArrayList<>();

		Map<String, Category> categories = seedCategories();
		List<Product> products = seedProducts(categories);
		List<Warehouse> warehouses = seedWarehouses();
		seedInventory(products, warehouses);

		ordersByStatus.put(OrderStatus.CREATED, seedCreatedOrder(products.get(0), 2L));
		ordersByStatus.put(OrderStatus.CONFIRMED, checkoutSingle(2L, products.get(1), 1));
		ordersByStatus.put(OrderStatus.PACKED, advanceOrder(checkoutSingle(3L, products.get(2), 1), OrderStatus.PACKED));
		ordersByStatus.put(OrderStatus.SHIPPED, advanceOrder(
				checkoutSingle(4L, products.get(3), 1), OrderStatus.PACKED, OrderStatus.SHIPPED));
		ordersByStatus.put(OrderStatus.DELIVERED, advanceOrder(
				checkoutSingle(5L, products.get(4), 1), OrderStatus.PACKED, OrderStatus.SHIPPED, OrderStatus.DELIVERED));

		Long returnedOrderId = advanceOrder(
				checkoutSingle(6L, products.get(5), 1), OrderStatus.PACKED, OrderStatus.SHIPPED, OrderStatus.DELIVERED);
		orderService.requestReturn(returnedOrderId, new OrderReturnRequest(6L, "Demo return for video"));
		ordersByStatus.put(OrderStatus.RETURNED, returnedOrderId);

		Long cancelledOrderId = checkoutSingle(7L, products.get(6), 1);
		orderService.cancelOrder(cancelledOrderId, new OrderCancelRequest(7L, "Demo cancellation for video"));
		ordersByStatus.put(OrderStatus.CANCELLED, cancelledOrderId);

		Long multiOrderId = checkoutMultiItem(8L, List.of(
				line(products.get(7), 1),
				line(products.get(8), 2),
				line(products.get(9), 1),
				line(products.get(10), 1)));
		multiItemOrderIds.add(advanceOrder(multiOrderId, OrderStatus.PACKED, OrderStatus.SHIPPED, OrderStatus.DELIVERED));

		Long multiOrder2Id = checkoutMultiItem(9L, List.of(
				line(products.get(0), 1),
				line(products.get(4), 1),
				line(products.get(11), 3)));
		multiItemOrderIds.add(advanceOrder(multiOrder2Id, OrderStatus.PACKED, OrderStatus.SHIPPED, OrderStatus.DELIVERED));

		return new DemoSeedResponse(
				"Demo data seeded successfully",
				categories.size(),
				products.size(),
				warehouses.size(),
				products.size() * warehouses.size(),
				ordersByStatus,
				multiItemOrderIds);
	}

	private DemoSeedResponse buildSummary(String message) {
		Map<OrderStatus, Long> ordersByStatus = new EnumMap<>(OrderStatus.class);
		orderRepository.findAll().forEach(order -> ordersByStatus.put(order.getStatus(), order.getId()));
		return new DemoSeedResponse(message, 0, 0, 0, 0, ordersByStatus, List.of());
	}

	private Map<String, Category> seedCategories() {
		Map<String, Category> categories = new HashMap<>();
		categories.put("electronics", saveCategory("Electronics", SEED_MARKER_SLUG,
				"Laptops, phones, audio, and TVs for your demo catalog"));
		categories.put("clothing", saveCategory("Clothing & Apparel", "demo-clothing",
				"Fashion and footwear"));
		categories.put("home", saveCategory("Home & Kitchen", "demo-home",
				"Appliances and home essentials"));
		categories.put("sports", saveCategory("Sports & Outdoors", "demo-sports",
				"Fitness and outdoor gear"));
		return categories;
	}

	private Category saveCategory(String name, String slug, String description) {
		Category category = categoryRepository.findBySlug(slug).orElseGet(Category::new);
		category.setName(name);
		category.setSlug(slug);
		category.setDescription(description);
		category.setActive(true);
		return categoryRepository.save(category);
	}

	private List<Product> seedProducts(Map<String, Category> categories) {
		List<Product> products = new ArrayList<>();
		products.add(saveProduct("MacBook Pro 14\"", "DEMO-LAP-001", "Apple M3 Pro laptop", "1999.99", categories.get("electronics")));
		products.add(saveProduct("iPhone 15 Pro", "DEMO-PHN-001", "256GB flagship smartphone", "999.99", categories.get("electronics")));
		products.add(saveProduct("Sony WH-1000XM5", "DEMO-AUD-001", "Noise-cancelling headphones", "349.99", categories.get("electronics")));
		products.add(saveProduct("Samsung 55\" 4K TV", "DEMO-TV-001", "Crystal UHD smart TV", "799.99", categories.get("electronics")));
		products.add(saveProduct("Nike Air Zoom Pegasus", "DEMO-SHO-001", "Men's running shoes", "129.99", categories.get("clothing")));
		products.add(saveProduct("Levi's 501 Original Jeans", "DEMO-JNS-001", "Classic straight fit denim", "79.99", categories.get("clothing")));
		products.add(saveProduct("North Face ThermoBall Jacket", "DEMO-JKT-001", "Insulated winter jacket", "199.99", categories.get("clothing")));
		products.add(saveProduct("Dyson V15 Detect", "DEMO-VAC-001", "Cordless vacuum cleaner", "449.99", categories.get("home")));
		products.add(saveProduct("Instant Pot Duo 7-in-1", "DEMO-POT-001", "Pressure cooker", "89.99", categories.get("home")));
		products.add(saveProduct("Nespresso Vertuo Plus", "DEMO-COF-001", "Coffee and espresso machine", "179.99", categories.get("home")));
		products.add(saveProduct("Manduka Pro Yoga Mat", "DEMO-YOG-001", "6mm eco-friendly mat", "29.99", categories.get("sports")));
		products.add(saveProduct("Bowflex Dumbbell Set", "DEMO-DMB-001", "Adjustable 52.5 lb pair", "349.99", categories.get("sports")));
		products.add(saveProduct("Wilson Pro Staff Tennis Racket", "DEMO-TEN-001", "Professional graphite racket", "119.99", categories.get("sports")));
		return products;
	}

	private Product saveProduct(String name, String sku, String description, String price, Category category) {
		Product product = productRepository.findAll().stream()
				.filter(p -> sku.equals(p.getSku()))
				.findFirst()
				.orElseGet(Product::new);
		product.setName(name);
		product.setSku(sku);
		product.setDescription(description);
		product.setPrice(new BigDecimal(price));
		product.setCategory(category);
		product.setActive(true);
		return productRepository.save(product);
	}

	private List<Warehouse> seedWarehouses() {
		List<Warehouse> warehouses = new ArrayList<>();
		warehouses.add(saveWarehouse("Demo East DC", "DEMO-WH-EAST", "New York, NY"));
		warehouses.add(saveWarehouse("Demo West DC", "DEMO-WH-WEST", "Los Angeles, CA"));
		warehouses.add(saveWarehouse("Demo Central DC", "DEMO-WH-CENTRAL", "Chicago, IL"));
		return warehouses;
	}

	private Warehouse saveWarehouse(String name, String code, String location) {
		Warehouse warehouse = warehouseRepository.findAll().stream()
				.filter(w -> code.equals(w.getCode()))
				.findFirst()
				.orElseGet(Warehouse::new);
		warehouse.setName(name);
		warehouse.setCode(code);
		warehouse.setLocation(location);
		warehouse.setActive(true);
		return warehouseRepository.save(warehouse);
	}

	private void seedInventory(List<Product> products, List<Warehouse> warehouses) {
		int[][] stockMatrix = {
				{ 40, 25, 30 },
				{ 60, 35, 45 },
				{ 80, 50, 55 },
				{ 20, 15, 18 },
				{ 100, 70, 80 },
				{ 120, 90, 85 },
				{ 45, 30, 35 },
				{ 25, 20, 22 },
				{ 150, 100, 110 },
				{ 55, 40, 48 },
				{ 200, 150, 175 },
				{ 30, 25, 28 },
				{ 65, 45, 50 }
		};

		for (int p = 0; p < products.size(); p++) {
			for (int w = 0; w < warehouses.size(); w++) {
				inventoryService.stockInventory(new InventoryRequest(
						warehouses.get(w).getId(),
						products.get(p).getId(),
						stockMatrix[p][w]));
			}
		}
	}

	private Long seedCreatedOrder(Product product, Long customerId) {
		List<ReservationLine> reservations = inventoryService.reserveProduct(product.getId(), 1);
		ReservationLine reservation = reservations.get(0);

		Cart cart = new Cart();
		cart.setCustomerId(customerId);
		cart.setStatus(CartStatus.CHECKED_OUT);
		cart = cartRepository.save(cart);

		BigDecimal subtotal = product.getPrice();
		BigDecimal taxAmount = taxService.calculateTax(subtotal);
		BigDecimal totalAmount = subtotal.add(taxAmount);

		Order order = new Order();
		order.setCustomerId(customerId);
		order.setCartId(cart.getId());
		order.setStatus(OrderStatus.CREATED);
		order.setStatusReason("Demo: awaiting payment confirmation");
		order.setSubtotal(subtotal);
		order.setDiscountAmount(BigDecimal.ZERO);
		order.setTaxAmount(taxAmount);
		order.setTotalAmount(totalAmount);

		OrderItem item = new OrderItem();
		item.setProductId(product.getId());
		item.setProductName(product.getName());
		item.setSku(product.getSku());
		item.setQuantity(1);
		item.setUnitPrice(product.getPrice());
		item.setLineTotal(product.getPrice());
		item.setWarehouseId(reservation.warehouseId());
		order.addItem(item);

		Payment payment = new Payment();
		payment.setAmount(totalAmount);
		payment.setCurrency("USD");
		payment.setPaymentMethod(PaymentMethod.CARD);
		payment.setStatus(PaymentStatus.PENDING);
		payment.setRefundedAmount(BigDecimal.ZERO);
		order.setPayment(payment);

		return orderRepository.save(order).getId();
	}

	private Long checkoutSingle(Long customerId, Product product, int quantity) {
		cartService.addItem(new Ecommerce.Management.dto.cart.AddCartItemRequest(customerId, product.getId(), quantity));
		CheckoutResponse response = checkoutService.checkout(new CheckoutRequest(customerId, "CARD", null, false));
		return response.orderId();
	}

	private Long checkoutMultiItem(Long customerId, List<CartItemLineRequest> lines) {
		cartService.addItems(new AddCartItemsRequest(customerId, lines));
		CheckoutResponse response = checkoutService.checkout(new CheckoutRequest(customerId, "CARD", null, false));
		return response.orderId();
	}

	private CartItemLineRequest line(Product product, int quantity) {
		return new CartItemLineRequest(product.getId(), quantity);
	}

	private Long advanceOrder(Long orderId, OrderStatus... statuses) {
		for (OrderStatus status : statuses) {
			orderService.updateStatus(orderId, new OrderStatusUpdateRequest(status));
		}
		return orderId;
	}

}
