package Ecommerce.Management.repository.order;

import Ecommerce.Management.domain.order.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

	@EntityGraph(attributePaths = { "items", "payment" })
	Optional<Order> findById(Long id);

	@EntityGraph(attributePaths = { "items", "payment" })
	List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

}
