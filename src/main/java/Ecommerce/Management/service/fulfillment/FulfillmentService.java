package Ecommerce.Management.service.fulfillment;

import Ecommerce.Management.domain.fulfillment.FulfillmentTask;
import Ecommerce.Management.domain.fulfillment.FulfillmentTaskStatus;
import Ecommerce.Management.domain.order.Order;
import Ecommerce.Management.domain.order.OrderItem;
import Ecommerce.Management.exception.ResourceNotFoundException;
import Ecommerce.Management.repository.fulfillment.FulfillmentTaskRepository;
import Ecommerce.Management.repository.order.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FulfillmentService {

	private final OrderRepository orderRepository;
	private final FulfillmentTaskRepository fulfillmentTaskRepository;

	public FulfillmentService(OrderRepository orderRepository, FulfillmentTaskRepository fulfillmentTaskRepository) {
		this.orderRepository = orderRepository;
		this.fulfillmentTaskRepository = fulfillmentTaskRepository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public List<FulfillmentTask> routeOrder(Long orderId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

		return order.getItems().stream()
				.map(this::createTask)
				.map(task -> {
					task.setOrderId(orderId);
					task.setStatus(FulfillmentTaskStatus.ROUTED);
					return fulfillmentTaskRepository.save(task);
				})
				.toList();
	}

	@Transactional(readOnly = true)
	public List<FulfillmentTask> getTasksForOrder(Long orderId) {
		return fulfillmentTaskRepository.findByOrderId(orderId);
	}

	private FulfillmentTask createTask(OrderItem item) {
		FulfillmentTask task = new FulfillmentTask();
		task.setWarehouseId(item.getWarehouseId());
		task.setProductId(item.getProductId());
		task.setQuantity(item.getQuantity());
		return task;
	}

}
