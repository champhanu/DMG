package Ecommerce.Management.repository.payment;

import Ecommerce.Management.domain.payment.Payment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	@EntityGraph(attributePaths = "order")
	Optional<Payment> findById(Long id);

	@EntityGraph(attributePaths = "order")
	Optional<Payment> findByOrderId(Long orderId);

	@EntityGraph(attributePaths = "order")
	@Query("SELECT p FROM Payment p JOIN p.order o WHERE o.customerId = :customerId ORDER BY p.createdAt DESC")
	List<Payment> findByCustomerId(@Param("customerId") Long customerId);

	Optional<Payment> findByTransactionRef(String transactionRef);

}
