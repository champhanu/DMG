package Ecommerce.Management.service.cart;

import Ecommerce.Management.domain.cart.Cart;
import Ecommerce.Management.domain.cart.CartItem;
import Ecommerce.Management.domain.cart.CartStatus;
import Ecommerce.Management.domain.catalog.Product;
import Ecommerce.Management.dto.cart.AddCartItemRequest;
import Ecommerce.Management.dto.cart.AddCartItemsRequest;
import Ecommerce.Management.dto.cart.CartItemLineRequest;
import Ecommerce.Management.dto.cart.CartItemResponse;
import Ecommerce.Management.dto.cart.CartResponse;
import Ecommerce.Management.dto.cart.UpdateCartItemRequest;
import Ecommerce.Management.exception.InvalidOperationException;
import Ecommerce.Management.exception.ResourceNotFoundException;
import Ecommerce.Management.repository.cart.CartItemRepository;
import Ecommerce.Management.repository.cart.CartRepository;
import Ecommerce.Management.service.catalog.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;

@Service
@Transactional
public class CartService {

	private final CartRepository cartRepository;
	private final CartItemRepository cartItemRepository;
	private final ProductService productService;

	public CartService(
			CartRepository cartRepository,
			CartItemRepository cartItemRepository,
			ProductService productService) {
		this.cartRepository = cartRepository;
		this.cartItemRepository = cartItemRepository;
		this.productService = productService;
	}

	@Transactional(readOnly = true)
	public CartResponse getCart(Long customerId) {
		Cart cart = cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)
				.orElseThrow(() -> new ResourceNotFoundException("Cart not found for customer: " + customerId));
		return toResponse(cart);
	}

	public CartResponse addItem(AddCartItemRequest request) {
		Cart cart = getOrCreateActiveCart(request.customerId());
		addOrMergeLine(cart, request.productId(), request.quantity());
		return toResponse(cart);
	}

	public CartResponse addItems(AddCartItemsRequest request) {
		Cart cart = getOrCreateActiveCart(request.customerId());
		for (CartItemLineRequest line : request.items()) {
			addOrMergeLine(cart, line.productId(), line.quantity());
		}
		return toResponse(cart);
	}

	public CartResponse updateItem(Long itemId, UpdateCartItemRequest request) {
		CartItem item = findCartItem(itemId);
		assertActiveCart(item.getCart());
		Product product = productService.findActiveProduct(item.getProduct().getId());
		item.setQuantity(request.quantity());
		item.setUnitPrice(product.getPrice());
		return toResponse(item.getCart());
	}

	public CartResponse removeItem(Long itemId) {
		CartItem item = findCartItem(itemId);
		Cart cart = item.getCart();
		assertActiveCart(cart);
		cart.removeItem(item);
		cartItemRepository.delete(item);
		return toResponse(cart);
	}

	public CartResponse clearCart(Long customerId) {
		Cart cart = cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)
				.orElseThrow(() -> new ResourceNotFoundException("Cart not found for customer: " + customerId));
		cart.getItems().clear();
		return toResponse(cart);
	}

	public Cart getActiveCartForCustomer(Long customerId) {
		return getOrCreateActiveCart(customerId);
	}

	private void addOrMergeLine(Cart cart, Long productId, int quantity) {
		Product product = productService.findActiveProduct(productId);
		CartItem item = findExistingLine(cart, productId);

		if (item == null) {
			item = new CartItem();
			item.setProduct(product);
			item.setQuantity(quantity);
			item.setUnitPrice(product.getPrice());
			cart.addItem(item);
		}
		else {
			item.setQuantity(item.getQuantity() + quantity);
			item.setUnitPrice(product.getPrice());
		}
	}

	private CartItem findExistingLine(Cart cart, Long productId) {
		CartItem inMemory = cart.getItems().stream()
				.filter(line -> line.getProduct().getId().equals(productId))
				.findFirst()
				.orElse(null);
		if (inMemory != null) {
			return inMemory;
		}
		if (cart.getId() == null) {
			return null;
		}
		return cartItemRepository.findByCartIdAndProductId(cart.getId(), productId).orElse(null);
	}

	private Cart getOrCreateActiveCart(Long customerId) {
		return cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)
				.orElseGet(() -> {
					Cart cart = new Cart();
					cart.setCustomerId(customerId);
					cart.setStatus(CartStatus.ACTIVE);
					return cartRepository.save(cart);
				});
	}

	private CartItem findCartItem(Long itemId) {
		return cartItemRepository.findById(itemId)
				.orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + itemId));
	}

	private void assertActiveCart(Cart cart) {
		if (cart.getStatus() != CartStatus.ACTIVE) {
			throw new InvalidOperationException("Cart is not active: " + cart.getId());
		}
	}

	private CartResponse toResponse(Cart cart) {
		var items = cart.getItems().stream()
				.sorted(Comparator.comparing(CartItem::getId, Comparator.nullsLast(Long::compareTo)))
				.map(this::toItemResponse)
				.toList();

		int lineCount = items.size();
		int totalItems = items.stream().mapToInt(CartItemResponse::quantity).sum();
		BigDecimal subtotal = items.stream()
				.map(CartItemResponse::lineTotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		return new CartResponse(
				cart.getId(),
				cart.getCustomerId(),
				cart.getStatus(),
				items,
				lineCount,
				totalItems,
				subtotal,
				cart.getUpdatedAt());
	}

	private CartItemResponse toItemResponse(CartItem item) {
		BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
		return new CartItemResponse(
				item.getId(),
				item.getProduct().getId(),
				item.getProduct().getName(),
				item.getProduct().getSku(),
				item.getProduct().getCategory().getId(),
				item.getProduct().getCategory().getName(),
				item.getQuantity(),
				item.getUnitPrice(),
				lineTotal);
	}

}
