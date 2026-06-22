package Ecommerce.Management.controller.cart;

import Ecommerce.Management.dto.cart.AddCartItemRequest;
import Ecommerce.Management.dto.cart.AddCartItemsRequest;
import Ecommerce.Management.dto.cart.CartResponse;
import Ecommerce.Management.dto.cart.UpdateCartItemRequest;
import Ecommerce.Management.service.cart.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@Validated
public class CartController {

	private final CartService cartService;

	public CartController(CartService cartService) {
		this.cartService = cartService;
	}

	@GetMapping
	public CartResponse getCart(@RequestParam @NotNull Long customerId) {
		return cartService.getCart(customerId);
	}

	@PostMapping("/items")
	@ResponseStatus(HttpStatus.OK)
	public CartResponse addItem(@Valid @RequestBody AddCartItemRequest request) {
		return cartService.addItem(request);
	}

	@PostMapping("/items/bulk")
	@ResponseStatus(HttpStatus.OK)
	public CartResponse addItems(@Valid @RequestBody AddCartItemsRequest request) {
		return cartService.addItems(request);
	}

	@PutMapping("/items/{itemId}")
	public CartResponse updateItem(
			@PathVariable Long itemId,
			@Valid @RequestBody UpdateCartItemRequest request) {
		return cartService.updateItem(itemId, request);
	}

	@DeleteMapping("/items/{itemId}")
	public CartResponse removeItem(@PathVariable Long itemId) {
		return cartService.removeItem(itemId);
	}

	@DeleteMapping
	public CartResponse clearCart(@RequestParam @NotNull Long customerId) {
		return cartService.clearCart(customerId);
	}

}
