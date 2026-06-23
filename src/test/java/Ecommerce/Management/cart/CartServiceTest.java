package Ecommerce.Management.cart;

import Ecommerce.Management.domain.cart.Cart;
import Ecommerce.Management.domain.cart.CartStatus;
import Ecommerce.Management.exception.ResourceNotFoundException;
import Ecommerce.Management.repository.cart.CartItemRepository;
import Ecommerce.Management.repository.cart.CartRepository;
import Ecommerce.Management.service.cart.CartService;
import Ecommerce.Management.service.catalog.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

	@Mock
	private CartRepository cartRepository;

	@Mock
	private CartItemRepository cartItemRepository;

	@Mock
	private ProductService productService;

	@InjectMocks
	private CartService cartService;

	@Test
	void getCartThrowsWhenMissing() {
		when(cartRepository.findByCustomerIdAndStatus(99L, CartStatus.ACTIVE)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> cartService.getCart(99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("99");
	}

	@Test
	void clearCartThrowsWhenMissing() {
		when(cartRepository.findByCustomerIdAndStatus(88L, CartStatus.ACTIVE)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> cartService.clearCart(88L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("88");
	}

}
