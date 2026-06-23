package Ecommerce.Management.repository.cart;

import Ecommerce.Management.domain.cart.Cart;
import Ecommerce.Management.domain.cart.CartStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

	@EntityGraph(attributePaths = { "items", "items.product", "items.product.category" })
	Optional<Cart> findByCustomerIdAndStatus(Long customerId, CartStatus status);

}
