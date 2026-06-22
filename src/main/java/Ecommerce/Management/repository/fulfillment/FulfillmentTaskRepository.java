package Ecommerce.Management.repository.fulfillment;

import Ecommerce.Management.domain.fulfillment.FulfillmentTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FulfillmentTaskRepository extends JpaRepository<FulfillmentTask, Long> {

	List<FulfillmentTask> findByOrderId(Long orderId);

}
